package io.github.descomanian.vaultbell.ui.onboarding

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.descomanian.vaultbell.AppContainer
import io.github.descomanian.vaultbell.system.SystemStatus
import io.github.descomanian.vaultbell.work.ScanWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnboardingStep { WELCOME, VAULT, NOTIFICATIONS, EXACT_ALARMS, DONE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val vaultUri: String? = null,
    val vaultFolderName: String = "",
    val vaultName: String = "",
    val hasObsidianFolder: Boolean = false,
    val vaultError: Boolean = false,
    val notificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false
) {
    val canContinue: Boolean
        get() = step != OnboardingStep.VAULT || (vaultUri != null && vaultName.isNotBlank())
}

class OnboardingViewModel(
    private val app: Application,
    private val container: AppContainer
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val status = SystemStatus.read(app)
        _state.update { it.copy(notificationsGranted = status.notificationsAllowed, exactAlarmsGranted = status.exactAlarmsAllowed) }
    }

    fun onVaultPicked(uri: Uri) {
        viewModelScope.launch {
            val info =
                withContext(Dispatchers.IO) {
                    runCatching {
                        container.vault.persistPermission(uri)
                        container.vault.inspect(uri)
                    }.getOrNull()
                }
            _state.update {
                if (info == null) {
                    it.copy(vaultError = true)
                } else {
                    it.copy(
                        vaultUri = uri.toString(),
                        vaultFolderName = info.displayName,
                        vaultName = info.displayName,
                        hasObsidianFolder = info.hasObsidianFolder,
                        vaultError = false
                    )
                }
            }
        }
    }

    fun onVaultNameChange(name: String) = _state.update { it.copy(vaultName = name) }

    fun next() =
        _state.update {
            if (!it.canContinue) {
                it
            } else {
                it.copy(
                    step = OnboardingStep.entries[(it.step.ordinal + 1).coerceAtMost(OnboardingStep.entries.lastIndex)]
                )
            }
        }

    fun back() = _state.update { it.copy(step = OnboardingStep.entries[(it.step.ordinal - 1).coerceAtLeast(0)]) }

    fun finish(onFinished: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            container.settings.update {
                it.copy(onboardingDone = true, vaultUri = s.vaultUri, vaultName = s.vaultName.trim())
            }
            ScanWork.schedulePeriodic(app, container.settings.current().scanIntervalMinutes)
            // First scan runs in the app scope so it survives leaving this screen.
            container.appScope.launch { container.repository.scan(force = true) }
            onFinished()
        }
    }
}
