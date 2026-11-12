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
 * - YAML frontmatter and fenced code blocks are skipped.
 * - The first matching enabled format wins (Reminder plugin, Dataview, Tasks ⏰, Tasks 📅).
 */
class ReminderParser(
    private val options: ParserOptions = ParserOptions(),
) {
    fun parse(content: String): List<ParsedReminder> {
        val lines = Lines.split(content)
        val result = ArrayList<ParsedReminder>()
        var inFence: String? = null
        var inFrontmatter = lines.firstOrNull()?.trim() == "---"

        lines.forEachIndexed { index, line ->
            if (inFrontmatter) {
                if (index > 0 && line.trim().let { it == "---" || it == "..." }) inFrontmatter = false
                return@forEachIndexed
            }
            val trimmed = line.trimStart()
            val fence = FENCE.find(trimmed)?.value
            if (fence != null) {
                inFence =
                    when {
                        inFence == null -> fence.take(3)
                        fence.startsWith(inFence!!) -> null
                        else -> inFence
                    }
                return@forEachIndexed
            }
            if (inFence != null) return@forEachIndexed

            parseLine(index, line)?.let(result::add)
        }
        return result
    }

    /** Parses a single line. Returns null if it is not an open task with a supported date. */
    fun parseLine(
        lineNumber: Int,
        line: String,
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

    /** Removes every known date token (of any format) so the title reads cleanly. */
    private fun cleanTitle(text: String): String =
        ORDER.fold(text) { acc, spec -> spec.regex.replace(acc, " ") }.replace(WHITESPACE, " ").trim()

    private data class FormatSpec(val reminderFormat: ReminderFormat, val regex: Regex)

    private companion object {
        const val STATUS = "status"
        const val BODY = "body"

        // Bullet (-, *, +) or ordered (1. / 1)) list item with a checkbox, optionally inside blockquotes.
        val TASK = Regex("""^[\s>]*(?:[-*+]|\d+[.)])\s+\[(?<status>.)]\s*(?<body>.*)$""")
        val FENCE = Regex("""^(```+|~~~+)""")
        val WHITESPACE = Regex("""\s+""")

        private const val DATE = """(\d{4}-\d{2}-\d{2})"""
        private const val TIME = """(\d{1,2}:\d{2})"""

        val ORDER =
            listOf(
                FormatSpec(ReminderFormat.REMINDER_PLUGIN, Regex("""\(@$DATE(?:[ T]$TIME)?\)""")),
                FormatSpec(
                    ReminderFormat.DATAVIEW,
                    Regex("""[\[(](?:due|remind|reminder)::\s*$DATE(?:[ T]$TIME)?\s*[\])]""", RegexOption.IGNORE_CASE),
                ),
                FormatSpec(ReminderFormat.TASKS_EMOJI, Regex("""⏰\uFE0F?\s*$DATE(?:[ T]$TIME)?""")),
                FormatSpec(ReminderFormat.TASKS_EMOJI, Regex("""📅\uFE0F?\s*$DATE""")),
            )
    }
}
