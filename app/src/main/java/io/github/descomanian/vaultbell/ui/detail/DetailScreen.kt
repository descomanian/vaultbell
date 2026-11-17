package io.github.descomanian.vaultbell.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.descomanian.vaultbell.AppContainer
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.EmptyState
import io.github.descomanian.vaultbell.designsystem.component.TonalIcon
import io.github.descomanian.vaultbell.designsystem.rememberHaptics
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme
import io.github.descomanian.vaultbell.system.ObsidianLinks
import io.github.descomanian.vaultbell.ui.common.formatDay
import io.github.descomanian.vaultbell.ui.common.formatMinutes
import io.github.descomanian.vaultbell.ui.common.formatRelative
import io.github.descomanian.vaultbell.ui.common.formatTime
import io.github.descomanian.vaultbell.ui.common.label
import io.github.descomanian.vaultbell.ui.common.sharedContainer
import io.github.descomanian.vaultbell.ui.preview.PreviewData
import java.time.LocalDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val reminder: Reminder? = null,
    val vaultName: String = "",
    val snoozeShortMinutes: Int = 10,
    val snoozeLongMinutes: Int = 60
)

class DetailViewModel(
    private val id: Long,
    private val container: AppContainer
) : ViewModel() {
    val state: StateFlow<DetailUiState> =
        combine(container.repository.observe(id), container.settings.settings) { r, s ->
            DetailUiState(false, r, s.vaultName, s.snoozeShortMinutes, s.snoozeLongMinutes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun done(after: () -> Unit) {
        viewModelScope.launch {
            container.repository.markDone(id)
            after()
        }
    }

    fun snooze(
        minutes: Int,
        after: () -> Unit
    ) {
        viewModelScope.launch {
            container.repository.snooze(id, minutes)
            after()
        }
    }
}

data class DetailActions(
    val onBack: () -> Unit = {},
    val onDone: () -> Unit = {},
    val onSnooze: (Int) -> Unit = {}
)

@Composable
fun DetailScreen(
    state: DetailUiState,
    actions: DetailActions,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    now: LocalDateTime = LocalDateTime.now()
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = actions.onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val reminder = state.reminder
        if (reminder == null) {
            if (!state.loading) {
                EmptyState(
                    icon = Icons.Rounded.Check,
                    title = stringResource(R.string.detail_gone_title),
                    message = stringResource(R.string.detail_gone_text),
                    modifier = Modifier.padding(padding)
                )
            }
        } else {
            DetailContent(state, actions, reminder, now, padding)
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    actions: DetailActions,
    reminder: Reminder,
    now: LocalDateTime,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    run {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            HeroCard(reminder, now)

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(vertical = Spacing.xs)) {
                    InfoRow(Icons.Rounded.Description, stringResource(R.string.detail_file), reminder.relativePath)
                    InfoRow(Icons.Rounded.Event, stringResource(R.string.detail_line), (reminder.lineNumber + 1).toString())
                    InfoRow(Icons.Rounded.Code, stringResource(R.string.detail_format), reminder.format.label())
                    reminder.recurrence?.let { rule ->
                        InfoRow(Icons.Rounded.Repeat, stringResource(R.string.detail_recurrence), rule.label())
                    }
                    if (reminder.isSnoozed) {
                        InfoRow(
                            Icons.Rounded.Snooze,
                            stringResource(R.string.detail_original_time),
                            "${reminder.originalDue.formatDay(context, now)}, ${reminder.originalDue.formatTime(context)}"
                        )
                    }
                }
            }

            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(
                    reminder.rawLine.trim(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(Spacing.md),
                    softWrap = false
                )
            }

            Button(
                onClick = { openInObsidian(context, state.vaultName, reminder.relativePath) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                Spacer(Modifier.size(Spacing.xs))
                Text(stringResource(R.string.action_open_obsidian))
            }
            FilledTonalButton(
                onClick = {
                    haptics.confirm()
                    actions.onDone()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.size(Spacing.xs))
                Text(stringResource(R.string.action_done))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(state.snoozeShortMinutes, state.snoozeLongMinutes).forEach { minutes ->
                    OutlinedButton(
                        onClick = {
                            haptics.tick()
                            actions.onSnooze(minutes)
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.xs))
                        Text("+" + formatMinutes(context, minutes))
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun HeroCard(
    reminder: Reminder,
    now: LocalDateTime
) {
    val context = LocalContext.current
    val overdue = reminder.due.isBefore(now)
    Card(
        modifier = Modifier.fillMaxWidth().sharedContainer("reminder-${reminder.id}"),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
        CardDefaults.cardColors(
            containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    "${reminder.due.formatDay(context, now)} · ${reminder.due.formatRelative(context, now)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(reminder.due.formatTime(context), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(Spacing.sm))
            Text(reminder.title, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        TonalIcon(
            icon,
            size = 36.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun openInObsidian(
    context: Context,
    vaultName: String,
    relativePath: String
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, ObsidianLinks.openNote(vaultName, relativePath)))
    } catch (e: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, R.string.error_obsidian_missing, android.widget.Toast.LENGTH_LONG).show()
    }
}

@ThemePreviews
@Composable
private fun DetailPreview() {
    VaultbellTheme(dynamicColor = false) {
        DetailScreen(
            state = DetailUiState(loading = false, reminder = PreviewData.reminders[3], vaultName = PreviewData.VAULT_NAME),
            actions = DetailActions(),
            now = PreviewData.now
        )
    }
}

@ThemePreviews
@Composable
private fun DetailOverduePreview() {
    VaultbellTheme(dynamicColor = false) {
        DetailScreen(
            state = DetailUiState(loading = false, reminder = PreviewData.reminders[1], vaultName = PreviewData.VAULT_NAME),
            actions = DetailActions(),
            now = PreviewData.now
        )
    }
}

@ThemePreviews
@Composable
private fun DetailSnoozedPreview() {
    VaultbellTheme(dynamicColor = false) {
        DetailScreen(
            state = DetailUiState(
                loading = false,
                reminder = PreviewData.reminders[4].copy(originalDue = PreviewData.now.withHour(18)),
                vaultName = PreviewData.VAULT_NAME
            ),
            actions = DetailActions(),
            now = PreviewData.now
        )
    }
}
