package io.github.descomanian.vaultbell.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.scheduler.AlarmScheduler
import io.github.descomanian.vaultbell.work.ScanWork
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Runs [block] off the main thread while keeping the receiver alive (max ~10 s). */
private fun BroadcastReceiver.runAsync(
    context: Context,
    block: suspend (VaultbellApplication) -> Unit
) {
    val app = context.applicationContext as VaultbellApplication
    val pending = goAsync()
    app.container.appScope.launch {
        try {
            block(app)
        } finally {
            pending.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val kind = intent.getIntExtra(ReminderAlarms.EXTRA_KIND, ReminderAlarms.KIND_MAIN)
        if (kind == ReminderAlarms.KIND_SCAN) {
            // Scan inline while the alarm keeps the app awake; large vaults fall back to WorkManager.
            runAsync(context) { app ->
                val done = withTimeoutOrNull(SCAN_BUDGET_MS) { app.container.repository.scan() }
                if (done == null) ScanWork.runNow(context)
            }
            return
        }
        val id = intent.getLongExtra(ReminderAlarms.EXTRA_ID, -1L)
        if (id < 0) return
        val repeat = intent.getIntExtra(ReminderAlarms.EXTRA_REPEAT, 0)
        runAsync(context) { it.container.repository.onAlarmFired(id, kind, repeat) }
    }

    private companion object {
        const val SCAN_BUDGET_MS = 8_000L
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val id = intent.getLongExtra(ReminderAlarms.EXTRA_ID, -1L)
        if (id < 0) return
        runAsync(context) { app ->
            val repo = app.container.repository
            repo.onNotificationHandled(id)
            val s = app.container.settings.current()
            when (intent.action) {
                ACTION_DONE -> repo.markDone(id)
                ACTION_SNOOZE_SHORT -> repo.snooze(id, s.snoozeShortMinutes)
                ACTION_SNOOZE_LONG -> repo.snooze(id, s.snoozeLongMinutes)
                ACTION_DISMISSED -> Unit
            }
        }
    }

    companion object {
        const val ACTION_DONE = "io.github.descomanian.vaultbell.action.DONE"
        const val ACTION_SNOOZE_SHORT = "io.github.descomanian.vaultbell.action.SNOOZE_SHORT"
        const val ACTION_SNOOZE_LONG = "io.github.descomanian.vaultbell.action.SNOOZE_LONG"
        const val ACTION_DISMISSED = "io.github.descomanian.vaultbell.action.DISMISSED"
    }
}

/** Boot, app update, clock/time zone changes and exact-alarm permission changes. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            -> runAsync(context) { app ->
                app.container.repository.rescheduleAll()
                ScanWork.schedulePeriodic(context, app.container.settings.current().scanIntervalMinutes)
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
            -> {
                // Plan points and series are local times: re-arm now, the forced scan recomputes due times.
                runAsync(context) { app -> app.container.repository.rescheduleAll() }
                ScanWork.runNow(context, force = true)
            }
        }
    }
}
