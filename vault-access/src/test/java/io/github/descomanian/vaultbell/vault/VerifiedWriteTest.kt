package io.github.descomanian.vaultbell.vault

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VerifiedWriteTest {
    private val original = "- [ ] a\r\n".toByteArray()
    private val updated = "- [x] a\r\n".toByteArray()

    /** In-memory "file" whose write can be broken on purpose. */
    private class FakeFile(var content: ByteArray) {
        var failWrites = 0
        var truncateOnFail = true

        fun write(bytes: ByteArray) {
            if (failWrites > 0) {
                failWrites--
                if (truncateOnFail) content = bytes.copyOf(bytes.size / 2)
                throw IOException("disk full")
            }
            content = bytes.copyOf()
        }
    }

    @Test
    fun writesAndVerifies() {
        val file = FakeFile(original)
        val outcome = VerifiedWrite(file::write) { file.content }.replace(original, updated)
        assertEquals(WriteOutcome.WRITTEN, outcome)
        assertArrayEquals(updated, file.content)
    }

    @Test
    fun restoresOriginalAfterTruncatedWrite() {
        val file = FakeFile(original).apply { failWrites = 1 }
        val outcome = VerifiedWrite(file::write) { file.content }.replace(original, updated)
        assertEquals(WriteOutcome.ROLLED_BACK, outcome)
        assertArrayEquals(original, file.content)
    }

    @Test
    fun restoresWhenReadBackDiffers() {
        val file = FakeFile(original)
        var reads = 0
        val outcome =
            VerifiedWrite(file::write) {
                reads++
                if (reads == 1) "- [x] a\n".toByteArray() else file.content
            }.replace(original, updated)
        assertEquals(WriteOutcome.ROLLED_BACK, outcome)
        assertArrayEquals(original, file.content)
    }

    @Test
    fun reportsDamageWhenRestoreFailsToo() {
        val file = FakeFile(original).apply { failWrites = 2 }
        val outcome = VerifiedWrite(file::write) { file.content }.replace(original, updated)
        assertEquals(WriteOutcome.DAMAGED, outcome)
    }

    @Test
    fun readLimitedAcceptsExactLimitAndRejectsMore() {
        val data = ByteArray(10) { it.toByte() }
        assertArrayEquals(data, readLimited(ByteArrayInputStream(data), 10))
        try {
            readLimited(ByteArrayInputStream(data), 9)
            throw AssertionError("expected FileTooLargeException")
        } catch (e: FileTooLargeException) {
            // expected
        }
    }
}
