package io.github.descomanian.vaultbell.parser

import java.time.LocalDateTime
import java.time.LocalTime

/** Supported inline reminder syntaxes. See docs/syntax.md. */
enum class ReminderFormat {
    /** Obsidian Reminder plugin: `- [ ] Text (@2026-11-14 18:30)` */
    REMINDER_PLUGIN,

    /** Dataview inline field: `- [ ] Text [due:: 2026-11-14 18:30]` */
    DATAVIEW,

    /** Tasks plugin emoji: `- [ ] Text ⏰ 2026-11-14 18:30` or `📅 2026-11-14` */
    TASKS_EMOJI
}

data class ParserOptions(
    val enabledFormats: Set<ReminderFormat> = setOf(ReminderFormat.REMINDER_PLUGIN),
    /** Time applied to date-only reminders. */
    val defaultTime: LocalTime = LocalTime.of(9, 0)
)

data class ParsedReminder(
    /** 0-based line index inside the file. */
    val lineNumber: Int,
    /** Task text without checkbox and date token. */
    val title: String,
    val dateTime: LocalDateTime,
    val hasExplicitTime: Boolean,
    val format: ReminderFormat,
    val rawLine: String,
    /** Stable hash of the raw line, used to detect edits and sync conflicts. */
    val lineHash: String
)
