package io.github.descomanian.vaultbell.parser

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Extracts reminders from Markdown. Pure Kotlin/JVM, no Android dependency.
 *
 * Rules:
 * - Only open tasks (`[ ]`) count. `[x]`, `[X]`, `[-]` and other states are ignored.
 * - Fenced code blocks are skipped. YAML frontmatter is only read for [ReminderFormat.PROPERTY].
 * - The first matching enabled format wins (Reminder plugin, Dataview, Tasks ⏰, Tasks 📅).
 * - Repeat rules (`🔁 every day|week|month|weekday`, `| daily|weekly|monthly|weekdays`) work with every task format.
 */
class ReminderParser(
    private val options: ParserOptions = ParserOptions()
) {
    private val propertyKeys = options.propertyNames.map { it.trim() }.filter { it.isNotEmpty() }

    /** [noteName] (file name without `.md`) is the title of property reminders when the note has no `title` property. */
    fun parse(
        content: String,
        noteName: String = ""
    ): List<ParsedReminder> {
        val lines = Lines.split(content)
        val frontmatter = FrontmatterReader.read(lines)
        val result = ArrayList<ParsedReminder>()
        if (frontmatter != null && ReminderFormat.PROPERTY in options.enabledFormats) {
            result += propertyReminders(frontmatter, noteName)
        }
        var inFence: String? = null
        for (index in (frontmatter?.endLine?.plus(1) ?: 0) until lines.size) {
            val line = lines[index]
            val fence = FENCE.find(line.trimStart())?.value
            if (fence != null) {
                inFence = toggleFence(inFence, fence)
            } else if (inFence == null) {
                parseLine(index, line)?.let(result::add)
            }
        }
        return result
    }

    private fun toggleFence(
        open: String?,
        fence: String
    ): String? =
        when {
            open == null -> fence.take(3)
            fence.startsWith(open) -> null
            else -> open
        }

    private fun propertyReminders(
        frontmatter: Frontmatter,
        noteName: String
    ): List<ParsedReminder> {
        val title = frontmatter.valuesOf(TITLE_PROPERTY).firstOrNull()?.text ?: noteName
        return propertyKeys
            .flatMap(frontmatter::valuesOf)
            .distinctBy { it.lineNumber to it.text }
            .sortedBy { it.lineNumber }
            .mapNotNull { value ->
                val match = PROPERTY_VALUE.matchEntire(value.text) ?: return@mapNotNull null
                val date = parseDate(match.groupValues[1]) ?: return@mapNotNull null
                val timeText = match.groupValues[2]
                val time = if (timeText.isEmpty()) null else parseTime(timeText) ?: return@mapNotNull null
                ParsedReminder(
                    lineNumber = value.lineNumber,
                    title = title.ifBlank { value.text },
                    dateTime = LocalDateTime.of(date, time ?: options.defaultTime),
                    hasExplicitTime = time != null,
                    format = ReminderFormat.PROPERTY,
                    rawLine = value.rawLine,
                    lineHash = Lines.hash("property:${value.key.lowercase()}=${value.text}"),
                    source = ReminderSource.PROPERTY
                )
            }
    }

    /** Parses a single line. Returns null if it is not an open task with a supported date. */
    fun parseLine(
        lineNumber: Int,
        line: String
    ): ParsedReminder? {
        val task = TASK.matchEntire(line) ?: return null
        if (task.groups[STATUS]?.value != " ") return null
        val body = task.groups[BODY]?.value ?: return null

        for (format in ORDER) {
            if (format.reminderFormat !in options.enabledFormats) continue
            val match = format.regex.find(body) ?: continue
            val date = parseDate(match.groupValues[1]) ?: continue
            val timeText = match.groupValues.getOrNull(2).orEmpty()
            val time = if (timeText.isEmpty()) null else parseTime(timeText) ?: continue
            val title = cleanTitle(body)
            return ParsedReminder(
                lineNumber = lineNumber,
                title = title.ifEmpty { body.trim() },
                dateTime = LocalDateTime.of(date, time ?: options.defaultTime),
                hasExplicitTime = time != null,
                format = format.reminderFormat,
                rawLine = line,
                lineHash = Lines.hash(line),
                recurrence = recurrenceOf(body)
            )
        }
        return null
    }

    private fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text)
        } catch (e: DateTimeException) {
            null
        }

    private fun parseTime(text: String): LocalTime? {
        val parts = text.split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return LocalTime.of(h, m)
    }

    /** Removes every known date and repeat token (of any format) so the title reads cleanly. */
    private fun cleanTitle(text: String): String =
        (ORDER.map { it.regex } + REPEAT_TOKENS)
            .fold(text) { acc, regex -> regex.replace(acc, " ") }
            .replace(WHITESPACE, " ")
            .trim()

    private fun recurrenceOf(body: String): Recurrence? {
        val word = (REPEAT_TASKS.find(body) ?: REPEAT_SHORT.find(body))?.groupValues?.get(1)?.lowercase() ?: return null
        return when (word) {
            "day", "daily" -> Recurrence.DAILY
            "week", "weekly" -> Recurrence.WEEKLY
            "month", "monthly" -> Recurrence.MONTHLY
            "weekday", "weekdays" -> Recurrence.WEEKDAYS
            else -> null
        }
    }

    private data class FormatSpec(val reminderFormat: ReminderFormat, val regex: Regex)

    private companion object {
        const val STATUS = "status"
        const val BODY = "body"

        // Bullet (-, *, +) or ordered (1. / 1)) list item with a checkbox, optionally inside blockquotes.
        val TASK = Regex("""^[\s>]*(?:[-*+]|\d+[.)])\s+\[(?<status>.)]\s*(?<body>.*)$""")
        val FENCE = Regex("""^(```+|~~~+)""")
        val WHITESPACE = Regex("""\s+""")
        const val TITLE_PROPERTY = "title"

        /** Property value: date, optionally with time (`T` or space) and seconds. Time zones are not supported. */
        val PROPERTY_VALUE = Regex("""^(\d{4}-\d{2}-\d{2})(?:[ T](\d{1,2}:\d{2})(?::\d{2}(?:\.\d+)?)?)?$""")

        /** Tasks plugin rule (supported subset). Words like `when done` after the rule are ignored. */
        val REPEAT_TASKS = Regex("""🔁\uFE0F?\s*every\s+(weekday|day|week|month)\b""", RegexOption.IGNORE_CASE)

        /** Short form for Reminder plugin and Dataview lines. */
        val REPEAT_SHORT = Regex("""\|\s*(daily|weekly|monthly|weekdays)\b""", RegexOption.IGNORE_CASE)

        /** Removed from titles: the whole Tasks rule up to the next Tasks emoji or token, and the short form. */
        val REPEAT_TOKENS =
            listOf(
                Regex("""🔁\uFE0F?[^📅⏰⏳🛫✅❌➕🔺⏫🔼🔽⏬🆔⛔\[(#]*"""),
                REPEAT_SHORT
            )

        private const val DATE = """(\d{4}-\d{2}-\d{2})"""
        private const val TIME = """(\d{1,2}:\d{2})"""

        val ORDER =
            listOf(
                FormatSpec(ReminderFormat.REMINDER_PLUGIN, Regex("""\(@$DATE(?:[ T]$TIME)?\)""")),
                FormatSpec(
                    ReminderFormat.DATAVIEW,
                    Regex("""[\[(](?:due|remind|reminder)::\s*$DATE(?:[ T]$TIME)?\s*[\])]""", RegexOption.IGNORE_CASE)
                ),
                FormatSpec(ReminderFormat.TASKS_EMOJI, Regex("""⏰\uFE0F?\s*$DATE(?:[ T]$TIME)?""")),
                FormatSpec(ReminderFormat.TASKS_EMOJI, Regex("""📅\uFE0F?\s*$DATE"""))
            )
    }
}
