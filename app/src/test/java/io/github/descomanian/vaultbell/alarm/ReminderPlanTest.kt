package io.github.descomanian.vaultbell.alarm

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanTest {
    private fun at(
        h: Int,
        m: Int = 0,
        d: Int = 20
    ) = LocalDateTime.of(2026, 11, d, h, m)

    private val due = at(20)
    private val stages = listOf(NagStage(24 * 60, 120), NagStage(120, 60), NagStage(60, 30))
    private val countdown = listOf(120, 60, 30, 15)

    @Test
    fun countdownOnly() {
        val p = PlanSettings(offsetsMinutes = countdown)
        assertEquals(listOf(at(18), at(19), at(19, 30), at(19, 45)), ReminderPlan.points(due, true, p))
    }

    @Test
    fun stagesGetDenserTowardsDueTime() {
        val p = PlanSettings(nagStages = stages, dayStart = LocalTime.of(9, 0))
        assertEquals(
            listOf(at(10), at(12), at(14), at(16), at(18), at(19), at(19, 30)),
            ReminderPlan.points(due, true, p)
        )
    }

    @Test
    fun stageOrderInSettingsDoesNotMatter() {
        val sorted = PlanSettings(nagStages = stages, dayStart = LocalTime.of(9, 0))
        val reversed = sorted.copy(nagStages = stages.reversed())
        assertEquals(ReminderPlan.points(due, true, sorted), ReminderPlan.points(due, true, reversed))
    }

    @Test
    fun singleStageRastersBackwardsFromDue() {
        val p = PlanSettings(nagStages = listOf(NagStage(24 * 60, 120)), dayStart = LocalTime.of(9, 0))
        assertEquals(listOf(at(10), at(12), at(14), at(16), at(18)), ReminderPlan.points(due, true, p))
    }

    @Test
    fun combinedPlanDedupesNearbyPoints() {
        val p = PlanSettings(offsetsMinutes = countdown, nagStages = stages, dayStart = LocalTime.of(9, 0))
        // 18, 19, 19:30 come from both; 19:45 only from the countdown
        assertEquals(
            listOf(at(10), at(12), at(14), at(16), at(18), at(19), at(19, 30), at(19, 45)),
            ReminderPlan.points(due, true, p)
        )
        val nearby = PlanSettings(offsetsMinutes = listOf(125), nagStages = listOf(NagStage(240, 120)), dayStart = LocalTime.of(9, 0))
        // 17:55 (lead time) wins over 18:00 (stage)
        assertEquals(listOf(at(16), at(17, 55)), ReminderPlan.points(due, true, nearby))
    }

    @Test
    fun noStagePointsBeforeDayStartOrForDateOnlyReminders() {
        val p = PlanSettings(nagStages = stages, dayStart = LocalTime.of(15, 0))
        assertEquals(listOf(at(16), at(18), at(19), at(19, 30)), ReminderPlan.points(due, true, p))
        assertTrue(ReminderPlan.points(at(9), false, PlanSettings(nagStages = stages)).isEmpty())
        // lead times still apply without explicit time
        assertEquals(listOf(at(8, 45)), ReminderPlan.points(at(9), false, PlanSettings(offsetsMinutes = listOf(15))))
    }

    @Test
    fun earlyMorningDueTimeHasNoStagesBeforeDayStart() {
        val p = PlanSettings(nagStages = stages, dayStart = LocalTime.of(9, 0))
        assertTrue(ReminderPlan.points(at(8, 30), true, p).isEmpty())
    }

    @Test
    fun activeHoursDropPointsAtNight() {
        val p = PlanSettings(offsetsMinutes = countdown, activeHours = ActiveHours(LocalTime.of(8, 0), LocalTime.of(22, 0)))
        // due 23:30: 21:30 stays, 22:30, 23:00, 23:15 are in the quiet hours
        assertEquals(listOf(at(21, 30)), ReminderPlan.points(at(23, 30), true, p))
        // due 08:30: 06:30, 07:30 quiet; 08:00 and 08:15 allowed
        assertEquals(listOf(at(8), at(8, 15)), ReminderPlan.points(at(8, 30), true, p))
    }

    @Test
    fun activeHoursAcrossMidnight() {
        val night = ActiveHours(LocalTime.of(20, 0), LocalTime.of(6, 0))
        assertTrue(night.allows(LocalTime.of(23, 0)))
        assertTrue(night.allows(LocalTime.of(5, 59)))
        assertTrue(!night.allows(LocalTime.of(12, 0)))
        assertTrue(ActiveHours(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT).allows(LocalTime.NOON))
    }

    @Test
    fun nextSkipsThePastAndFiredPoints() {
        val p = PlanSettings(offsetsMinutes = countdown)
        assertEquals(at(19, 30), ReminderPlan.next(due, true, p, after = at(19, 10)))
        assertEquals(at(19, 45), ReminderPlan.next(due, true, p, after = at(19, 30)))
        assertNull(ReminderPlan.next(due, true, p, after = at(19, 45)))
        // instance created 40 min before due: only 30 and 15 are left
        assertEquals(listOf(at(19, 30), at(19, 45)), ReminderPlan.points(due, true, p).filter { it.isAfter(at(19, 20)) })
    }

    @Test
    fun daylightSavingDayKeepsLocalTimes() {
        val dstDue = LocalDateTime.of(2026, 3, 29, 12, 0)
        val p = PlanSettings(offsetsMinutes = listOf(180), nagStages = listOf(NagStage(600, 120)), dayStart = LocalTime.of(1, 0))
        val hours = ReminderPlan.points(dstDue, true, p).map { it.hour }
        assertEquals(listOf(2, 4, 6, 8, 9, 10), hours)
    }
}
