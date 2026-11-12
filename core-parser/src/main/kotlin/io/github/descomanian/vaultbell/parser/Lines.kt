package io.github.descomanian.vaultbell.parser

import java.security.MessageDigest

internal object Lines {
    private val SPLIT = Regex("\r\n|\n|\r")

    fun split(content: String): List<String> = content.split(SPLIT)

    /** Returns the dominant line separator so edits keep the file's style. */
    fun separatorOf(content: String): String =
        when {
            content.contains("\r\n") -> "\r\n"
            content.contains('\n') -> "\n"
            content.contains('\r') -> "\r"
            else -> "\n"
        }

    fun hash(line: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(line.trimEnd().toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}

/** Public hash helper so the app computes identical hashes. */
fun lineHashOf(line: String): String = Lines.hash(line)
