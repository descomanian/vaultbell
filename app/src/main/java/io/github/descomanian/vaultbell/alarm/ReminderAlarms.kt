package io.github.descomanian.vaultbell.alarm

import android.os.Bundle
import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.triggerAt
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.scheduler.AlarmScheduler
import io.github.descomanian.vaultbell.scheduler.ScheduleMode

/** Maps reminders to up to three alarms each: main, pre-reminder and "repeat if ignored". */
class ReminderAlarms(private val scheduler: AlarmScheduler) {
    fun canScheduleExact(): Boolean = scheduler.canScheduleExact()

    /** (Re)arms the alarms for [e]. Past times are not scheduled. Returns the mode of the main alarm, if any. */
    fun schedule(
        e: ReminderEntity,
        s: AppSettings,
        now: Long
    ): ScheduleMode? {
        cancel(e.id)
        if (e.completedLocally) return null
        var mode: ScheduleMode? = null
        val trigger = e.triggerAt
        if (trigger > now) mode = scheduler.schedule(code(e.id, KIND_MAIN), trigger, extras(e.id, KIND_MAIN, 0))
        if (s.preReminderEnabled && e.snoozedUntil == null) {
            val pre = e.dueAt - s.preReminderMinutes * 60_000L
            if (pre > now) scheduler.schedule(code(e.id, KIND_PRE), pre, extras(e.id, KIND_PRE, 0))
        }
        return mode
    }

    fun scheduleRepeat(
        id: Long,
        at: Long,
        count: Int
    ) {
        scheduler.schedule(code(id, KIND_REPEAT), at, extras(id, KIND_REPEAT, count))
    }

    fun cancelRepeat(id: Long) = scheduler.cancel(code(id, KIND_REPEAT))

    fun cancel(id: Long) {
        scheduler.cancel(code(id, KIND_MAIN))
        scheduler.cancel(code(id, KIND_PRE))
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
        const val KIND_PRE = 1
        const val KIND_REPEAT = 2
        const val MAX_REPEATS = 3
        const val REPEAT_INTERVAL_MINUTES = 10

        const val EXTRA_ID = "reminder_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_REPEAT = "repeat"

        fun code(
            id: Long,
            kind: Int
        ): Int = (id * 4 + kind).toInt()
    }
}
