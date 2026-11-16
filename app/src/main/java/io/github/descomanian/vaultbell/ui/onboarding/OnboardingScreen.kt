package io.github.descomanian.vaultbell.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.InfoCard
import io.github.descomanian.vaultbell.designsystem.component.StatusLevel
import io.github.descomanian.vaultbell.designsystem.component.StatusLight
import io.github.descomanian.vaultbell.designsystem.component.StepIndicator
import io.github.descomanian.vaultbell.designsystem.component.TonalIcon
import io.github.descomanian.vaultbell.designsystem.rememberHaptics
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme

data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onPickVault: () -> Unit = {},
    val onVaultNameChange: (String) -> Unit = {},
    val onRequestNotifications: () -> Unit = {},
    val onRequestExactAlarms: () -> Unit = {},
    val onRequestBackground: () -> Unit = {},
    val onFinish: () -> Unit = {}
)

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val steps = OnboardingStep.entries
    BackHandler(enabled = state.step.ordinal > 0, onBack = actions.onBack)

    Scaffold(modifier = modifier, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            StepIndicator(current = state.step.ordinal, total = steps.size)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.onboarding_step, state.step.ordinal + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "onboarding"
            ) { step ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = Spacing.xl)
                    ) {
                        when (step) {
                            OnboardingStep.WELCOME -> WelcomePage()
                            OnboardingStep.VAULT -> VaultPage(state, actions)
                            OnboardingStep.NOTIFICATIONS -> NotificationsPage(state, actions)
                            OnboardingStep.EXACT_ALARMS -> ExactAlarmsPage(state, actions)
                            OnboardingStep.BACKGROUND -> BackgroundPage(state, actions)
                            OnboardingStep.DONE -> DonePage(state)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = state.step.ordinal > 0) {
                    TextButton(onClick = actions.onBack) { Text(stringResource(R.string.action_back)) }
                }
                Spacer(Modifier.weight(1f))
                val last = state.step == OnboardingStep.DONE
                Button(
                    onClick = {
                        haptics.confirm()
                        if (last) actions.onFinish() else actions.onNext()
                    },
                    enabled = state.canContinue,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        stringResource(
                            when {
                                last -> R.string.onboarding_start
                                state.step == OnboardingStep.WELCOME -> R.string.onboarding_lets_go
                                else -> R.string.action_continue
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeader(
    icon: ImageVector,
    title: String,
    body: String
) {
    TonalIcon(
        icon,
        size = 72.dp,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    Spacer(Modifier.height(Spacing.lg))
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Spacing.sm))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Spacing.lg))
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        TonalIcon(icon, size = 40.dp)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GrantedRow(
    granted: Boolean,
    grantedText: String,
    missingText: String
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            val label = if (granted) grantedText else missingText
            StatusLight(if (granted) StatusLevel.OK else StatusLevel.WARNING, label)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WelcomePage() {
    PageHeader(
        Icons.Rounded.NotificationsActive,
        stringResource(R.string.onboarding_welcome_title, stringResource(R.string.app_name)),
        stringResource(R.string.onboarding_welcome_text)
    )
    FeatureRow(Icons.Rounded.Description, stringResource(R.string.onboarding_feature_markdown))
    FeatureRow(Icons.Rounded.CloudOff, stringResource(R.string.onboarding_feature_offline))
    FeatureRow(Icons.Rounded.Lock, stringResource(R.string.onboarding_feature_private))
    Spacer(Modifier.height(Spacing.lg))
    Text(
        stringResource(R.string.not_affiliated),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun VaultPage(
    state: OnboardingUiState,
    actions: OnboardingActions
) {
    PageHeader(Icons.Rounded.FolderOpen, stringResource(R.string.onboarding_vault_title), stringResource(R.string.onboarding_vault_text))
    if (state.vaultUri == null) {
        Button(onClick = actions.onPickVault, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Rounded.Folder, contentDescription = null)
            Spacer(Modifier.size(Spacing.xs))
            Text(stringResource(R.string.action_choose_folder))
        }
    } else {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TonalIcon(Icons.Rounded.Folder, size = 40.dp)
                    Text(state.vaultFolderName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions.onPickVault) { Text(stringResource(R.string.action_change)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Icon(
                        if (state.hasObsidianFolder) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (state.hasObsidianFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(
                            if (state.hasObsidianFolder) R.string.onboarding_vault_detected else R.string.onboarding_vault_not_detected
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = state.vaultName,
                    onValueChange = actions.onVaultNameChange,
                    label = { Text(stringResource(R.string.settings_vault_name)) },
                    supportingText = { Text(stringResource(R.string.onboarding_vault_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (state.vaultError) {
        Spacer(Modifier.height(Spacing.sm))
        Text(stringResource(R.string.onboarding_vault_error), color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(Spacing.md))
    InfoCard(stringResource(R.string.onboarding_vault_why))
}

@Composable
private fun NotificationsPage(
    state: OnboardingUiState,
    actions: OnboardingActions
) {
    PageHeader(
        Icons.Rounded.Notifications,
        stringResource(R.string.onboarding_notifications_title),
        stringResource(R.string.onboarding_notifications_text)
    )
    GrantedRow(state.notificationsGranted, stringResource(R.string.status_allowed), stringResource(R.string.status_not_allowed))
    Spacer(Modifier.height(Spacing.md))
    if (!state.notificationsGranted) {
        FilledTonalButton(onClick = actions.onRequestNotifications, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.action_allow_notifications))
        }
        Spacer(Modifier.height(Spacing.md))
    }
    InfoCard(stringResource(R.string.onboarding_notifications_why))
}

@Composable
private fun ExactAlarmsPage(
    state: OnboardingUiState,
    actions: OnboardingActions
) {
    PageHeader(Icons.Rounded.Alarm, stringResource(R.string.onboarding_exact_title), stringResource(R.string.onboarding_exact_text))
    GrantedRow(state.exactAlarmsGranted, stringResource(R.string.status_allowed), stringResource(R.string.status_not_allowed))
    Spacer(Modifier.height(Spacing.md))
    if (!state.exactAlarmsGranted) {
        FilledTonalButton(onClick = actions.onRequestExactAlarms, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.action_allow_exact))
        }
        Spacer(Modifier.height(Spacing.md))
        InfoCard(stringResource(R.string.onboarding_exact_fallback), icon = Icons.Rounded.Warning)
        Spacer(Modifier.height(Spacing.sm))
    }
    InfoCard(stringResource(R.string.onboarding_exact_why))
}

@Composable
private fun BackgroundPage(
    state: OnboardingUiState,
    actions: OnboardingActions
) {
    PageHeader(
        Icons.Rounded.BatteryChargingFull,
        stringResource(R.string.onboarding_background_title),
        stringResource(R.string.onboarding_background_text)
    )
    GrantedRow(state.backgroundAllowed, stringResource(R.string.status_allowed), stringResource(R.string.status_not_allowed))
    Spacer(Modifier.height(Spacing.md))
    if (!state.backgroundAllowed) {
        FilledTonalButton(onClick = actions.onRequestBackground, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.action_allow_background))
        }
        Spacer(Modifier.height(Spacing.md))
    }
    InfoCard(stringResource(R.string.onboarding_background_why))
}

@Composable
private fun DonePage(state: OnboardingUiState) {
    PageHeader(Icons.Rounded.Celebration, stringResource(R.string.onboarding_done_title), stringResource(R.string.onboarding_done_text))
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SummaryRow(true, stringResource(R.string.onboarding_summary_vault, state.vaultName))
            SummaryRow(state.notificationsGranted, stringResource(R.string.status_notifications))
            SummaryRow(state.exactAlarmsGranted, stringResource(R.string.status_exact_alarms))
            SummaryRow(state.backgroundAllowed, stringResource(R.string.status_battery))
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    Text(stringResource(R.string.onboarding_syntax_hint), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(Spacing.xs))
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            "- [ ] Zahnarzt anrufen (@2026-11-14 18:30)\n- [ ] Müll rausbringen (@2026-11-15)",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun SummaryRow(
    ok: Boolean,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusLight(
            if (ok) StatusLevel.OK else StatusLevel.WARNING,
            stringResource(if (ok) R.string.status_allowed else R.string.status_not_allowed)
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@ThemePreviews
@Composable
private fun OnboardingWelcomePreview() {
    VaultbellTheme(dynamicColor = false) { OnboardingScreen(OnboardingUiState(), OnboardingActions()) }
}

@ThemePreviews
@Composable
private fun OnboardingVaultPreview() {
    VaultbellTheme(dynamicColor = false) {
        OnboardingScreen(
            OnboardingUiState(
                step = OnboardingStep.VAULT,
                vaultUri = "content://x",
                vaultFolderName = "SebiMain",
                vaultName = "SebiMain",
                hasObsidianFolder = true
            ),
            OnboardingActions()
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingExactPreview() {
    VaultbellTheme(dynamicColor = false) {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.EXACT_ALARMS), OnboardingActions())
    }
}

@ThemePreviews
@Composable
private fun OnboardingDonePreview() {
    VaultbellTheme(dynamicColor = false) {
        OnboardingScreen(
            OnboardingUiState(step = OnboardingStep.DONE, vaultName = "SebiMain", notificationsGranted = true),
            OnboardingActions()
        )
    }
}
