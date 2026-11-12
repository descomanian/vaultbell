package io.github.descomanian.vaultbell.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderParserTest {
    private val all = ParserOptions(enabledFormats = ReminderFormat.entries.toSet(), defaultTime = LocalTime.of(9, 0))
    private val parser = ReminderParser()
    private val allParser = ReminderParser(all)

    @Test
    fun reminderPluginWithTime() {
        val r = parser.parseLine(3, "- [ ] Zahnarzt anrufen (@2026-11-14 18:30)")!!
        assertEquals("Zahnarzt anrufen", r.title)
        assertEquals(LocalDateTime.of(2026, 11, 14, 18, 30), r.dateTime)
        assertTrue(r.hasExplicitTime)
        assertEquals(3, r.lineNumber)
        assertEquals(ReminderFormat.REMINDER_PLUGIN, r.format)
    }

    @Test
    fun dateOnlyUsesDefaultTime() {
        val r = ReminderParser(ParserOptions(defaultTime = LocalTime.of(7, 45))).parseLine(0, "- [ ] Müll (@2026-11-14)")!!
        assertEquals(LocalDateTime.of(2026, 11, 14, 7, 45), r.dateTime)
        assertFalse(r.hasExplicitTime)
    }

    @Test
    fun singleDigitHourAndIsoSeparator() {
        assertEquals(LocalDateTime.of(2026, 1, 2, 7, 5), parser.parseLine(0, "- [ ] a (@2026-01-02 7:05)")!!.dateTime)
        assertEquals(LocalDateTime.of(2026, 1, 2, 7, 5), parser.parseLine(0, "- [ ] a (@2026-01-02T07:05)")!!.dateTime)
    }

    @Test
    fun ignoresDoneCancelledAndNonTasks() {
        assertNull(parser.parseLine(0, "- [x] erledigt (@2026-11-14 18:30)"))
        assertNull(parser.parseLine(0, "- [X] erledigt (@2026-11-14 18:30)"))
        assertNull(parser.parseLine(0, "- [-] abgebrochen (@2026-11-14 18:30)"))
        assertNull(parser.parseLine(0, "Nur Text (@2026-11-14 18:30)"))
        assertNull(parser.parseLine(0, "- Liste ohne Checkbox (@2026-11-14 18:30)"))
        assertNull(parser.parseLine(0, "- [ ] ohne Datum"))
    }

    @Test
    fun rejectsInvalidDatesAndTimes() {
        assertNull(parser.parseLine(0, "- [ ] a (@2026-02-30 10:00)"))
        assertNull(parser.parseLine(0, "- [ ] a (@2026-11-14 24:00)"))
        assertNull(parser.parseLine(0, "- [ ] a (@2026-11-14 12:60)"))
    }

    @Test
    fun listVariants() {
        assertEquals("Stern", parser.parseLine(0, "* [ ] Stern (@2026-11-14)")!!.title)
        assertEquals("Plus", parser.parseLine(0, "+ [ ] Plus (@2026-11-14)")!!.title)
        assertEquals("Nummer", parser.parseLine(0, "12. [ ] Nummer (@2026-11-14)")!!.title)
        assertEquals("Klammer", parser.parseLine(0, "3) [ ] Klammer (@2026-11-14)")!!.title)
        assertEquals("Tab", parser.parseLine(0, "\t\t- [ ] Tab (@2026-11-14)")!!.title)
        assertEquals("Zitat", parser.parseLine(0, "> - [ ] Zitat (@2026-11-14)")!!.title)
    }

    @Test
    fun titleKeepsTextAroundToken() {
        val r = parser.parseLine(0, "- [ ] Vor  (@2026-11-14 10:00)  nach #tag")!!
        assertEquals("Vor nach #tag", r.title)
    }

    @Test
    fun formatsAreDisabledByDefault() {
        assertNull(parser.parseLine(0, "- [ ] a [due:: 2026-11-14]"))
        assertNull(parser.parseLine(0, "- [ ] a ⏰ 2026-11-14 10:00"))
        assertNull(parser.parseLine(0, "- [ ] a 📅 2026-11-14"))
    }

    @Test
    fun dataviewVariants() {
        val a = allParser.parseLine(0, "- [ ] Release [due:: 2026-11-20 18:00]")!!
        assertEquals(ReminderFormat.DATAVIEW, a.format)
        assertEquals(LocalDateTime.of(2026, 11, 20, 18, 0), a.dateTime)
        assertEquals("Release", a.title)
        val b = allParser.parseLine(0, "- [ ] Paren (Reminder:: 2026-11-20)")!!
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), b.dateTime)
    }

    @Test
    fun tasksEmojiVariants() {
        val alarm = allParser.parseLine(0, "- [ ] Screens ⏰ 2026-11-21 10:15 #release")!!
        assertEquals(ReminderFormat.TASKS_EMOJI, alarm.format)
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 15), alarm.dateTime)
        assertEquals("Screens #release", alarm.title)
        val due = allParser.parseLine(0, "- [ ] F-Droid 📅 2026-11-30")!!
        assertEquals(LocalDateTime.of(2026, 11, 30, 9, 0), due.dateTime)
        assertFalse(due.hasExplicitTime)
        val vs16 = allParser.parseLine(0, "- [ ] VS16 ⏰️ 2026-11-21 10:15")!!
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 15), vs16.dateTime)
    }

    @Test
    fun reminderPluginWinsOverOtherFormats() {
        val r = allParser.parseLine(0, "- [ ] Mix (@2026-11-22 09:30) [due:: 2026-12-01]")!!
        assertEquals(ReminderFormat.REMINDER_PLUGIN, r.format)
        assertEquals(LocalDateTime.of(2026, 11, 22, 9, 30), r.dateTime)
    }

    @Test
    fun skipsFrontmatterAndCodeFences() {
        val md =
            """
            ---
            x: "- [ ] fm (@2026-11-14 10:00)"
            ---
            - [ ] real (@2026-11-14 10:00)
            ```
            - [ ] code (@2026-11-14 10:00)
            ```
            ~~~~md
            - [ ] tilde (@2026-11-14 10:00)
            ```
            - [ ] still tilde (@2026-11-14 10:00)
            ~~~~
            - [ ] after (@2026-11-14 11:00)
            """.trimIndent()
        val result = parser.parse(md)
        assertEquals(listOf("real", "after"), result.map { it.title })
        assertEquals(listOf(3, 12), result.map { it.lineNumber })
    }

    @Test
    fun lineNumbersWithCrLf() {
        val md = "# T\r\n\r\n- [ ] a (@2026-11-14 10:00)\r\n- [ ] b (@2026-11-14 11:00)\r\n"
        assertEquals(listOf(2, 3), parser.parse(md).map { it.lineNumber })
    }

    @Test
    fun hashChangesWithContentButIgnoresTrailingWhitespace() {
        val a = parser.parseLine(0, "- [ ] a (@2026-11-14 10:00)")!!
        val b = parser.parseLine(0, "- [ ] a (@2026-11-14 10:00)   ")!!
        val c = parser.parseLine(0, "- [ ] a (@2026-11-14 10:05)")!!
        assertEquals(a.lineHash, b.lineHash)
        assertNotEquals(a.lineHash, c.lineHash)
        assertEquals(16, a.lineHash.length)
        assertEquals(a.lineHash, lineHashOf("- [ ] a (@2026-11-14 10:00)"))
    }
}
