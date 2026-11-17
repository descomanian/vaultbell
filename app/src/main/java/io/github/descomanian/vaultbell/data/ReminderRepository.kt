package io.github.descomanian.vaultbell.data

import android.net.Uri
import io.github.descomanian.vaultbell.alarm.Notifications
import io.github.descomanian.vaultbell.alarm.ReminderAlarms
import io.github.descomanian.vaultbell.data.db.FileStateEntity
import io.github.descomanian.vaultbell.data.db.ReminderDao
import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.triggerAt
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.data.settings.SettingsRepository
import io.github.descomanian.vaultbell.parser.ParsedReminder
import io.github.descomanian.vaultbell.parser.ReminderParser
import io.github.descomanian.vaultbell.parser.TaskLineEditor
import io.github.descomanian.vaultbell.scheduler.ScheduleMode
import io.github.descomanian.vaultbell.vault.ScanFilter
import io.github.descomanian.vaultbell.vault.VaultAccess
import io.github.descomanian.vaultbell.vault.VaultFile
import io.github.descomanian.vaultbell.vault.WriteOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ScanOutcome {
    data class Success(val files: Int, val changedFiles: Int, val reminders: Int, val exactAlarms: Boolean) : ScanOutcome

    data object NoVault : ScanOutcome

    data object PermissionLost : ScanOutcome

    data class Failed(val message: String) : ScanOutcome
}

/** Result of marking a reminder done, so the UI can offer Undo when nothing was written. */
enum class DoneResult { LOCAL_ONLY, WRITTEN_TO_FILE, CONFLICT_NOT_WRITTEN, NOT_UTF8, WRITE_FAILED }

/**
 * Single source of truth: keeps the Room index in sync with the vault and the alarms in sync with the index.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val settings: SettingsRepository,
    private val vault: VaultAccess,
    private val alarms: ReminderAlarms,
    private val notifications: Notifications,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Called after the index changed (scan, done, snooze), e.g. to refresh the home screen widget. */
    private val onChanged: suspend () -> Unit = {}
) {
    private val scanMutex = Mutex()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    val activeReminders: Flow<List<Reminder>> = dao.observeActive().map { list -> list.map { it.toModel() } }
    val activeCount: Flow<Int> = dao.observeActiveCount()

    fun observe(id: Long): Flow<Reminder?> = dao.observe(id).map { it?.takeUnless { e -> e.completedLocally }?.toModel() }

    suspend fun scan(force: Boolean = false): ScanOutcome =
        scanMutex.withLock {
            withContext(Dispatchers.IO) {
                _scanning.value = true
                try {
                    doScan(force).also { onChanged() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SecurityException) {
                    recordScan(error = "permission")
                    ScanOutcome.PermissionLost
                } catch (e: Exception) {
                    recordScan(error = e.message ?: e.javaClass.simpleName)
                    ScanOutcome.Failed(e.message ?: e.javaClass.simpleName)
                } finally {
                    _scanning.value = false
                    withContext(NonCancellable) { armScanHeartbeat() }
                }
            }
        }

    private suspend fun doScan(forceRequested: Boolean): ScanOutcome {
        val s = settings.current()
        val tree = s.vaultUri?.let(Uri::parse) ?: return ScanOutcome.NoVault
        if (!vault.hasReadPermission(tree)) {
            recordScan(error = "permission")
            return ScanOutcome.PermissionLost
        }
        val force = forceRequested || s.lastParserSignature != s.parserSignature
        val parser = ReminderParser(s.parserOptions)
        val files = vault.listMarkdownFiles(tree, ScanFilter(s.includeFolders, s.excludeFolders))
        val known = dao.files().associateBy { it.uri }
        val seen = HashSet<String>(files.size)
        var changed = 0

        for (file in files) {
            val uri = file.documentUri.toString()
            seen += uri
            if (!force && known[uri]?.lastModified == file.lastModified) continue
            val content = runCatching { vault.readText(file.documentUri) }.getOrNull() ?: continue
            syncFile(file, parser.parse(content), s)
            dao.upsertFile(FileStateEntity(uri, file.relativePath, file.lastModified))
            changed++
        }

        val removed = known.keys - seen
        if (removed.isNotEmpty()) {
            for (uri in removed) dao.forFile(uri).forEach { alarms.cancel(it.id) }
            dao.deleteForFiles(removed.toList())
            dao.deleteFiles(removed.toList())
        }

        val active = dao.allActive()
        settings.update { it.copy(lastScanAt = clock(), lastScanError = null, lastParserSignature = it.parserSignature) }
        return ScanOutcome.Success(files.size, changed, active.size, alarms.canScheduleExact())
    }

    /** Diff by line hash: unchanged lines keep their id (and snooze/done state), edits become new entries. */
    private suspend fun syncFile(
        file: VaultFile,
        parsed: List<ParsedReminder>,
        s: AppSettings
    ) {
        val uri = file.documentUri.toString()
        val existing = dao.forFile(uri)
        val pool = existing.groupBy { it.lineHash }.mapValues { it.value.toMutableList() }.toMutableMap()
        val keep = HashSet<Long>()
        val now = clock()

        for (p in parsed) {
            val dueAt = p.dateTime.toEpochMillis()
            val match = pool[p.lineHash]?.removeFirstOrNull()
            if (match != null) {
                keep += match.id
                val dueChanged = match.dueAt != dueAt
                val updated =
                    match.copy(
                        relativePath = file.relativePath,
                        lineNumber = p.lineNumber,
                        title = p.title,
                        dueAt = dueAt,
                        hasExplicitTime = p.hasExplicitTime,
                        format = p.format.name,
                        snoozedUntil = if (dueChanged) null else match.snoozedUntil,
                        firedAt = if (dueChanged) null else match.firedAt
                    )
                if (updated != match) dao.update(updated)
                if (dueChanged) alarms.schedule(updated, s, now)
            } else {
                val entity =
                    ReminderEntity(
                        fileUri = uri,
                        relativePath = file.relativePath,
                        lineNumber = p.lineNumber,
                        title = p.title,
                        rawLine = p.rawLine,
                        lineHash = p.lineHash,
                        dueAt = dueAt,
                        hasExplicitTime = p.hasExplicitTime,
                        format = p.format.name
                    )
                val id = dao.insert(entity)
                alarms.schedule(entity.copy(id = id), s, now)
            }
        }

        val gone = existing.filter { it.id !in keep }
        if (gone.isNotEmpty()) {
            gone.forEach {
                alarms.cancel(it.id)
                notifications.cancel(it.id)
            }
            dao.delete(gone.map { it.id })
        }
    }

    private suspend fun recordScan(error: String?) {
        settings.update { it.copy(lastScanAt = clock(), lastScanError = error) }
    }

    /** Re-arms every active alarm (boot, permission change, settings change). */
    suspend fun rescheduleAll(): ScheduleMode? =
        withContext(Dispatchers.IO) {
            val s = settings.current()
            val now = clock()
            var mode: ScheduleMode? = null
            dao.allActive().forEach { e -> alarms.schedule(e, s, now)?.let { mode = it } }
            armScanHeartbeat()
            mode
        }

    /** Next background scan, independent of WorkManager (survives Doze and OEM battery savers better). */
    private suspend fun armScanHeartbeat() {
        val s = settings.current()
        if (s.vaultUri == null || !s.onboardingDone) {
            alarms.cancelScanHeartbeat()
        } else {
            alarms.scheduleScanHeartbeat(clock() + s.scanIntervalMinutes * MINUTE)
        }
    }

    /** Drops the index, e.g. after switching to another vault. */
    suspend fun clearIndex() =
        withContext<Unit>(Dispatchers.IO) {
            dao.allActive().forEach {
                alarms.cancel(it.id)
                notifications.cancel(it.id)
            }
            dao.clearReminders()
            dao.clearFiles()
        }

    suspend fun markDone(id: Long): DoneResult? =
        withContext(Dispatchers.IO) {
            val e = dao.get(id) ?: return@withContext null
            alarms.cancel(id)
            notifications.cancel(id)
            val s = settings.current()
            val result =
                if (s.writeDoneToFile) {
                    writeDone(e)
                } else {
                    DoneResult.LOCAL_ONLY
                }
            dao.update(e.copy(completedLocally = true))
            onChanged()
            result
        }

    /**
     * Strict write-back (B2): only valid UTF-8, only the indexed line, original line endings,
     * verified by reading back and restored on mismatch.
     */
    private fun writeDone(e: ReminderEntity): DoneResult {
        val uri = Uri.parse(e.fileUri)
        return try {
            val original = vault.readBytes(uri)
            when (val edit = TaskLineEditor.markDoneInFile(original, e.lineNumber, e.lineHash)) {
                is TaskLineEditor.FileEditResult.Edited ->
                    if (vault.replaceVerified(uri, original, edit.bytes) == WriteOutcome.WRITTEN) {
                        DoneResult.WRITTEN_TO_FILE
                    } else {
                        DoneResult.WRITE_FAILED
                    }
                is TaskLineEditor.FileEditResult.Rejected ->
                    if (edit.reason == TaskLineEditor.EditResult.NotUtf8) DoneResult.NOT_UTF8 else DoneResult.CONFLICT_NOT_WRITTEN
            }
        } catch (ex: Exception) {
            DoneResult.CONFLICT_NOT_WRITTEN
        }
    }

    suspend fun undoDone(id: Long) =
        withContext<Unit>(Dispatchers.IO) {
            val e = dao.get(id) ?: return@withContext
            val restored = e.copy(completedLocally = false)
            dao.update(restored)
            alarms.schedule(restored, settings.current(), clock())
            onChanged()
        }

    suspend fun snooze(
        id: Long,
        minutes: Int
    ) = withContext<Unit>(Dispatchers.IO) {
        val e = dao.get(id) ?: return@withContext
        notifications.cancel(id)
        val snoozed = e.copy(snoozedUntil = clock() + minutes * MINUTE, firedAt = null)
        dao.update(snoozed)
        alarms.schedule(snoozed, settings.current(), clock())
        onChanged()
    }

    /** Called by the alarm receiver. */
    suspend fun onAlarmFired(
        id: Long,
        kind: Int,
        repeatCount: Int
    ) = withContext<Unit>(Dispatchers.IO) {
        val e = dao.get(id) ?: return@withContext
        if (e.completedLocally) return@withContext
        val now = clock()
        // Stale alarm (time changed after scheduling): the new alarm is already armed.
        if (kind == ReminderAlarms.KIND_MAIN && e.triggerAt > now + MINUTE) return@withContext
        val s = settings.current()
        notifications.show(e, kind, s)
        if (kind != ReminderAlarms.KIND_PRE) {
            dao.update(e.copy(firedAt = now))
            if (s.repeatIfIgnored && repeatCount < ReminderAlarms.MAX_REPEATS) {
                alarms.scheduleRepeat(id, now + ReminderAlarms.REPEAT_INTERVAL_MINUTES * MINUTE, repeatCount + 1)
            }
        }
    }

    /** User swiped the notification away or opened it: stop nagging. */
    fun onNotificationHandled(id: Long) {
        alarms.cancelRepeat(id)
    }

    /** Next active reminders by effective time (overdue first), for the widget. */
    suspend fun upcoming(limit: Int): List<Reminder> =
        withContext(Dispatchers.IO) {
            dao.allActive().sortedBy { it.triggerAt }.take(limit).map { it.toModel() }
        }

    suspend fun get(id: Long): ReminderEntity? = withContext(Dispatchers.IO) { dao.get(id) }

    private companion object {
        const val MINUTE = 60_000L
    }
}
