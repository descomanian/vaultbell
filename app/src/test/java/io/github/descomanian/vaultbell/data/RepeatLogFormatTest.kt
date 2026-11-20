package io.github.descomanian.vaultbell.data

import io.github.descomanian.vaultbell.parser.ParserOptions
import io.github.descomanian.vaultbell.parser.ReminderFormat
import io.github.descomanian.vaultbell.parser.ReminderParser
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatLogFormatTest {
    private val done = LocalDateTime.of(2026, 11, 20, 9, 31)
    private val labels = RepeatLogFormat.Labels(heading = "Vaultbell log", intro = "Completed dates of repeating tasks.")

    private fun entry(
        title: String = "Pflanzen gießen",
        path: String = "Routinen.md",
        rule: String = "daily",
        explicitTime: Boolean = true
    ) = RepeatLogFormat.Entry(title, path, rule, LocalDateTime.of(2026, 11, 20, 9, 0), explicitTime, done)

    @Test
    fun lineIsADoneTaskWithFields() {
        assertEquals(
            "- [x] Pflanzen gießen ✅ 2026-11-20 [instance:: 2026-11-20 09:00] [rule:: daily] " +
                "[done-at:: 2026-11-20 09:31] [note:: [[Routinen]]]",
            RepeatLogFormat.line(entry())
        )
    }

    @Test
    fun dateOnlyUnsupportedRuleAndFolders() {
        assertEquals(
            "- [x] Müll rausbringen ✅ 2026-11-20 [instance:: 2026-11-20] [rule:: every 2 weeks] " +
                "[done-at:: 2026-11-20 09:31] [note:: [[Haushalt/Wohnung]]]",
            RepeatLogFormat.line(entry("Müll rausbringen", "Haushalt/Wohnung.md", "every 2 weeks", explicitTime = false))
        )
    }

    @Test
    fun emptyRuleIsLeftOutAndBracketsAreRemoved() {
        val line = RepeatLogFormat.line(entry(rule = "[every] (week)"))
        assertTrue(line, line.contains("[rule:: every week]"))
        assertTrue(!RepeatLogFormat.line(entry(rule = "")).contains("rule::"))
    }

    @Test
    fun documentHasFrontmatterHeadingAndSortedEntries() {
        val later = entry("Team-Retro").copy(done = done.plusHours(5))
        val text = RepeatLogFormat.document(listOf(later, entry()), done.plusHours(6), labels)
        val head = "---\ntags: [vaultbell-log]\nexported: 2026-11-20T15:31\nentries: 2\n---\n\n"
        assertTrue(text, text.startsWith(head + "# Vaultbell log · 2026-11-20 15:31\n\nCompleted dates of repeating tasks.\n\n- [x] "))
        assertTrue(text.indexOf("Pflanzen gießen") < text.indexOf("Team-Retro"))
        assertTrue(text.endsWith("\n"))
    }

    @Test
    fun fileNameIsUniquePerMinute() {
        assertEquals("vaultbell-log-2026-11-20-0931.md", RepeatLogFormat.fileName(done))
    }

    /** The exported file may land in the scanned vault: no line may become a reminder again, in any format. */
    @Test
    fun exportedLinesAreNeverParsedAsReminders() {
        val parser = ReminderParser(ParserOptions(enabledFormats = ReminderFormat.entries.toSet()))
        val text = RepeatLogFormat.document(listOf(entry(), entry(rule = "every 2 weeks", explicitTime = false)), done, labels)
        assertEquals(emptyList<Any>(), parser.parse(text, noteName = "vaultbell-log-2026-11-20-0931"))
    }
}
