package io.github.descomanian.vaultbell.parser

/**
 * Detects repeat markers of any kind, including rules Vaultbell cannot compute (`🔁 every 2 weeks`, `[repeat:: …]`).
 *
 * A task with such a marker is never written back: checking it off in the note would end the series for Obsidian
 * (the Tasks plugin only creates the next occurrence when the task is toggled inside Obsidian). Completions of
 * repeating tasks go to the repeat log instead.
 */
object RepeatMarkers {
    /** Tasks plugin emoji rule; everything up to the next Tasks emoji or token is the rule text. */
    private val TASKS = Regex("""🔁\uFE0F?\s*([^📅⏰⏳🛫✅❌➕🔺⏫🔼🔽⏬🆔⛔\[(#]*)""")

    /** Tasks plugin Dataview format and similar inline fields. */
    private val DATAVIEW = Regex("""[\[(]\s*(?:repeat|recurrence|recurring)::\s*([^\])]*)[\])]""", RegexOption.IGNORE_CASE)

    /** Vaultbell short form; `yearly` and `| every …` are no supported rules but still mark a repeating task. */
    private val SHORT = Regex("""\|\s*(daily|weekly|monthly|weekdays|yearly|every\s[^|\])]*)""", RegexOption.IGNORE_CASE)

    fun isRepeating(line: String): Boolean = line.contains(EMOJI) || DATAVIEW.containsMatchIn(line) || SHORT.containsMatchIn(line)

    /** The rule as written, e.g. `every 2 weeks` or `daily`, for the repeat log. Null if the line has no marker. */
    fun ruleText(line: String): String? {
        val match = TASKS.find(line) ?: DATAVIEW.find(line) ?: SHORT.find(line) ?: return null
        return match.groupValues[1].trim()
    }

    private const val EMOJI = "🔁"
}
