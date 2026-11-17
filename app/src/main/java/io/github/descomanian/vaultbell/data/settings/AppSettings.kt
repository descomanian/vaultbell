package io.github.descomanian.vaultbell.data.settings

import io.github.descomanian.vaultbell.designsystem.theme.AccentColor
import io.github.descomanian.vaultbell.designsystem.theme.ThemeMode
import io.github.descomanian.vaultbell.parser.ParserOptions
import io.github.descomanian.vaultbell.parser.ReminderFormat
import java.time.LocalTime

data class AppSettings(
    val onboardingDone: Boolean = false,
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val accent: AccentColor = AccentColor.SAGE,
    // Reminders
    val defaultTimeMinutes: Int = 9 * 60,
    val snoozeShortMinutes: Int = 10,
    val snoozeLongMinutes: Int = 60,
    val preReminderEnabled: Boolean = false,
    val preReminderMinutes: Int = 15,
    val repeatIfIgnored: Boolean = false,
    val soundAndVibration: Boolean = true,
    /** Deliver as alarm: passes Do Not Disturb and plays on the alarm stream in silent mode. */
    val breakThroughSilence: Boolean = true,
    val showOverdueOnStart: Boolean = true,
    /** Hide reminders on the lock screen (title and note path appear after unlocking). */
    val hideOnLockScreen: Boolean = false,
    // Vault
    val vaultUri: String? = null,
    val vaultName: String = "",
    val includeFolders: List<String> = emptyList(),
    val excludeFolders: List<String> = emptyList(),
    val scanIntervalMinutes: Int = 30,
    // Detection
    val formatReminderPlugin: Boolean = true,
    val formatDataview: Boolean = false,
    val formatTasks: Boolean = false,
    val formatProperties: Boolean = false,
    /** Frontmatter keys read as reminders when [formatProperties] is on. */
    val propertyNames: List<String> = ParserOptions.DEFAULT_PROPERTY_NAMES,
    // Write-back
    val writeDoneToFile: Boolean = false,
    // Scan status
    val lastScanAt: Long = 0L,
    val lastScanError: String? = null,
    /** Fingerprint of everything that changes parse results; a change forces a full rescan. */
    val lastParserSignature: String = ""
) {
    val defaultTime: LocalTime get() = LocalTime.of(defaultTimeMinutes / 60, defaultTimeMinutes % 60)

    val enabledFormats: Set<ReminderFormat>
        get() =
            buildSet {
                if (formatReminderPlugin) add(ReminderFormat.REMINDER_PLUGIN)
                if (formatDataview) add(ReminderFormat.DATAVIEW)
                if (formatTasks) add(ReminderFormat.TASKS_EMOJI)
                if (formatProperties) add(ReminderFormat.PROPERTY)
            }

    val parserOptions: ParserOptions
        get() = ParserOptions(enabledFormats = enabledFormats, defaultTime = defaultTime, propertyNames = propertyNames)

    val parserSignature: String
        get() =
            listOf(
                enabledFormats.sorted().joinToString(","),
                propertyNames.joinToString("|"),
                defaultTimeMinutes,
                includeFolders.joinToString("|"),
                excludeFolders.joinToString("|"),
                vaultUri
            ).joinToString(";")

    companion object {
        val SCAN_INTERVALS = listOf(15, 30, 60, 120, 240)
        val SNOOZE_SHORT_OPTIONS = listOf(5, 10, 15, 30)
        val SNOOZE_LONG_OPTIONS = listOf(30, 60, 120, 180)
    }
}
