package io.github.descomanian.vaultbell

import android.app.Application
import android.content.Context
import io.github.descomanian.vaultbell.alarm.AlarmReceiver
import io.github.descomanian.vaultbell.alarm.Notifications
import io.github.descomanian.vaultbell.alarm.ReminderAlarms
import io.github.descomanian.vaultbell.data.ReminderRepository
import io.github.descomanian.vaultbell.data.db.VaultbellDatabase
import io.github.descomanian.vaultbell.data.settings.SettingsRepository
import io.github.descomanian.vaultbell.scheduler.AlarmScheduler
import io.github.descomanian.vaultbell.vault.VaultAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class VaultbellApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannels()
    }
}

/** Manual DI: one place, no annotation processing, easy to read. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob())
    val settings = SettingsRepository(context)
    val vault = VaultAccess(context)
    val notifications = Notifications(context.applicationContext)
    val alarmScheduler = AlarmScheduler(context, AlarmReceiver::class.java)
    private val alarms = ReminderAlarms(alarmScheduler)
    private val database = VaultbellDatabase.create(context)
    val repository = ReminderRepository(database.reminderDao(), settings, vault, alarms, notifications)
}
