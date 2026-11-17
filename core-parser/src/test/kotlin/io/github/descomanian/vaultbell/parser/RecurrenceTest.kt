package io.github.descomanian.vaultbell.parser

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceTest {
    private val all = ReminderParser(ParserOptions(enabledFormats = ReminderFormat.entries.toSet()))

    private fun at(
        y: Int,
        m: Int,
        d: Int,
        h: Int = 9,
        min: Int = 0
    ) = LocalDateTime.of(y, m, d, h, min)

    // ------------------------------------------------------------------ syntax

    @Test
    fun tasksSyntax() {
        assertEquals(Recurrence.DAILY, all.parseLine(0, "- [ ] a ⏰ 2026-11-18 08:00 🔁 every day")!!.recurrence)
        assertEquals(Recurrence.WEEKLY, all.parseLine(0, "- [ ] a 🔁 every week 📅 2026-11-18")!!.recurrence)
        assertEquals(Recurrence.MONTHLY, all.parseLine(0, "- [ ] a 📅 2026-11-18 🔁️ Every Month")!!.recurrence)
        assertEquals(Recurrence.WEEKDAYS, all.parseLine(0, "- [ ] a (@2026-11-18) 🔁 every weekday")!!.recurrence)
        assertEquals(Recurrence.WEEKLY, all.parseLine(0, "- [ ] a 📅 2026-11-18 🔁 every week when done")!!.recurrence)
    }

    @Test
    fun shortSyntax() {
        assertEquals(Recurrence.DAILY, all.parseLine(0, "- [ ] a (@2026-11-18 07:00) | daily")!!.recurrence)
        assertEquals(Recurrence.WEEKLY, all.parseLine(0, "- [ ] a (@2026-11-18) |weekly")!!.recurrence)
        assertEquals(Recurrence.MONTHLY, all.parseLine(0, "- [ ] a [due:: 2026-11-30] | Monthly")!!.recurrence)
        assertEquals(Recurrence.WEEKDAYS, all.parseLine(0, "- [ ] a | weekdays (@2026-11-18)")!!.recurrence)
    }

    @Test
    fun unsupportedRulesGiveOneOffReminders() {
        assertNull(all.parseLine(0, "- [ ] a (@2026-11-18) 🔁 every 2 weeks")!!.recurrence)
        assertNull(all.parseLine(0, "- [ ] a (@2026-11-18) | yearly")!!.recurrence)
        assertNull(all.parseLine(0, "- [ ] a (@2026-11-18)")!!.recurrence)
    }

    @Test
    fun titlesAreCleaned() {
        assertEquals("Müll", all.parseLine(0, "- [ ] Müll (@2026-11-18 07:00) | weekly")!!.title)
        assertEquals("Review #team", all.parseLine(0, "- [ ] Review 📅 2026-11-21 🔁 every week when done #team")!!.title)
        assertEquals("Zwei", all.parseLine(0, "- [ ] Zwei (@2026-11-18) 🔁 every 2 weeks")!!.title)
        assertEquals("Gießen", all.parseLine(0, "- [ ] Gießen 🔁 every day ⏰ 2026-11-18 08:00")!!.title)
    }

    @Test
    fun anchorStaysTheWrittenDate() {
        assertEquals(at(2026, 11, 18, 7, 0), all.parseLine(0, "- [ ] a (@2026-11-18 07:00) | daily")!!.dateTime)
    }

    // ------------------------------------------------------------------ occurrences

    @Test
    fun anchorInTheFutureIsTheFirstOccurrence() {
        val anchor = at(2026, 12, 1)
        assertEquals(anchor, Recurrence.DAILY.firstOnOrAfter(anchor, at(2026, 11, 1)))
        assertEquals(anchor, Recurrence.MONTHLY.firstOnOrAfter(anchor, at(2026, 11, 1)))
    }

    @Test
    fun dailyKeepsLocalTimeAcrossDaylightSaving() {
        val anchor = at(2026, 3, 27, 9, 0)
        assertEquals(at(2026, 3, 29, 9, 0), Recurrence.DAILY.nextAfter(anchor, at(2026, 3, 28, 9, 0)))
        assertEquals(at(2026, 10, 26, 9, 0), Recurrence.DAILY.firstOnOrAfter(anchor, at(2026, 10, 25, 9, 1)))
    }

    @Test
    fun nextAfterSkipsTheCurrentInstance() {
        val anchor = at(2026, 11, 17, 7, 0)
        assertEquals(at(2026, 11, 24, 7, 0), Recurrence.WEEKLY.nextAfter(anchor, anchor))
        assertEquals(at(2026, 11, 18, 7, 0), Recurrence.DAILY.nextAfter(anchor, anchor))
        assertEquals(anchor, Recurrence.WEEKLY.firstOnOrAfter(anchor, anchor))
    }

    @Test
    fun pastOccurrencesAreNotRepeated() {
        val anchor = at(2026, 1, 5, 7, 0) // Monday
        val now = at(2026, 11, 17, 12, 0) // Tuesday
        assertEquals(at(2026, 11, 23, 7, 0), Recurrence.WEEKLY.firstOnOrAfter(anchor, now))
        assertEquals(at(2026, 11, 18, 7, 0), Recurrence.DAILY.firstOnOrAfter(anchor, now))
    }

    @Test
    fun monthEndsClampAndRecover() {
        val anchor = at(2026, 1, 31, 18, 0)
        val feb = Recurrence.MONTHLY.nextAfter(anchor, anchor)
        assertEquals(at(2026, 2, 28, 18, 0), feb)
        val mar = Recurrence.MONTHLY.nextAfter(anchor, feb)
        assertEquals(at(2026, 3, 31, 18, 0), mar)
        assertEquals(at(2026, 4, 30, 18, 0), Recurrence.MONTHLY.nextAfter(anchor, mar))
    }

    @Test
    fun leapYear() {
        val anchor = at(2028, 1, 31)
        assertEquals(at(2028, 2, 29), Recurrence.MONTHLY.nextAfter(anchor, anchor))
        val leapDay = at(2028, 2, 29)
        assertEquals(at(2028, 3, 29), Recurrence.MONTHLY.nextAfter(leapDay, leapDay))
        assertEquals(at(2028, 3, 1), Recurrence.DAILY.nextAfter(leapDay, leapDay))
    }

    @Test
    fun weekdaysSkipWeekends() {
        val friday = at(2026, 11, 20, 9, 30)
        assertEquals(at(2026, 11, 23, 9, 30), Recurrence.WEEKDAYS.nextAfter(friday, friday))
        val saturdayAnchor = at(2026, 11, 21, 9, 30)
        assertEquals(at(2026, 11, 23, 9, 30), Recurrence.WEEKDAYS.firstOnOrAfter(saturdayAnchor, at(2026, 11, 1)))
        assertEquals(at(2026, 11, 24, 9, 30), Recurrence.WEEKDAYS.nextAfter(friday, at(2026, 11, 23, 9, 30)))
    }

    @Test
    fun weeklyOverYearBoundary() {
        val anchor = at(2026, 12, 28)
        assertEquals(at(2027, 1, 4), Recurrence.WEEKLY.nextAfter(anchor, anchor))
    }
}
