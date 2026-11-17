package io.github.descomanian.vaultbell.parser

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the parser against the fixtures in /sample-vault. */
class SampleVaultTest {
    private val vault = File(System.getProperty("vaultbell.sampleVault") ?: "../sample-vault")

    private fun parse(
        relative: String,
        options: ParserOptions = ParserOptions()
    ) = ReminderParser(options).parse(File(vault, relative).readText())

    private val allFormats = ParserOptions(enabledFormats = ReminderFormat.entries.toSet())

    @Test
    fun vaultExists() {
        assertTrue("sample vault not found at ${vault.absolutePath}", File(vault, ".obsidian").isDirectory)
    }

    @Test
    fun dailyNote() {
        val titles = parse("Daily/2026-11-14.md").map { it.title }
        assertEquals(listOf("Zahnarzt anrufen", "Wäsche abholen", "Sternchen-Liste", "Eingerückte Unteraufgabe"), titles)
        assertEquals(4, parse("Daily/2026-11-14.md", allFormats).size)
    }

    @Test
    fun projectNoteDefaultFormatsOnly() {
        assertEquals(
            listOf("Mehrere Formate", "Nummerierte Liste", "Im Zitat"),
            parse("Projekte/Vaultbell.md").map { it.title }
        )
    }

    @Test
    fun projectNoteAllFormats() {
        val result = parse("Projekte/Vaultbell.md", allFormats)
        assertEquals(
            listOf(
                "Release 0.1 taggen",
                "Screenshots machen",
                "F-Droid Merge Request",
                "Mehrere Formate",
                "Nummerierte Liste",
                "Im Zitat"
            ),
            result.map { it.title }
        )
    }

    @Test
    fun wholeVault() {
        val files = vault.walkTopDown().filter { it.isFile && it.extension == "md" && !it.path.contains("/.") }.toList()
        val count = files.sumOf { ReminderParser().parse(it.readText()).size }
        val countAll = files.sumOf { ReminderParser(allFormats).parse(it.readText()).size }
        assertEquals(12, count)
        assertEquals(21, countAll)
    }

    @Test
    fun propertyNote() {
        val result = ReminderParser(allFormats).parse(File(vault, "Projekte/Abgabe.md").readText(), noteName = "Abgabe")
        assertEquals(
            listOf("Abgabe Projektarbeit", "Abgabe Projektarbeit", "Abgabe Projektarbeit", "Folien fertig"),
            result.map { it.title }
        )
        assertEquals(
            listOf(ReminderSource.PROPERTY, ReminderSource.PROPERTY, ReminderSource.PROPERTY, ReminderSource.TASK_LINE),
            result.map { it.source }
        )
        assertEquals(listOf(3, 5, 6, 11), result.map { it.lineNumber })
    }

    @Test
    fun routines() {
        val result = ReminderParser(allFormats).parse(File(vault, "Routinen.md").readText())
        assertEquals(
            listOf(
                "Müll rausbringen" to Recurrence.WEEKLY,
                "Standup" to Recurrence.WEEKDAYS,
                "Miete überweisen" to Recurrence.MONTHLY,
                "Pflanzen gießen" to Recurrence.DAILY,
                "Review" to Recurrence.WEEKLY,
                "Alle zwei Wochen" to null
            ),
            result.map { it.title to it.recurrence }
        )
    }
}
