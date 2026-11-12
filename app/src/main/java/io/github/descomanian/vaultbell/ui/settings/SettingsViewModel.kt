package io.github.descomanian.vaultbell.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.descomanian.vaultbell.AppContainer
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.system.AppLanguage
import io.github.descomanian.vaultbell.system.SystemStatus
import io.github.descomanian.vaultbell.work.ScanWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val status: SystemStatus = SystemStatus(notificationsAllowed = true, exactAlarmsAllowed = true, batteryUnrestricted = true),
    val scanning: Boolean = false,
    val activeCount: Int = 0,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val vaultFolderName: String? = null
)

class SettingsViewModel(
    private val app: Application,
    private val container: AppContainer
) : ViewModel() {
    private val repo = container.repository
    private val status = MutableStateFlow(SystemStatus.read(app))
    private val language = MutableStateFlow(AppLanguage.current())
    private val folderName = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> =
        combine(container.settings.settings, status, repo.scanning, repo.activeCount, combine(language, folderName, ::Pair)) {
                s,
                st,
                scanning,
                count,
                langAndFolder
            ->
            SettingsUiState(true, s, st, scanning, count, langAndFolder.first, langAndFolder.second ?: s.vaultName)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            val uri = container.settings.current().vaultUri ?: return@launch
            folderName.value =
                withContext(Dispatchers.IO) { runCatching { container.vault.inspect(Uri.parse(uri)).displayName }.getOrNull() }
        }
    }

    fun refreshStatus() {
        status.value = SystemStatus.read(app)
    }

    /** Plain change without side effects (appearance, toggles read at fire time). */
    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    /** Changes that alter parse results: the signature mismatch forces a full rescan. */
    fun updateAndRescan(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settings.update(transform)
            repo.scan()
        }
    }

    /** Changes that alter alarm times without re-parsing. */
    fun updateAndReschedule(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settings.update(transform)
            repo.rescheduleAll()
        }
    }

    fun setScanInterval(minutes: Int) {
        viewModelScope.launch {
            container.settings.update { it.copy(scanIntervalMinutes = minutes) }
            ScanWork.schedulePeriodic(app, minutes)
        }
    }

    fun setLanguage(value: AppLanguage) {
        language.value = value
        AppLanguage.apply(value)
    }

    fun scanNow() {
        viewModelScope.launch { repo.scan(force = true) }
    }

    fun changeVault(uri: Uri) {
        viewModelScope.launch {
            val info =
                withContext(Dispatchers.IO) {
                    runCatching {
                        container.vault.persistPermission(uri)
                        container.vault.inspect(uri)
                    }.getOrNull()
                } ?: return@launch
            val old = container.settings.current().vaultUri
            repo.clearIndex()
            if (old != null && old != uri.toString()) container.vault.releasePermission(Uri.parse(old))
            container.settings.update { it.copy(vaultUri = uri.toString(), vaultName = info.displayName) }
            folderName.value = info.displayName
            repo.scan(force = true)
        }
    }
}
