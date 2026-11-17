package io.github.descomanian.vaultbell.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.alarm.NagStage
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.SettingsDivider
import io.github.descomanian.vaultbell.designsystem.component.SettingsGroup
import io.github.descomanian.vaultbell.designsystem.component.SettingsRow
import io.github.descomanian.vaultbell.designsystem.component.SettingsSwitchRow
import io.github.descomanian.vaultbell.designsystem.component.SettingsValueRow
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme
import io.github.descomanian.vaultbell.ui.common.ChoiceDialog
import io.github.descomanian.vaultbell.ui.common.formatMinutes

private enum class PlanDialog { ADD_OFFSET, STAGE, DAY_START, ACTIVE_FROM, ACTIVE_UNTIL }

/** A stage starting this far before the due time effectively starts at the start of the day. */
private const val FROM_DAY_START = 24 * 60

private val STAGE_START_OPTIONS = listOf(FROM_DAY_START, 6 * 60, 4 * 60, 3 * 60, 2 * 60, 60, 30)

/**
 * Reminder plan (lead times as chips), "Keep nudging" (editable stages), start of the day, active hours and
 * whether early reminders break through Do Not Disturb. Every change re-arms the alarms ([update]).
 */
@Composable
fun PlanSettingsGroup(
    s: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit
) {
    var dialog by rememberSaveable { mutableStateOf<PlanDialog?>(null) }
    // Index of the stage being edited, -1 = new stage.
    var editStage by rememberSaveable { mutableIntStateOf(-1) }

    SettingsGroup(stringResource(R.string.settings_group_plan)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_pre_reminder),
            description = stringResource(R.string.settings_pre_reminder_desc),
            icon = Icons.Rounded.Timer,
            checked = s.planEnabled,
            onCheckedChange = { v -> update { it.copy(planEnabled = v) } }
        )
        AnimatedVisibility(visible = s.planEnabled) {
            OffsetChips(
                offsets = s.planOffsetsMinutes,
                onRemove = { o -> update { it.copy(planOffsetsMinutes = it.planOffsetsMinutes - o) } },
                onAdd = { dialog = PlanDialog.ADD_OFFSET }
            )
        }
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_nag),
            description = stringResource(R.string.settings_nag_desc),
            icon = Icons.Rounded.AvTimer,
            checked = s.nagEnabled,
            onCheckedChange = { v -> update { it.copy(nagEnabled = v) } }
        )
        AnimatedVisibility(visible = s.nagEnabled) {
            Column {
                StageRows(
                    stages = s.nagStages,
                    onEdit = { i ->
                        editStage = i
                        dialog = PlanDialog.STAGE
                    },
                    onRemove = { i -> update { it.copy(nagStages = it.nagStages.filterIndexed { j, _ -> j != i }) } },
                    onAdd = {
                        editStage = -1
                        dialog = PlanDialog.STAGE
                    }
                )
                SettingsValueRow(
                    title = stringResource(R.string.settings_nag_day_start),
                    value = AppSettings.minutesToTime(s.nagDayStartMinutes ?: s.defaultTimeMinutes).toString(),
                    icon = Icons.Rounded.WbTwilight,
                    onClick = { dialog = PlanDialog.DAY_START }
                )
            }
        }
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_active_hours),
            description = stringResource(R.string.settings_active_hours_desc),
            icon = Icons.Rounded.Bedtime,
            checked = s.activeHoursEnabled,
            onCheckedChange = { v -> update { it.copy(activeHoursEnabled = v) } }
        )
        AnimatedVisibility(visible = s.activeHoursEnabled) {
            SettingsRow(
                title =
                stringResource(
                    R.string.settings_active_hours_value,
                    AppSettings.minutesToTime(s.activeFromMinutes).toString(),
                    AppSettings.minutesToTime(s.activeUntilMinutes).toString()
                ),
                trailing = {
                    TextButton(onClick = { dialog = PlanDialog.ACTIVE_FROM }) { Text(stringResource(R.string.settings_active_from)) }
                    TextButton(onClick = { dialog = PlanDialog.ACTIVE_UNTIL }) { Text(stringResource(R.string.settings_active_until)) }
                }
            )
        }
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_plan_break_through),
            description =
            stringResource(
                if (s.breakThroughSilence) R.string.settings_plan_break_through_desc else R.string.settings_plan_break_through_off
            ),
            icon = Icons.Rounded.NotificationsActive,
            checked = s.planBreakThrough && s.breakThroughSilence,
            enabled = s.breakThroughSilence,
            onCheckedChange = { v -> update { it.copy(planBreakThrough = v) } }
        )
    }

    PlanDialogs(dialog, editStage, s, update, onDismiss = { dialog = null })
}

@Composable
private fun OffsetChips(
    offsets: List<Int>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = 72.dp, end = Spacing.md, bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        offsets.sortedDescending().forEach { o ->
            val label = stringResource(R.string.plan_offset_before, formatMinutes(context, o))
            val remove = stringResource(R.string.plan_remove, label)
            InputChip(
                selected = false,
                onClick = { onRemove(o) },
                label = { Text(label) },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = remove, modifier = Modifier.size(18.dp)) }
            )
        }
        AssistChip(
            onClick = onAdd,
            label = { Text(stringResource(R.string.plan_add)) },
            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun StageRows(
    stages: List<NagStage>,
    onEdit: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.padding(start = 56.dp)) {
        // Shown in time order: earliest stage first, closest to the due time last.
        stages.withIndex().sortedByDescending { it.value.startMinutesBefore }.forEach { (i, stage) ->
            SettingsRow(
                title = stageStartLabel(stage.startMinutesBefore),
                description = stringResource(R.string.nag_stage_every, formatMinutes(context, stage.everyMinutes)),
                onClick = { onEdit(i) },
                trailing = {
                    val remove = stringResource(R.string.plan_remove, stageStartLabel(stage.startMinutesBefore))
                    IconButton(onClick = { onRemove(i) }) { Icon(Icons.Rounded.Delete, contentDescription = remove) }
                }
            )
        }
        TextButton(onClick = onAdd, modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Spacing.xs))
            Text(stringResource(R.string.nag_stage_add))
        }
    }
}

@Composable
private fun stageStartLabel(startMinutesBefore: Int): String =
    if (startMinutesBefore >= FROM_DAY_START) {
        stringResource(R.string.nag_stage_from_day_start)
    } else {
        stringResource(R.string.nag_stage_from, formatMinutes(LocalContext.current, startMinutesBefore))
    }

@Composable
private fun PlanDialogs(
    dialog: PlanDialog?,
    editStage: Int,
    s: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    when (dialog) {
        null -> Unit
        PlanDialog.ADD_OFFSET -> {
            val options = AppSettings.PLAN_OFFSET_OPTIONS.filter { it !in s.planOffsetsMinutes }
            ChoiceDialog(
                title = stringResource(R.string.plan_add),
                options = options,
                selected = -1,
                label = { stringResource(R.string.plan_offset_before, formatMinutes(context, it)) },
                onSelect = { o -> update { it.copy(planOffsetsMinutes = (it.planOffsetsMinutes + o).distinct()) } },
                onDismiss = onDismiss
            )
        }
        PlanDialog.STAGE ->
            StageDialog(
                initial = s.nagStages.getOrNull(editStage) ?: NagStage(2 * 60, 60),
                onConfirm = { stage ->
                    update {
                        val list = it.nagStages.toMutableList()
                        if (editStage in list.indices) list[editStage] = stage else list += stage
                        it.copy(nagStages = list.distinctBy { st -> st.startMinutesBefore })
                    }
                },
                onDismiss = onDismiss
            )
        PlanDialog.DAY_START ->
            TimeDialog(
                title = stringResource(R.string.settings_nag_day_start),
                initial = AppSettings.minutesToTime(s.nagDayStartMinutes ?: s.defaultTimeMinutes),
                onConfirm = { t -> update { it.copy(nagDayStartMinutes = t.hour * 60 + t.minute) } },
                onDismiss = onDismiss
            )
        PlanDialog.ACTIVE_FROM ->
            TimeDialog(
                title = stringResource(R.string.settings_active_from),
                initial = AppSettings.minutesToTime(s.activeFromMinutes),
                onConfirm = { t -> update { it.copy(activeFromMinutes = t.hour * 60 + t.minute) } },
                onDismiss = onDismiss
            )
        PlanDialog.ACTIVE_UNTIL ->
            TimeDialog(
                title = stringResource(R.string.settings_active_until),
                initial = AppSettings.minutesToTime(s.activeUntilMinutes),
                onConfirm = { t -> update { it.copy(activeUntilMinutes = t.hour * 60 + t.minute) } },
                onDismiss = onDismiss
            )
    }
}

/** "Starts …" and "Repeat every …" as two chip rows. */
@Composable
private fun StageDialog(
    initial: NagStage,
    onConfirm: (NagStage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var start by rememberSaveable { mutableIntStateOf(initial.startMinutesBefore.coerceAtMost(FROM_DAY_START)) }
    var every by rememberSaveable { mutableIntStateOf(initial.everyMinutes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nag_stage_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.nag_stage_start_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    (STAGE_START_OPTIONS + start).distinct().sortedDescending().forEach { o ->
                        FilterChip(selected = o == start, onClick = { start = o }, label = { Text(stageStartLabel(o)) })
                    }
                }
                Text(stringResource(R.string.nag_stage_every_label), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    (AppSettings.NAG_INTERVAL_OPTIONS + every).distinct().sorted().forEach { o ->
                        FilterChip(selected = o == every, onClick = { every = o }, label = { Text(formatMinutes(context, o)) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(NagStage(start, every))
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

// ---------------------------------------------------------------------------------------------
// Previews (neutral defaults, no personal data)
// ---------------------------------------------------------------------------------------------

@ThemePreviews
@Composable
private fun PlanSettingsPreview() {
    VaultbellTheme(dynamicColor = false) {
        Column {
            PlanSettingsGroup(AppSettings(planEnabled = true, nagEnabled = true), update = {})
        }
    }
}
