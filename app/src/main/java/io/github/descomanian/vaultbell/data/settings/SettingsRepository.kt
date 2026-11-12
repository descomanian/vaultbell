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
import io.github.descomanian.vaultbell.designsystem.theme.AccentColor
import io.github.descomanian.vaultbell.designsystem.theme.ThemeMode
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
        val preEnabled = booleanPreferencesKey("pre_reminder_enabled")
        val preMinutes = intPreferencesKey("pre_reminder_minutes")
        val repeatIgnored = booleanPreferencesKey("repeat_if_ignored")
        val sound = booleanPreferencesKey("sound_and_vibration")
        val showOverdue = booleanPreferencesKey("show_overdue_on_start")
        val vaultUri = stringPreferencesKey("vault_uri")
        val vaultName = stringPreferencesKey("vault_name")
        val include = stringPreferencesKey("include_folders")
        val exclude = stringPreferencesKey("exclude_folders")
        val scanInterval = intPreferencesKey("scan_interval_minutes")
        val fmtReminder = booleanPreferencesKey("format_reminder_plugin")
        val fmtDataview = booleanPreferencesKey("format_dataview")
        val fmtTasks = booleanPreferencesKey("format_tasks")
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
            preReminderEnabled = this[K.preEnabled] ?: d.preReminderEnabled,
            preReminderMinutes = this[K.preMinutes] ?: d.preReminderMinutes,
            repeatIfIgnored = this[K.repeatIgnored] ?: d.repeatIfIgnored,
            soundAndVibration = this[K.sound] ?: d.soundAndVibration,
            showOverdueOnStart = this[K.showOverdue] ?: d.showOverdueOnStart,
            vaultUri = this[K.vaultUri],
            vaultName = this[K.vaultName] ?: d.vaultName,
            includeFolders = this[K.include].toFolderList(),
            excludeFolders = this[K.exclude].toFolderList(),
            scanIntervalMinutes = this[K.scanInterval] ?: d.scanIntervalMinutes,
            formatReminderPlugin = this[K.fmtReminder] ?: d.formatReminderPlugin,
            formatDataview = this[K.fmtDataview] ?: d.formatDataview,
            formatTasks = this[K.fmtTasks] ?: d.formatTasks,
            writeDoneToFile = this[K.writeDone] ?: d.writeDoneToFile,
            lastScanAt = this[K.lastScanAt] ?: d.lastScanAt,
            lastScanError = this[K.lastScanError],
            lastParserSignature = this[K.parserSignature] ?: d.lastParserSignature,
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
        this[K.preEnabled] = s.preReminderEnabled
        this[K.preMinutes] = s.preReminderMinutes
        this[K.repeatIgnored] = s.repeatIfIgnored
        this[K.sound] = s.soundAndVibration
        this[K.showOverdue] = s.showOverdueOnStart
        if (s.vaultUri != null) this[K.vaultUri] = s.vaultUri else remove(K.vaultUri)
        this[K.vaultName] = s.vaultName
        this[K.include] = s.includeFolders.joinToString("\n")
        this[K.exclude] = s.excludeFolders.joinToString("\n")
        this[K.scanInterval] = s.scanIntervalMinutes
        this[K.fmtReminder] = s.formatReminderPlugin
        this[K.fmtDataview] = s.formatDataview
        this[K.fmtTasks] = s.formatTasks
        this[K.writeDone] = s.writeDoneToFile
        this[K.lastScanAt] = s.lastScanAt
        if (s.lastScanError != null) this[K.lastScanError] = s.lastScanError else remove(K.lastScanError)
        this[K.parserSignature] = s.lastParserSignature
    }

    private fun String?.toFolderList(): List<String> = this?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private inline fun <reified E : Enum<E>> enumOr(
        name: String?,
        fallback: E,
    ): E = enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
