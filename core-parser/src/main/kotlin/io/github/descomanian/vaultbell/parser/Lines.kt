package io.github.descomanian.vaultbell.parser

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

internal object Lines {
    private val SPLIT = Regex("\r\n|\n|\r")

    fun split(content: String): List<String> = content.split(SPLIT)

    /**
     * Start (inclusive) and end (exclusive, separator not included) of every line, consistent with [split].
     * Lets edits replace one line in place so all other line endings stay exactly as they were.
     */
    fun spans(content: String): List<IntRange> {
        val spans = ArrayList<IntRange>()
        var start = 0
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\n' || c == '\r') {
                spans += start until i
                i += if (c == '\r' && content.getOrNull(i + 1) == '\n') 2 else 1
                start = i
            } else {
                i++
            }
        }
        spans += start until content.length
        return spans
    }

    fun hash(line: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(line.trimEnd().toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}

/** Public hash helper so the app computes identical hashes. */
fun lineHashOf(line: String): String = Lines.hash(line)

/** Strict UTF-8 handling for write-back: a file that is not valid UTF-8 is never rewritten. */
object Utf8 {
    /** Decodes [bytes] or returns null if they are not valid UTF-8. A BOM is kept as `\uFEFF`, so re-encoding is lossless. */
    fun decodeStrict(bytes: ByteArray): String? =
        try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            null
        }

    fun encode(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)
}
