package io.github.descomanian.vaultbell.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

object ObsidianLinks {
    /** `obsidian://open?vault=<name>&file=<path>` — Obsidian resolves the path with or without `.md`. */
    fun openNote(
        vaultName: String,
        relativePath: String
    ): Uri = Uri.parse("obsidian://open?vault=${Uri.encode(vaultName)}&file=${Uri.encode(relativePath.removeSuffix(".md"))}")
}

data class SystemStatus(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val batteryUnrestricted: Boolean
) {
    companion object {
        fun read(context: Context): SystemStatus {
            val notifications =
                NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                    (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        )
            val exact =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            val battery = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
            return SystemStatus(notifications, exact, battery)
        }
    }
}

/** Intents into the matching system settings pages. */
object SystemIntents {
    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            appDetails(context)
        }

    /** System list of all apps' battery optimization (fallback). */
    fun batterySettings(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /**
     * One-tap system dialog "Allow app to always run in background?".
     * Justified for a reminder app: without it, OEM battery savers (e.g. Nothing OS) delay vault scans for hours.
     */
    @SuppressLint("BatteryLife")
    fun requestBatteryExemption(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    fun appDetails(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    GERMAN("de"),
    ENGLISH("en")
    ;

    companion object {
        fun current(): AppLanguage {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: SYSTEM
        }

        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                if (language == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag)
            )
        }
    }
}
