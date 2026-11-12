package io.github.descomanian.vaultbell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every string exists in EN and DE with the same format arguments. Runs on the JVM, no device needed. */
class StringsParityTest {
    private val nameAndValue = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""")
    private val formatArg = Regex("""%\d\$[sd]""")

    private fun load(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings.xml")
        assertTrue("missing ${file.absolutePath}", file.exists())
        return nameAndValue.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun sameKeys() {
        val en = load("values")
        val de = load("values-de")
        assertEquals("keys only in EN", emptySet<String>(), en.keys - de.keys)
        assertEquals("keys only in DE", emptySet<String>(), de.keys - en.keys)
    }

    @Test
    fun sameFormatArguments() {
        val en = load("values")
        val de = load("values-de")
        val mismatches =
            en.filter { (key, value) ->
                formatArg.findAll(value).map { it.value }.sorted().toList() !=
                    formatArg.findAll(de[key].orEmpty()).map { it.value }.sorted().toList()
            }.keys
        assertEquals(emptySet<String>(), mismatches)
    }
}
