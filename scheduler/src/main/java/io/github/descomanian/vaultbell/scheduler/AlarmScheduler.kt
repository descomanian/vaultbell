package io.github.descomanian.vaultbell.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Whether an alarm is set as exact alarm.
 * [canScheduleExactAlarms] is only called on Android 12+ (API 31), where the method exists,
 * and never for inexact requests such as the scan heartbeat.
 */
fun shouldScheduleExact(
    exact: Boolean,
    sdkInt: Int,
    canScheduleExactAlarms: () -> Boolean
): Boolean = exact && (sdkInt < API_EXACT_ALARM_PERMISSION || canScheduleExactAlarms())

/** Android 12 (S): exact alarms need the SCHEDULE_EXACT_ALARM permission from here on. */
const val API_EXACT_ALARM_PERMISSION = 31

enum class ScheduleMode {
    /** setExactAndAllowWhileIdle: fires on the minute, even in Doze. */
    EXACT,

    /** setAndAllowWhileIdle: may be delayed by the system (exact alarms not permitted). */
    INEXACT
}

/**
 * Thin, testable wrapper around [AlarmManager].
 * The receiving [BroadcastReceiver] lives in the app module and is passed in, so this module has no UI dependencies.
 */
class AlarmScheduler(
    context: Context,
    private val receiver: Class<out BroadcastReceiver>
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Schedules (or replaces) the alarm with [requestCode].
     * Falls back to an inexact alarm if exact alarms are not allowed.
     */
    fun schedule(
        requestCode: Int,
        triggerAtMillis: Long,
        extras: Bundle,
        exact: Boolean = true
    ): ScheduleMode {
        val pending = pendingIntent(requestCode, extras)
        if (shouldScheduleExact(exact, Build.VERSION.SDK_INT, ::canScheduleExact)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                return ScheduleMode.EXACT
            } catch (e: SecurityException) {
                // Permission revoked between check and call: fall through to inexact.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        return ScheduleMode.INEXACT
    }

    fun cancel(requestCode: Int) {
        val intent = baseIntent(requestCode)
        val pending =
            PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun baseIntent(requestCode: Int) =
        Intent(appContext, receiver)
            .setAction(ACTION_FIRE)
            // Data makes each alarm's Intent unique beyond the request code.
            .setData(android.net.Uri.parse("vaultbell://alarm/$requestCode"))

    private fun pendingIntent(
        requestCode: Int,
        extras: Bundle
    ): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            baseIntent(requestCode).putExtras(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_FIRE = "io.github.descomanian.vaultbell.action.ALARM_FIRE"
    }
}
