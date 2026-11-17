package io.github.descomanian.vaultbell.parser

import java.time.LocalDateTime
import java.time.LocalTime

/** Supported reminder syntaxes. See docs/syntax.md. */
enum class ReminderFormat {
    /** Obsidian Reminder plugin: `- [ ] Text (@2026-11-14 18:30)` */
    REMINDER_PLUGIN,

    /** Dataview inline field: `- [ ] Text [due:: 2026-11-14 18:30]` */
    DATAVIEW,

    /** Tasks plugin emoji: `- [ ] Text ⏰ 2026-11-14 18:30` or `📅 2026-11-14` */
    TASKS_EMOJI,

    /** Note property (YAML frontmatter): `reminder: 2026-11-14T18:30` */
    PROPERTY
}

/** Where a reminder comes from. */
enum class ReminderSource {
    /** An open task line in the note body. */
    TASK_LINE,

    /** A date property in the frontmatter; [ParsedReminder.lineNumber] is the frontmatter line of the value. */
    PROPERTY
}

data class ParserOptions(
    val enabledFormats: Set<ReminderFormat> = setOf(ReminderFormat.REMINDER_PLUGIN),
    /** Time applied to date-only reminders. */
    val defaultTime: LocalTime = LocalTime.of(9, 0),
    /** Frontmatter keys read when [ReminderFormat.PROPERTY] is enabled (case-insensitive). */
    val propertyNames: List<String> = DEFAULT_PROPERTY_NAMES
) {
    companion object {
        val DEFAULT_PROPERTY_NAMES = listOf("reminder", "due", "remind")
    }
}

data class ParsedReminder(
    /** 0-based line index inside the file (task line, or frontmatter line for properties). */
    val lineNumber: Int,
    /** Task text without checkbox, date and recurrence tokens; for properties the `title` property or the note name. */
    val title: String,
    val dateTime: LocalDateTime,
    val hasExplicitTime: Boolean,
    val format: ReminderFormat,
    val rawLine: String,
    /** Stable hash used to detect edits and sync conflicts (line hash; for properties a hash of key and value). */
    val lineHash: String,
    val source: ReminderSource = ReminderSource.TASK_LINE,
    /** Repeat rule, or null for a one-off reminder. [dateTime] is the first occurrence (anchor) as written in the note. */
    val recurrence: Recurrence? = null
)
