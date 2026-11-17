package io.github.descomanian.vaultbell.parser

/** One value of a frontmatter property with the line it was read from. */
internal data class PropertyValue(
    val key: String,
    val text: String,
    val lineNumber: Int,
    val rawLine: String
)

/** Frontmatter block: [endLine] is the index of the closing `---` / `...`. */
internal data class Frontmatter(
    val endLine: Int,
    val values: List<PropertyValue>
) {
    fun valuesOf(key: String): List<PropertyValue> = values.filter { it.key.equals(key, ignoreCase = true) }
}

/**
 * Minimal YAML reader for Obsidian properties, no dependency. Understands what Obsidian writes:
 * top-level `key: value` scalars (plain, 'single' or "double" quoted, with trailing `# comment`),
 * inline lists `key: [a, b]` and block lists (`key:` followed by `- item` lines).
 * Nested maps and multi-line strings are ignored. A block without closing delimiter is not frontmatter.
 */
internal object FrontmatterReader {
    private val KEY = Regex("""^([^\s#\-'"][^:]*?)\s*:(?:\s+(.*))?$""")
    private val ITEM = Regex("""^\s*-\s+(.*)$""")

    fun read(lines: List<String>): Frontmatter? {
        if (lines.firstOrNull()?.trim() != "---") return null
        val end = (1 until lines.size).firstOrNull { lines[it].trim().let { t -> t == "---" || t == "..." } } ?: return null
        val values = ArrayList<PropertyValue>()
        var listKey: String? = null
        for (i in 1 until end) {
            val line = lines[i]
            val item = ITEM.matchEntire(line)
            val key = KEY.matchEntire(line)
            val openList = listKey
            when {
                line.isBlank() || line.trimStart().startsWith("#") -> Unit
                item != null && openList != null -> scalar(item.groupValues[1])?.let { values += PropertyValue(openList, it, i, line) }
                key != null -> {
                    val name = key.groupValues[1].trim()
                    val rest = key.groupValues[2].trim()
                    listKey = if (rest.isEmpty()) name else null
                    values += valuesOf(rest).map { PropertyValue(name, it, i, line) }
                }
                else -> listKey = null
            }
        }
        return Frontmatter(end, values)
    }

    /** Values written on the key line itself: none, one scalar or an inline list. */
    private fun valuesOf(rest: String): List<String> =
        when {
            rest.isEmpty() -> emptyList()
            rest.startsWith("[") -> rest.removePrefix("[").substringBeforeLast("]").split(',').mapNotNull(::scalar)
            else -> listOfNotNull(scalar(rest))
        }

    /** Unquotes a scalar and drops a trailing comment. Returns null for empty values. */
    fun scalar(raw: String): String? {
        val text = raw.trim()
        val value =
            when {
                text.startsWith("\"") -> text.drop(1).substringBefore('"').replace("\\\\", "\\")
                text.startsWith("'") -> text.drop(1).replace("''", "\u0000").substringBefore('\'').replace("\u0000", "'")
                else -> text.substringBefore(" #").trim()
            }
        return value.trim().ifEmpty { null }
    }
}
