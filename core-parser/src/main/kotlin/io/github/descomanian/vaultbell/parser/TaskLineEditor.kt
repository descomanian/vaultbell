package io.github.descomanian.vaultbell.parser

/** Minimal, conflict-aware edits to task lines. */
object TaskLineEditor {
    private val OPEN_BOX = Regex("""^([\s>]*(?:[-*+]|\d+[.)])\s+)\[ ]""")

    /** Turns `- [ ] x` into `- [x] x`. Returns null if the line is not an open task. */
    fun markDone(line: String): String? {
        val m = OPEN_BOX.find(line) ?: return null
        return line.replaceRange(m.range, m.groupValues[1] + "[x]")
    }

    sealed interface EditResult {
        data class Success(val content: String) : EditResult

        /** The line no longer matches the indexed hash (edited or synced elsewhere). Nothing was changed. */
        data object Conflict : EditResult

        data object NotATask : EditResult
    }

    /**
     * Marks the task at [lineNumber] as done if its hash still equals [expectedHash].
     * Line separators and the trailing newline of the file are preserved.
     */
    fun markDoneInContent(
        content: String,
        lineNumber: Int,
        expectedHash: String
    ): EditResult {
        val lines = Lines.split(content).toMutableList()
        val line = lines.getOrNull(lineNumber) ?: return EditResult.Conflict
        if (Lines.hash(line) != expectedHash) return EditResult.Conflict
        val edited = markDone(line) ?: return EditResult.NotATask
        lines[lineNumber] = edited
        return EditResult.Success(lines.joinToString(Lines.separatorOf(content)))
    }
}
