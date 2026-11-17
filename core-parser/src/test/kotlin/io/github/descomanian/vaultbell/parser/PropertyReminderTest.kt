package io.github.descomanian.vaultbell.parser

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyReminderTest {
    private val props = ReminderParser(ParserOptions(enabledFormats = setOf(ReminderFormat.PROPERTY, ReminderFormat.REMINDER_PLUGIN)))

    private fun parse(md: String) = props.parse(md, noteName = "Notiz")

    @Test
    fun disabledByDefault() {
        assertTrue(ReminderParser().parse("---\nreminder: 2026-11-20\n---\n").isEmpty())
    }

    @Test
    fun dateAndDateTimeVariants() {
        val md =
            """
            ---
            reminder: 2026-11-20
            due: 2026-11-21T18:30
            remind: '2026-11-22 07:05'
            ---
            """.trimIndent()
        val r = parse(md)
        assertEquals(
            listOf(LocalDateTime.of(2026, 11, 20, 9, 0), LocalDateTime.of(2026, 11, 21, 18, 30), LocalDateTime.of(2026, 11, 22, 7, 5)),
            r.map { it.dateTime }
        )
        assertEquals(listOf(false, true, true), r.map { it.hasExplicitTime })
        assertTrue(r.all { it.format == ReminderFormat.PROPERTY && it.source == ReminderSource.PROPERTY })
        assertEquals(listOf("Notiz", "Notiz", "Notiz"), r.map { it.title })
        assertEquals(listOf(1, 2, 3), r.map { it.lineNumber })
    }

    @Test
    fun secondsCommentsAndDoubleQuotes() {
        val md = "---\nreminder: \"2026-11-20T18:30:00\" # Abgabe\ndue: 2026-11-21 # nur Datum\n---\n"
        assertEquals(
            listOf(LocalDateTime.of(2026, 11, 20, 18, 30), LocalDateTime.of(2026, 11, 21, 9, 0)),
            parse(md).map { it.dateTime }
        )
    }

    @Test
    fun blockAndInlineLists() {
        val md =
            """
            ---
            reminder:
              - 2026-11-20T08:00
              - "2026-11-21"
            - 2026-11-22
            due: [2026-11-23, '2026-11-24T10:00']
            tags:
              - 2026-11-25
            ---
            """.trimIndent()
        val r = parse(md)
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 11, 20, 8, 0),
                LocalDateTime.of(2026, 11, 21, 9, 0),
                LocalDateTime.of(2026, 11, 22, 9, 0),
                LocalDateTime.of(2026, 11, 23, 9, 0),
                LocalDateTime.of(2026, 11, 24, 10, 0)
            ),
            r.map { it.dateTime }
        )
        assertEquals(listOf(2, 3, 4, 5, 5), r.map { it.lineNumber })
    }

    @Test
    fun titlePropertyWins() {
        val r = parse("---\ntitle: \"Abgabe: Bericht\"\nreminder: 2026-11-20\n---\n").single()
        assertEquals("Abgabe: Bericht", r.title)
    }

    @Test
    fun customNamesCaseInsensitive() {
        val parser =
            ReminderParser(ParserOptions(enabledFormats = setOf(ReminderFormat.PROPERTY), propertyNames = listOf("Deadline")))
        assertEquals(1, parser.parse("---\ndeadline: 2026-11-20\nreminder: 2026-11-21\n---\n").size)
    }

    @Test
    fun invalidValuesAndTimeZonesAreIgnored() {
        val md = "---\nreminder: morgen\ndue: 2026-02-30\nremind: 2026-11-20T25:00\nx: 2026-11-20\n---\n"
        assertTrue(parse(md).isEmpty())
        assertTrue(parse("---\nreminder: 2026-11-20T18:30Z\n---\n").isEmpty())
    }

    @Test
    fun withoutClosingDelimiterThereIsNoFrontmatter() {
        val md = "---\nreminder: 2026-11-20\n- [ ] Task (@2026-11-21 10:00)\n"
        val r = parse(md)
        assertEquals(listOf(ReminderSource.TASK_LINE), r.map { it.source })
        assertEquals(2, r.single().lineNumber)
    }

    @Test
    fun crlfAndDotsDelimiter() {
        val md = "---\r\nreminder: 2026-11-20T18:30\r\n...\r\n- [ ] a (@2026-11-21 10:00)\r\n"
        val r = parse(md)
        assertEquals(listOf(ReminderSource.PROPERTY, ReminderSource.TASK_LINE), r.map { it.source })
        assertEquals(listOf(1, 3), r.map { it.lineNumber })
    }

    @Test
    fun propertyAndTaskLinesGiveSeparateReminders() {
        val md = "---\nreminder: 2026-11-20T18:30\n---\n- [ ] Task (@2026-11-20 18:30)\n"
        assertEquals(2, parse(md).size)
    }

    @Test
    fun hashDependsOnKeyAndValueNotOnPosition() {
        val a = parse("---\nreminder: 2026-11-20\n---\n").single()
        val moved = parse("---\ntags: [x]\nreminder: 2026-11-20\n---\n").single()
        val changed = parse("---\nreminder: 2026-11-21\n---\n").single()
        assertEquals(a.lineHash, moved.lineHash)
        assertNotEquals(a.lineHash, changed.lineHash)
    }

    @Test
    fun frontmatterTasksAreNeverParsedAsTasks() {
        val md = "---\nnote: \"- [ ] fm (@2026-11-14 10:00)\"\n---\n"
        assertFalse(parse(md).any { it.source == ReminderSource.TASK_LINE })
    }
}
