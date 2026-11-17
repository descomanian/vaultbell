package io.github.descomanian.vaultbell.data.settings

import io.github.descomanian.vaultbell.alarm.ActiveHours
import io.github.descomanian.vaultbell.alarm.NagStage
import io.github.descomanian.vaultbell.alarm.PlanSettings
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
    /** Reminder plan: early reminders at fixed lead times (replaces the single v1.0 early reminder). */
    val planEnabled: Boolean = false,
    val planOffsetsMinutes: List<Int> = DEFAULT_PLAN_OFFSETS,
    /** "Dranbleiben": repeated reminders on the due day, denser towards the due time. Only with an explicit time. */
    val nagEnabled: Boolean = false,
    val nagStages: List<NagStage> = DEFAULT_NAG_STAGES,
    /** Start of "Dranbleiben" on the due day; null = default time. */
    val nagDayStartMinutes: Int? = null,
    /** Early reminders and "Dranbleiben" only inside these hours; the main alarm always rings. */
    val activeHoursEnabled: Boolean = true,
    val activeFromMinutes: Int = DEFAULT_ACTIVE_FROM,
    val activeUntilMinutes: Int = DEFAULT_ACTIVE_UNTIL,
    /** Early reminders and "Dranbleiben" also pass Do Not Disturb (off: normal notification). */
    val planBreakThrough: Boolean = false,
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
    val defaultTime: LocalTime get() = minutesToTime(defaultTimeMinutes)

    val planSettings: PlanSettings
        get() =
            PlanSettings(
                offsetsMinutes = if (planEnabled) planOffsetsMinutes else emptyList(),
                nagStages = if (nagEnabled) nagStages else emptyList(),
                dayStart = minutesToTime(nagDayStartMinutes ?: defaultTimeMinutes),
                activeHours =
                if (activeHoursEnabled) ActiveHours(minutesToTime(activeFromMinutes), minutesToTime(activeUntilMinutes)) else null
            )

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
        val DEFAULT_PLAN_OFFSETS = listOf(120, 60, 30, 15)
        val DEFAULT_NAG_STAGES = listOf(NagStage(24 * 60, 120), NagStage(120, 60), NagStage(60, 30))
        val PLAN_OFFSET_OPTIONS = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 720, 1440)
        val NAG_INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 180, 240)

        val DEFAULT_ACTIVE_FROM = 8 * 60
        val DEFAULT_ACTIVE_UNTIL = 22 * 60
        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24

        fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / MINUTES_PER_HOUR % HOURS_PER_DAY, minutes % MINUTES_PER_HOUR)

        val SCAN_INTERVALS = listOf(15, 30, 60, 120, 240)
        val SNOOZE_SHORT_OPTIONS = listOf(5, 10, 15, 30)
        val SNOOZE_LONG_OPTIONS = listOf(30, 60, 120, 180)
    }
}
