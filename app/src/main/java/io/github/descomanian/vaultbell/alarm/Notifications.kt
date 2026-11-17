package io.github.descomanian.vaultbell.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.data.db.ReminderEntity
import io.github.descomanian.vaultbell.data.db.triggerAt
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.system.ObsidianLinks

class Notifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val loud =
            NotificationChannel(CHANNEL_LOUD, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
            }
        val silent =
            NotificationChannel(CHANNEL_SILENT, context.getString(R.string.channel_silent), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_silent_desc)
                setSound(null, null)
                enableVibration(false)
            }
        // Alarm usage: Do Not Disturb lets alarms through by default and silent mode does not mute the alarm stream.
        val alarm =
            NotificationChannel(CHANNEL_ALARM, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(loud, silent, alarm))
    }

    fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                )

    @SuppressLint("MissingPermission")
    fun show(
        e: ReminderEntity,
        kind: Int,
        s: AppSettings
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val id = notificationId(e.id)
        val whenText =
            DateUtils.formatDateTime(
                context,
                e.triggerAt,
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
            )
        val text =
            if (kind == ReminderAlarms.KIND_PRE) {
                context.getString(R.string.notification_pre, s.preReminderMinutes, whenText)
            } else {
                whenText
            }

        val builder =
            NotificationCompat.Builder(context, channelFor(s))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(e.title)
                .setContentText(text)
                .setSubText(e.relativePath.removeSuffix(".md"))
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                // Do Not Disturb filters by category: ALARM passes by default, REMINDER does not.
                .setCategory(if (s.breakThroughSilence) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(e.triggerAt)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(kind == ReminderAlarms.KIND_REPEAT)
                .setContentIntent(openIntent(e, s))
                .setDeleteIntent(actionIntent(e.id, NotificationActionReceiver.ACTION_DISMISSED, 0))
                .addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.action_done),
                    actionIntent(e.id, NotificationActionReceiver.ACTION_DONE, 1)
                ).addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.action_snooze_minutes, s.snoozeShortMinutes),
                    actionIntent(e.id, NotificationActionReceiver.ACTION_SNOOZE_SHORT, 2)
                ).addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.action_snooze_minutes, s.snoozeLongMinutes),
                    actionIntent(e.id, NotificationActionReceiver.ACTION_SNOOZE_LONG, 3)
                )
        builder.setVisibility(if (s.hideOnLockScreen) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
        // Shown instead of the full notification when Android hides sensitive lock screen content: no title, no path.
        builder.setPublicVersion(publicVersion(e, s))
        manager.notify(id, builder.build())
    }

    private fun publicVersion(
        e: ReminderEntity,
        s: AppSettings
    ) = NotificationCompat.Builder(context, channelFor(s))
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.notification_public_title))
        .setContentText(DateUtils.formatDateTime(context, e.triggerAt, DateUtils.FORMAT_SHOW_TIME))
        .setCategory(if (s.breakThroughSilence) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
        .build()

    fun cancel(reminderId: Long) = manager.cancel(notificationId(reminderId))

    private fun openIntent(
        e: ReminderEntity,
        s: AppSettings
    ): PendingIntent {
        val intent =
            Intent(context, OpenNoteActivity::class.java)
                .putExtra(ReminderAlarms.EXTRA_ID, e.id)
                .putExtra(OpenNoteActivity.EXTRA_OBSIDIAN_URI, ObsidianLinks.openNote(s.vaultName, e.relativePath).toString())
                .putExtra(OpenNoteActivity.EXTRA_DOCUMENT_URI, e.fileUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            requestCode(e.id, 4),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(
        id: Long,
        action: String,
        slot: Int
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(id, slot),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderAlarms.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun channelFor(s: AppSettings): String =
        when {
            !s.soundAndVibration -> CHANNEL_SILENT
            s.breakThroughSilence -> CHANNEL_ALARM
            else -> CHANNEL_LOUD
        }

    companion object {
        const val CHANNEL_ALARM = "reminders_alarm"
        const val CHANNEL_LOUD = "reminders"
        const val CHANNEL_SILENT = "reminders_silent"

        fun notificationId(reminderId: Long): Int = reminderId.toInt()

        private fun requestCode(
            id: Long,
            slot: Int
        ): Int = (id * 8 + slot).toInt()
    }
}
