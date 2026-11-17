package io.github.descomanian.vaultbell.parser

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B2: write-back must never change anything but the checkbox of the indexed line. */
class WriteBackSafetyTest {
    private fun bytes(s: String) = s.toByteArray(Charsets.UTF_8)

    @Test
    fun mixedLineEndingsArePreserved() {
        val content = "# T\r\n- [ ] a (@2026-11-14)\n- [ ] b\r- [ ] c\r\n"
        val hash = lineHashOf("- [ ] b")
        val result = TaskLineEditor.markDoneInContent(content, 2, hash) as TaskLineEditor.EditResult.Success
        assertEquals("# T\r\n- [ ] a (@2026-11-14)\n- [x] b\r- [ ] c\r\n", result.content)
    }

    @Test
    fun missingTrailingNewlineStaysMissing() {
        val content = "a\n- [ ] last"
        val result = TaskLineEditor.markDoneInContent(content, 1, lineHashOf("- [ ] last")) as TaskLineEditor.EditResult.Success
        assertEquals("a\n- [x] last", result.content)
    }

    @Test
    fun spansMatchSplit() {
        val samples = listOf("", "a", "a\n", "a\r\nb", "a\rb\n\nc\r\n", "\n\n", "x\r\r\ny")
        for (s in samples) {
            val viaSpans = Lines.spans(s).map { s.substring(it.first, it.last + 1) }
            assertEquals("for ${s.replace("\r", "\\r").replace("\n", "\\n")}", Lines.split(s), viaSpans)
        }
    }

    @Test
    fun onlyTheCheckboxBytesChange() {
        val original = bytes("\uFEFF# Überschrift 🌱\r\n- [ ] Grüße (@2026-11-14)\n\tmehr ✓\r\n")
        val hash = lineHashOf("- [ ] Grüße (@2026-11-14)")
        val edited = TaskLineEditor.markDoneInFile(original, 1, hash) as TaskLineEditor.FileEditResult.Edited
        assertEquals(original.size, edited.bytes.size)
        val diff = original.indices.filter { original[it] != edited.bytes[it] }
        assertEquals(1, diff.size)
        assertEquals('x'.code.toByte(), edited.bytes[diff.single()])
        // BOM survives
        assertArrayEquals(original.copyOfRange(0, 3), edited.bytes.copyOfRange(0, 3))
    }

    @Test
    fun nonUtf8FileIsRejected() {
        // "- [ ] Grüße" in ISO-8859-1: ü = 0xFC, ß = 0xDF are invalid UTF-8 here.
        val latin1 = "- [ ] Grüße (@2026-11-14)\n".toByteArray(Charsets.ISO_8859_1)
        val hash = lineHashOf(String(latin1, Charsets.ISO_8859_1).trimEnd())
        val result = TaskLineEditor.markDoneInFile(latin1, 0, hash)
        assertEquals(TaskLineEditor.FileEditResult.Rejected(TaskLineEditor.EditResult.NotUtf8), result)
        assertNull(Utf8.decodeStrict(latin1))
    }

    @Test
    fun strictDecodeRoundTripsValidUtf8() {
        val original = bytes("\uFEFFa\r\nü 🌱 \u00A0 end")
        val decoded = Utf8.decodeStrict(original)
        assertTrue(decoded != null)
        assertArrayEquals(original, Utf8.encode(decoded!!))
    }
}
