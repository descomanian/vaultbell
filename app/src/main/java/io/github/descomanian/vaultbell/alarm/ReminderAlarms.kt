package io.github.descomanian.vaultbell.alarm

import android.os.Bundle
import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.triggerAt
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.data.toEpochMillis
import io.github.descomanian.vaultbell.data.toLocalDateTime
import io.github.descomanian.vaultbell.scheduler.AlarmScheduler
import io.github.descomanian.vaultbell.scheduler.ScheduleMode

/**
 * Maps reminders to alarms with chain scheduling: per reminder at most one main alarm, one plan alarm
 * (the next early reminder only; the following one is set when it fires) and one "repeat if ignored" alarm.
 * This keeps far below Android's limit of about 500 concurrent alarms per app (Android 12+); reminders further
 * away than [ARM_WINDOW_DAYS] are armed later by [ensureArmed] after scans.
 *
 * Request codes stay `id * 4 + kind`: with chain scheduling one slot per kind is enough.
 */
class ReminderAlarms(private val scheduler: AlarmScheduler) {
    fun canScheduleExact(): Boolean = scheduler.canScheduleExact()

    /** (Re)arms the alarms for [e]. Past times are not scheduled. Returns the mode of the main alarm, if any. */
    fun schedule(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ): ScheduleMode? {
        cancel(e.id)
        return arm(e, s, now)
    }

    /** Arms main and plan alarm if the main alarm is due inside the window and not pending yet. Leaves repeat alarms alone. */
    fun ensureArmed(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ) {
        if (!scheduler.isScheduled(code(e.id, KIND_MAIN))) arm(e, s, now)
    }

    private fun arm(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ): ScheduleMode? {
        if (e.completedLocally) return null
        val trigger = e.triggerAt
        if (trigger <= now || trigger > now + ARM_WINDOW_DAYS * DAY) return null
        val mode = scheduler.schedule(code(e.id, KIND_MAIN), trigger, extras(e.id, KIND_MAIN, 0))
        scheduleNextPlan(e, s, now)
        return mode
    }

    /** Next early reminder after now and after the last fired plan point. Snoozed reminders get no plan. */
    fun scheduleNextPlan(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ) {
        scheduler.cancel(code(e.id, KIND_PLAN))
        nextPlanPoint(e, s, now)?.let { scheduler.schedule(code(e.id, KIND_PLAN), it, extras(e.id, KIND_PLAN, 0)) }
    }

    fun nextPlanPoint(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ): Long? {
        if (e.completedLocally || e.snoozedUntil != null) return null
        val after = maxOf(now, e.planFiredAt ?: 0L)
        return ReminderPlan.next(e.dueAt.toLocalDateTime(), e.hasExplicitTime, s.planSettings, after.toLocalDateTime())?.toEpochMillis()
    }

    fun scheduleRepeat(
        id: Long,
        at: Long,
        count: Int
    ) {
        scheduler.schedule(code(id, KIND_REPEAT), at, extras(id, KIND_REPEAT, count))
    }

    /**
     * Wakes the app for a vault scan even in Doze. Inexact on purpose: Android batches it,
     * which keeps the battery impact tiny while being far more reliable than WorkManager alone.
     */
    fun scheduleScanHeartbeat(at: Long) {
        scheduler.schedule(SCAN_CODE, at, extras(-1L, KIND_SCAN, 0), exact = false)
    }

    fun cancelScanHeartbeat() = scheduler.cancel(SCAN_CODE)

    fun cancelRepeat(id: Long) = scheduler.cancel(code(id, KIND_REPEAT))

    fun cancelPlan(id: Long) = scheduler.cancel(code(id, KIND_PLAN))

    fun cancel(id: Long) {
        scheduler.cancel(code(id, KIND_MAIN))
        scheduler.cancel(code(id, KIND_PLAN))
        scheduler.cancel(code(id, KIND_REPEAT))
    }

    private fun extras(
        id: Long,
        kind: Int,
        repeat: Int
    ) = Bundle().apply {
        putLong(EXTRA_ID, id)
        putInt(EXTRA_KIND, kind)
        putInt(EXTRA_REPEAT, repeat)
    }

    companion object {
        const val KIND_MAIN = 0

        /** Early reminder from the plan (lead time or "Dranbleiben"). Same slot as the v1.0 early reminder. */
        const val KIND_PLAN = 1
        const val KIND_REPEAT = 2
        const val KIND_SCAN = 3

        /** Outside the id * 4 + kind range of real reminders. */
        const val SCAN_CODE = Int.MAX_VALUE - 7
        const val MAX_REPEATS = 3
        const val REPEAT_INTERVAL_MINUTES = 10
        const val ARM_WINDOW_DAYS = 14L
        private const val DAY = 24 * 60 * 60_000L

        const val EXTRA_ID = "reminder_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_REPEAT = "repeat"

        fun code(
            id: Long,
            kind: Int
        ): Int = (id * 4 + kind).toInt()
    }
}
