package io.github.descomanian.vaultbell.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.descomanian.vaultbell.alarm.NagStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val amoled = booleanPreferencesKey("amoled_black")
        val accent = stringPreferencesKey("accent")
        val defaultTime = intPreferencesKey("default_time_minutes")
        val snoozeShort = intPreferencesKey("snooze_short")
        val snoozeLong = intPreferencesKey("snooze_long")

        // v1.0 early reminder, migrated into the reminder plan on first read
        val preEnabled = booleanPreferencesKey("pre_reminder_enabled")
        val preMinutes = intPreferencesKey("pre_reminder_minutes")
        val planEnabled = booleanPreferencesKey("plan_enabled")
        val planOffsets = stringPreferencesKey("plan_offsets")
        val nagEnabled = booleanPreferencesKey("nag_enabled")
        val nagStages = stringPreferencesKey("nag_stages")
        val nagDayStart = intPreferencesKey("nag_day_start_minutes")
        val activeHours = booleanPreferencesKey("active_hours_enabled")
        val activeFrom = intPreferencesKey("active_from_minutes")
        val activeUntil = intPreferencesKey("active_until_minutes")
        val planBreakThrough = booleanPreferencesKey("plan_break_through")
        val repeatIgnored = booleanPreferencesKey("repeat_if_ignored")
        val sound = booleanPreferencesKey("sound_and_vibration")
        val breakSilence = booleanPreferencesKey("break_through_silence")
        val showOverdue = booleanPreferencesKey("show_overdue_on_start")
        val hideOnLockScreen = booleanPreferencesKey("hide_on_lock_screen")
        val vaultUri = stringPreferencesKey("vault_uri")
        val vaultName = stringPreferencesKey("vault_name")
        val include = stringPreferencesKey("include_folders")
        val exclude = stringPreferencesKey("exclude_folders")
        val scanInterval = intPreferencesKey("scan_interval_minutes")
        val fmtReminder = booleanPreferencesKey("format_reminder_plugin")
        val fmtDataview = booleanPreferencesKey("format_dataview")
        val fmtTasks = booleanPreferencesKey("format_tasks")
        val fmtProperties = booleanPreferencesKey("format_properties")
        val propertyNames = stringPreferencesKey("property_names")
        val writeDone = booleanPreferencesKey("write_done_to_file")
        val lastScanAt = longPreferencesKey("last_scan_at")
        val lastScanError = stringPreferencesKey("last_scan_error")
        val parserSignature = stringPreferencesKey("parser_signature")
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            themeMode = enumOr(this[K.themeMode], d.themeMode),
            dynamicColor = this[K.dynamicColor] ?: d.dynamicColor,
            amoledBlack = this[K.amoled] ?: d.amoledBlack,
            accent = enumOr(this[K.accent], d.accent),
            defaultTimeMinutes = this[K.defaultTime] ?: d.defaultTimeMinutes,
            snoozeShortMinutes = this[K.snoozeShort] ?: d.snoozeShortMinutes,
            snoozeLongMinutes = this[K.snoozeLong] ?: d.snoozeLongMinutes,
            planEnabled = this[K.planEnabled] ?: this[K.preEnabled] ?: d.planEnabled,
            planOffsetsMinutes = this[K.planOffsets]?.let(::parseOffsets) ?: this[K.preMinutes]?.let(::listOf) ?: d.planOffsetsMinutes,
            nagEnabled = this[K.nagEnabled] ?: d.nagEnabled,
            nagStages = this[K.nagStages]?.let(::parseStages) ?: d.nagStages,
            nagDayStartMinutes = this[K.nagDayStart],
            activeHoursEnabled = this[K.activeHours] ?: d.activeHoursEnabled,
            activeFromMinutes = this[K.activeFrom] ?: d.activeFromMinutes,
            activeUntilMinutes = this[K.activeUntil] ?: d.activeUntilMinutes,
            planBreakThrough = this[K.planBreakThrough] ?: d.planBreakThrough,
            repeatIfIgnored = this[K.repeatIgnored] ?: d.repeatIfIgnored,
            soundAndVibration = this[K.sound] ?: d.soundAndVibration,
            breakThroughSilence = this[K.breakSilence] ?: d.breakThroughSilence,
            showOverdueOnStart = this[K.showOverdue] ?: d.showOverdueOnStart,
            hideOnLockScreen = this[K.hideOnLockScreen] ?: d.hideOnLockScreen,
            vaultUri = this[K.vaultUri],
            vaultName = this[K.vaultName] ?: d.vaultName,
            includeFolders = this[K.include].toFolderList(),
            excludeFolders = this[K.exclude].toFolderList(),
            scanIntervalMinutes = this[K.scanInterval] ?: d.scanIntervalMinutes,
            formatReminderPlugin = this[K.fmtReminder] ?: d.formatReminderPlugin,
            formatDataview = this[K.fmtDataview] ?: d.formatDataview,
            formatTasks = this[K.fmtTasks] ?: d.formatTasks,
            formatProperties = this[K.fmtProperties] ?: d.formatProperties,
            propertyNames = this[K.propertyNames]?.toFolderList()?.ifEmpty { null } ?: d.propertyNames,
            writeDoneToFile = this[K.writeDone] ?: d.writeDoneToFile,
            lastScanAt = this[K.lastScanAt] ?: d.lastScanAt,
            lastScanError = this[K.lastScanError],
            lastParserSignature = this[K.parserSignature] ?: d.lastParserSignature
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.onboardingDone] = s.onboardingDone
        this[K.themeMode] = s.themeMode.name
        this[K.dynamicColor] = s.dynamicColor
        this[K.amoled] = s.amoledBlack
        this[K.accent] = s.accent.name
        this[K.defaultTime] = s.defaultTimeMinutes
        this[K.snoozeShort] = s.snoozeShortMinutes
        this[K.snoozeLong] = s.snoozeLongMinutes
        remove(K.preEnabled)
        remove(K.preMinutes)
        this[K.planEnabled] = s.planEnabled
        this[K.planOffsets] = s.planOffsetsMinutes.joinToString(",")
        this[K.nagEnabled] = s.nagEnabled
        this[K.nagStages] = s.nagStages.joinToString(",") { "${it.startMinutesBefore}:${it.everyMinutes}" }
        if (s.nagDayStartMinutes != null) this[K.nagDayStart] = s.nagDayStartMinutes else remove(K.nagDayStart)
        this[K.activeHours] = s.activeHoursEnabled
        this[K.activeFrom] = s.activeFromMinutes
        this[K.activeUntil] = s.activeUntilMinutes
        this[K.planBreakThrough] = s.planBreakThrough
        this[K.repeatIgnored] = s.repeatIfIgnored
        this[K.sound] = s.soundAndVibration
        this[K.breakSilence] = s.breakThroughSilence
        this[K.showOverdue] = s.showOverdueOnStart
        this[K.hideOnLockScreen] = s.hideOnLockScreen
        if (s.vaultUri != null) this[K.vaultUri] = s.vaultUri else remove(K.vaultUri)
        this[K.vaultName] = s.vaultName
        this[K.include] = s.includeFolders.joinToString("\n")
        this[K.exclude] = s.excludeFolders.joinToString("\n")
        this[K.scanInterval] = s.scanIntervalMinutes
        this[K.fmtReminder] = s.formatReminderPlugin
        this[K.fmtDataview] = s.formatDataview
        this[K.fmtTasks] = s.formatTasks
        this[K.fmtProperties] = s.formatProperties
        this[K.propertyNames] = s.propertyNames.joinToString("\n")
        this[K.writeDone] = s.writeDoneToFile
        this[K.lastScanAt] = s.lastScanAt
        if (s.lastScanError != null) this[K.lastScanError] = s.lastScanError else remove(K.lastScanError)
        this[K.parserSignature] = s.lastParserSignature
    }

    /** "120,60,30,15"; empty string = empty list (all lead times removed). */
    private fun parseOffsets(text: String): List<Int> = text.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }

    /** "1440:120,120:60,60:30" */
    private fun parseStages(text: String): List<NagStage> =
        text.split(',').mapNotNull { part ->
            val (start, every) = part.split(':').map { it.trim().toIntOrNull() }.let { it.getOrNull(0) to it.getOrNull(1) }
            if (start != null && every != null && start > 0 && every > 0) NagStage(start, every) else null
        }

    private fun String?.toFolderList(): List<String> = this?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private inline fun <reified E : Enum<E>> enumOr(
        name: String?,
        fallback: E
    ): E = enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
