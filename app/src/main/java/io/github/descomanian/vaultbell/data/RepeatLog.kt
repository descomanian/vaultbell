package io.github.descomanian.vaultbell.data

import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.RepeatLogDao
import io.github.descomanian.vaultbell.data.db.RepeatLogEntity
import io.github.descomanian.vaultbell.parser.RepeatMarkers
import io.github.descomanian.vaultbell.parser.Utf8
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repeating tasks are never checked off in the note. Each completed date is kept here and exported as a Markdown file
 * the user puts into the vault. Entries are done tasks (`- [x] … ✅ date`), so Tasks and Dataview can query them and
 * Vaultbell's own scan ignores them.
 */
class RepeatLog(
    private val dao: RepeatLogDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val pendingCount: Flow<Int> = dao.observePendingCount()

    /** [rule] null: the rule text written in the note (unsupported rules such as `every 2 weeks`). */
    suspend fun record(
        e: ReminderEntity,
        rule: String?
    ) {
        dao.insert(
            RepeatLogEntity(
                reminderId = e.id,
                title = e.title,
                relativePath = e.relativePath,
                rule = rule ?: RepeatMarkers.ruleText(e.rawLine).orEmpty(),
                instanceAt = e.dueAt,
                hasExplicitTime = e.hasExplicitTime,
                doneAt = clock()
            )
        )
    }

    /** Undo of "done": the entry has not been exported yet. */
    suspend fun undo(reminderId: Long) = dao.deleteNewestPending(reminderId)

    /**
     * Writes all entries that were not exported yet with [write] and marks them as exported only if writing succeeded.
     * Returns the number of exported entries; 0 writes nothing.
     */
    suspend fun export(
        labels: RepeatLogFormat.Labels,
        write: (ByteArray) -> Unit
    ): Int =
        withContext(Dispatchers.IO) {
            val entries = dao.pending()
            if (entries.isEmpty()) return@withContext 0
            val now = clock()
            val text = RepeatLogFormat.document(entries.map { it.toEntry() }, now.toLocalDateTime(), labels)
            write(Utf8.encode(text))
            dao.markExported(entries.map { it.id }, now)
            dao.pruneExported(now - RepeatLogDao.KEEP_EXPORTED_DAYS * DAY)
            entries.size
        }

    private fun RepeatLogEntity.toEntry() =
        RepeatLogFormat.Entry(
            title = title,
            relativePath = relativePath,
            rule = rule,
            instance = instanceAt.toLocalDateTime(),
            hasExplicitTime = hasExplicitTime,
            done = doneAt.toLocalDateTime()
        )

    private companion object {
        const val DAY = 24 * 60 * 60_000L
    }
}

/** Markdown layout of the exported log. Pure functions, unit-tested on the JVM. */
object RepeatLogFormat {
    data class Entry(
        val title: String,
        val relativePath: String,
        val rule: String,
        val instance: LocalDateTime,
        val hasExplicitTime: Boolean,
        val done: LocalDateTime
    )

    /** Localized texts; the entry lines themselves stay language-neutral so queries work in every language. */
    data class Labels(val heading: String, val intro: String)

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val ISO_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
    private val LINE_BREAKS = Regex("""[\r\n]+""")
    private val FIELD_UNSAFE = Regex("""[\[\]()]""")

    /** Suggested file name, unique per minute: `vaultbell-log-2026-11-20-0930.md`. */
    fun fileName(exportedAt: LocalDateTime): String = "vaultbell-log-${exportedAt.format(FILE_STAMP)}.md"

    fun document(
        entries: List<Entry>,
        exportedAt: LocalDateTime,
        labels: Labels
    ): String =
        buildString {
            append("---\n")
            append("tags: [vaultbell-log]\n")
            append("exported: ").append(exportedAt.format(ISO_MINUTES)).append('\n')
            append("entries: ").append(entries.size).append('\n')
            append("---\n\n")
            append("# ").append(labels.heading).append(" · ").append(exportedAt.format(DATE_TIME)).append("\n\n")
            append(labels.intro).append("\n\n")
            entries.sortedBy { it.done }.forEach { append(line(it)).append('\n') }
        }

    /**
     * `- [x] Pflanzen gießen ✅ 2026-11-20 [instance:: 2026-11-20 09:00] [rule:: daily]`
     * `[done-at:: 2026-11-20 09:31] [note:: [[Routinen]]]` (one line in the file).
     *
     * Field names avoid `due`, `repeat` and `completed` on purpose: they would turn the entry into a reminder,
     * a new Tasks series or clash with Dataview's implicit task fields.
     */
    fun line(e: Entry): String {
        val title = e.title.replace(LINE_BREAKS, " ").trim().ifEmpty { noteName(e.relativePath) }
        val instance = e.instance.format(if (e.hasExplicitTime) DATE_TIME else DATE)
        val rule = e.rule.replace(FIELD_UNSAFE, "").replace(LINE_BREAKS, " ").trim()
        return buildString {
            append("- [x] ").append(title)
            append(" ✅ ").append(e.done.format(DATE))
            append(" [instance:: ").append(instance).append(']')
            if (rule.isNotEmpty()) append(" [rule:: ").append(rule).append(']')
            append(" [done-at:: ").append(e.done.format(DATE_TIME)).append(']')
            append(" [note:: [[").append(e.relativePath.removeMarkdownExtension()).append("]]]")
        }
    }

    private fun noteName(path: String) = path.substringAfterLast('/').removeMarkdownExtension()

    private fun String.removeMarkdownExtension() = if (endsWith(MD, ignoreCase = true)) dropLast(MD.length) else this

    private const val MD = ".md"
}
