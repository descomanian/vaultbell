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

        /** The file is not valid UTF-8; rewriting it could change characters. Nothing was changed. */
        data object NotUtf8 : EditResult
    }

    /**
     * Marks the task at [lineNumber] as done if its hash still equals [expectedHash].
     * Only that line is replaced in place: every line separator (also mixed CRLF/LF) and the trailing newline stay untouched.
     */
    fun markDoneInContent(
        content: String,
        lineNumber: Int,
        expectedHash: String
    ): EditResult {
        val span = Lines.spans(content).getOrNull(lineNumber) ?: return EditResult.Conflict
        val line = content.substring(span.first, span.last + 1)
        if (Lines.hash(line) != expectedHash) return EditResult.Conflict
        val edited = markDone(line) ?: return EditResult.NotATask
        return EditResult.Success(content.replaceRange(span.first, span.last + 1, edited))
    }

    /**
     * Byte-level variant for write-back: decodes strictly as UTF-8 (anything else is a [EditResult.NotUtf8] conflict)
     * and returns the new file bytes, which differ from [original] only inside the edited line.
     */
    fun markDoneInFile(
        original: ByteArray,
        lineNumber: Int,
        expectedHash: String
    ): FileEditResult {
        val content = Utf8.decodeStrict(original) ?: return FileEditResult.Rejected(EditResult.NotUtf8)
        return when (val edit = markDoneInContent(content, lineNumber, expectedHash)) {
            is EditResult.Success -> FileEditResult.Edited(Utf8.encode(edit.content))
            else -> FileEditResult.Rejected(edit)
        }
    }

    sealed interface FileEditResult {
        class Edited(val bytes: ByteArray) : FileEditResult

        data class Rejected(val reason: EditResult) : FileEditResult
    }
}
