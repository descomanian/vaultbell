package io.github.descomanian.vaultbell.data

import io.github.descomanian.vaultbell.parser.Recurrence
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesTest {
    private fun at(
        d: Int,
        h: Int = 7,
        m: Int = 0
    ) = LocalDateTime.of(2026, 11, d, h, m)

    private val anchor = at(2) // Monday 2026-11-02 07:00

    @Test
    fun newSeriesStartsAtTodaysOrNextInstance() {
        // Tuesday 17th, 12:00: weekly on Mondays → next Monday
        assertEquals(at(23), Series.currentInstance(Recurrence.WEEKLY, anchor, null, null, at(17, 12)))
        // daily: today's 07:00 already passed but stays as overdue today
        assertEquals(at(17), Series.currentInstance(Recurrence.DAILY, anchor, null, null, at(17, 12)))
    }

    @Test
    fun storedInstanceSurvivesScansToday() {
        assertEquals(at(17), Series.currentInstance(Recurrence.DAILY, anchor, at(17), null, at(17, 23, 59)))
    }

    @Test
    fun missedInstancesAreSkippedNotCaughtUp() {
        // stored instance from Saturday, now Tuesday → today's instance
        assertEquals(at(17), Series.currentInstance(Recurrence.DAILY, anchor, at(14), null, at(17, 6)))
        // weekly instance from last Monday, now Tuesday → next Monday
        assertEquals(at(23), Series.currentInstance(Recurrence.WEEKLY, anchor, at(16), null, at(17, 6)))
    }

    @Test
    fun completedInstanceIsNeverRevived() {
        // today's 07:00 was done; a rescan (anchor changed → stored = null) must not bring it back
        assertEquals(at(18), Series.currentInstance(Recurrence.DAILY, anchor, null, at(17), at(17, 8)))
    }

    @Test
    fun doneEarlyOrLate() {
        // done before the instance: next one after the instance
        assertEquals(at(18), Series.nextAfterDone(Recurrence.DAILY, anchor, at(17), at(17, 6)))
        // done late (overdue since days): next one after now
        assertEquals(at(18), Series.nextAfterDone(Recurrence.DAILY, anchor, at(14), at(17, 12)))
        // weekdays: Friday done → Monday
        assertEquals(at(23), Series.nextAfterDone(Recurrence.WEEKDAYS, anchor, at(20), at(20, 8)))
    }
}
