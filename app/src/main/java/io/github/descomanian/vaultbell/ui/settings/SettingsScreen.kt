package io.github.descomanian.vaultbell.ui.settings

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.InfoCard
import io.github.descomanian.vaultbell.designsystem.component.SettingsDivider
import io.github.descomanian.vaultbell.designsystem.component.SettingsGroup
import io.github.descomanian.vaultbell.designsystem.component.SettingsRow
import io.github.descomanian.vaultbell.designsystem.component.SettingsSliderRow
import io.github.descomanian.vaultbell.designsystem.component.SettingsSwitchRow
import io.github.descomanian.vaultbell.designsystem.component.SettingsValueRow
import io.github.descomanian.vaultbell.designsystem.component.StatusLevel
import io.github.descomanian.vaultbell.designsystem.component.StatusLight
import io.github.descomanian.vaultbell.designsystem.rememberHaptics
import io.github.descomanian.vaultbell.designsystem.theme.AccentColor
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.ThemeMode
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme
import io.github.descomanian.vaultbell.designsystem.theme.supportsDynamicColor
import io.github.descomanian.vaultbell.designsystem.theme.swatch
import io.github.descomanian.vaultbell.parser.ParserOptions
import io.github.descomanian.vaultbell.system.AppLanguage
import io.github.descomanian.vaultbell.system.SystemStatus
import io.github.descomanian.vaultbell.ui.common.ChoiceDialog
import io.github.descomanian.vaultbell.ui.common.formatMinutes
import io.github.descomanian.vaultbell.ui.common.inSentence
import io.github.descomanian.vaultbell.ui.preview.PreviewData
import java.time.LocalTime

data class SettingsActions(
    val onBack: () -> Unit = {},
    val update: ((AppSettings) -> AppSettings) -> Unit = {},
    val updateAndRescan: ((AppSettings) -> AppSettings) -> Unit = {},
    val updateAndReschedule: ((AppSettings) -> AppSettings) -> Unit = {},
    val onScanInterval: (Int) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onPickVault: () -> Unit = {},
    val onScanNow: () -> Unit = {},
    val onOpenNotificationSettings: () -> Unit = {},
    val onOpenExactAlarmSettings: () -> Unit = {},
    val onOpenBatterySettings: () -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
    val onOpenAbout: () -> Unit = {},
    /** Opens the "save as" dialog for the repeat log (Markdown file the user puts into the vault). */
    val onExportRepeatLog: () -> Unit = {}
)

private enum class SettingsDialog {
    THEME,
    LANGUAGE,
    DEFAULT_TIME,
    SNOOZE_SHORT,
    SNOOZE_LONG,
    VAULT_NAME,
    INCLUDE,
    EXCLUDE,
    WRITE_BACK,
    PROPERTY_NAMES
}

/** Settings are split into calm sub-pages: an overview list first, one topic per page. */
enum class SettingsSection {
    REMINDERS,
    PLAN,
    VAULT,
    DETECTION,
    DONE_AND_LOG,
    APPEARANCE,
    SYSTEM
}

/**
 * Overview with categories (title, icon, one line of current state); a tap opens the page of that category.
 * [twoPane] (large screens): overview on the left, the selected page on the right.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    twoPane: Boolean = false,
    initialSection: SettingsSection? = null
) {
    var section by rememberSaveable { mutableStateOf(initialSection ?: if (twoPane) SettingsSection.REMINDERS else null) }
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val onPage = section != null && !twoPane

    BackHandler(enabled = onPage) { section = null }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(section?.takeIf { onPage }?.let { sectionTitle(it) } ?: stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (onPage || showBack) {
                        IconButton(onClick = {
                            if (onPage) {
                                section = null
                            } else {
                                actions.onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        val openDialog: (SettingsDialog) -> Unit = { dialog = it }
        if (twoPane) {
            Row(Modifier.fillMaxSize().padding(padding)) {
                SettingsOverview(state, selected = section, onOpen = { section = it }, modifier = Modifier.weight(0.4f))
                VerticalDivider()
                SectionPage(section ?: SettingsSection.REMINDERS, state, actions, openDialog, Modifier.weight(0.6f), showTitle = true)
            }
        } else {
            Crossfade(targetState = section, label = "settings-page", modifier = Modifier.fillMaxSize().padding(padding)) { current ->
                if (current == null) {
                    SettingsOverview(state, selected = null, onOpen = { section = it })
                } else {
                    SectionPage(current, state, actions, openDialog)
                }
            }
        }
    }

    SettingsDialogs(dialog, state, actions, onDismiss = { dialog = null })
}

// ---------------------------------------------------------------------------------------------
// Overview
// ---------------------------------------------------------------------------------------------

@Composable
private fun SettingsOverview(
    state: SettingsUiState,
    selected: SettingsSection?,
    onOpen: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups =
        listOf(
            R.string.settings_overview_remind to listOf(SettingsSection.REMINDERS, SettingsSection.PLAN),
            R.string.settings_overview_notes to
                listOf(SettingsSection.VAULT, SettingsSection.DETECTION, SettingsSection.DONE_AND_LOG),
            R.string.settings_overview_app to listOf(SettingsSection.APPEARANCE, SettingsSection.SYSTEM)
        )
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.lg)) {
        groups.forEach { (titleRes, sections) ->
            item(key = titleRes) {
                SettingsGroup(stringResource(titleRes)) {
                    sections.forEachIndexed { index, s ->
                        if (index > 0) SettingsDivider()
                        val isSelected = s == selected
                        SettingsRow(
                            title = sectionTitle(s),
                            description = sectionSummary(s, state),
                            icon = sectionIcon(s),
                            onClick = { onOpen(s) },
                            modifier =
                            if (isSelected) {
                                Modifier.background(MaterialTheme.colorScheme.secondaryContainer).semantics { this.selected = true }
                            } else {
                                Modifier
                            },
                            trailing = {
                                if (s == SettingsSection.SYSTEM && !state.status.allOk) {
                                    StatusLight(StatusLevel.WARNING, stringResource(R.string.summary_system_attention))
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
        item(key = "insets") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun sectionTitle(section: SettingsSection): String =
    stringResource(
        when (section) {
            SettingsSection.REMINDERS -> R.string.settings_group_reminders
            SettingsSection.PLAN -> R.string.settings_group_plan
            SettingsSection.VAULT -> R.string.settings_group_vault
            SettingsSection.DETECTION -> R.string.settings_group_detection
            SettingsSection.DONE_AND_LOG -> R.string.settings_group_writeback
            SettingsSection.APPEARANCE -> R.string.settings_group_appearance
            SettingsSection.SYSTEM -> R.string.settings_group_system
        }
    )

private fun sectionIcon(section: SettingsSection): ImageVector =
    when (section) {
        SettingsSection.REMINDERS -> Icons.Rounded.Notifications
        SettingsSection.PLAN -> Icons.Rounded.Timer
        SettingsSection.VAULT -> Icons.Rounded.Folder
        SettingsSection.DETECTION -> Icons.Rounded.Code
        SettingsSection.DONE_AND_LOG -> Icons.Rounded.TaskAlt
        SettingsSection.APPEARANCE -> Icons.Rounded.Palette
        SettingsSection.SYSTEM -> Icons.Rounded.Info
    }

private val SystemStatus.allOk: Boolean get() = notificationsAllowed && exactAlarmsAllowed && batteryUnrestricted

/** One short line of current state per category, so the overview answers "how is it set?" without opening it. */
@Composable
private fun sectionSummary(
    section: SettingsSection,
    state: SettingsUiState
): String {
    val s = state.settings
    val context = LocalContext.current
    return when (section) {
        SettingsSection.REMINDERS ->
            listOf(
                stringResource(R.string.summary_default_time, s.defaultTime.toString()),
                stringResource(
                    R.string.summary_snooze,
                    formatMinutes(context, s.snoozeShortMinutes),
                    formatMinutes(context, s.snoozeLongMinutes)
                )
            ).joinToString(SEPARATOR)
        SettingsSection.PLAN ->
            listOf(
                onOff(stringResource(R.string.settings_pre_reminder), s.planEnabled),
                onOff(stringResource(R.string.settings_nag), s.nagEnabled)
            ).joinToString(SEPARATOR)
        SettingsSection.VAULT ->
            listOf(
                state.vaultFolderName?.takeIf { s.vaultUri != null } ?: stringResource(R.string.settings_vault_none),
                stringResource(R.string.summary_scan_every, formatMinutes(context, s.scanIntervalMinutes))
            ).joinToString(SEPARATOR)
        SettingsSection.DETECTION ->
            buildList {
                if (s.formatReminderPlugin) add(stringResource(R.string.format_reminder_plugin))
                if (s.formatDataview) add(stringResource(R.string.format_dataview))
                if (s.formatTasks) add(stringResource(R.string.format_tasks))
                if (s.formatProperties) add(stringResource(R.string.format_property))
            }.joinToString(", ").ifEmpty { stringResource(R.string.summary_no_format) }
        SettingsSection.DONE_AND_LOG ->
            listOf(
                onOff(stringResource(R.string.settings_write_done), s.writeDoneToFile),
                repeatLogState(state.repeatLogPending)
            ).joinToString(SEPARATOR)
        SettingsSection.APPEARANCE ->
            listOf(themeLabel(s.themeMode), languageLabel(state.language)).joinToString(SEPARATOR)
        SettingsSection.SYSTEM ->
            if (state.status.allOk) {
                stringResource(R.string.summary_system_ok)
            } else {
                val missing =
                    buildList {
                        if (!state.status.notificationsAllowed) add(stringResource(R.string.status_notifications))
                        if (!state.status.exactAlarmsAllowed) add(stringResource(R.string.status_exact_alarms))
                        if (!state.status.batteryUnrestricted) add(stringResource(R.string.status_battery))
                    }
                stringResource(R.string.summary_system_check, missing.joinToString(", "))
            }
    }
}

@Composable
private fun onOff(
    label: String,
    on: Boolean
): String = stringResource(if (on) R.string.summary_on else R.string.summary_off, label)

@Composable
private fun repeatLogState(pending: Int): String =
    if (pending == 0) {
        stringResource(R.string.settings_repeat_log_none)
    } else {
        pluralStringResource(R.plurals.repeat_log_pending, pending, pending)
    }

private const val SEPARATOR = " · "

// ---------------------------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionPage(
    section: SettingsSection,
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.lg)) {
        if (showTitle) {
            item(key = "title") {
                Text(
                    sectionTitle(section),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = Spacing.xl, top = Spacing.md).semantics { heading() }
                )
            }
        }
        item(key = section.name) {
            Column {
                when (section) {
                    SettingsSection.REMINDERS -> RemindersPage(state, actions, openDialog)
                    SettingsSection.PLAN -> PlanSettingsGroup(state.settings, actions.updateAndReschedule, title = null)
                    SettingsSection.VAULT -> VaultPage(state, actions, openDialog)
                    SettingsSection.DETECTION -> DetectionPage(state, actions, openDialog)
                    SettingsSection.DONE_AND_LOG -> DoneAndLogPage(state, actions, openDialog)
                    SettingsSection.APPEARANCE -> AppearancePage(state, actions, openDialog)
                    SettingsSection.SYSTEM -> SystemPage(state, actions)
                }
            }
        }
        item(key = "insets") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun AppearancePage(
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit
) {
    val s = state.settings
    SettingsGroup(title = null) {
        SettingsValueRow(
            title = stringResource(R.string.settings_theme),
            value = themeLabel(s.themeMode),
            icon = Icons.Rounded.BrightnessMedium,
            onClick = { openDialog(SettingsDialog.THEME) }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_dynamic_color),
            description =
            stringResource(
                if (supportsDynamicColor) R.string.settings_dynamic_color_desc else R.string.settings_dynamic_color_unsupported
            ),
            icon = Icons.Rounded.Wallpaper,
            checked = s.dynamicColor && supportsDynamicColor,
            enabled = supportsDynamicColor,
            onCheckedChange = { v -> actions.update { it.copy(dynamicColor = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_amoled),
            description = stringResource(R.string.settings_amoled_desc),
            icon = Icons.Rounded.DarkMode,
            checked = s.amoledBlack,
            onCheckedChange = { v -> actions.update { it.copy(amoledBlack = v) } }
        )
        SettingsDivider()
        AccentRow(
            selected = s.accent,
            enabled = !(s.dynamicColor && supportsDynamicColor),
            onSelect = { a -> actions.update { it.copy(accent = a) } }
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_language),
            value = languageLabel(state.language),
            icon = Icons.Rounded.Language,
            onClick = { openDialog(SettingsDialog.LANGUAGE) }
        )
    }
}

@Composable
private fun RemindersPage(
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit
) {
    val s = state.settings
    val context = LocalContext.current
    SettingsGroup(stringResource(R.string.settings_sub_times)) {
        SettingsValueRow(
            title = stringResource(R.string.settings_default_time),
            value = stringResource(R.string.settings_default_time_value, s.defaultTime.toString()),
            icon = Icons.Rounded.AccessTime,
            onClick = { openDialog(SettingsDialog.DEFAULT_TIME) }
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_snooze_short),
            value = formatMinutes(context, s.snoozeShortMinutes),
            icon = Icons.Rounded.Snooze,
            onClick = { openDialog(SettingsDialog.SNOOZE_SHORT) }
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_snooze_long),
            value = formatMinutes(context, s.snoozeLongMinutes),
            icon = Icons.Rounded.Snooze,
            onClick = { openDialog(SettingsDialog.SNOOZE_LONG) }
        )
    }
    SettingsGroup(stringResource(R.string.settings_sub_notification)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_repeat_ignored),
            description = stringResource(R.string.settings_repeat_ignored_desc),
            icon = Icons.Rounded.Repeat,
            checked = s.repeatIfIgnored,
            onCheckedChange = { v -> actions.update { it.copy(repeatIfIgnored = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_sound),
            description = stringResource(R.string.settings_sound_desc),
            icon = Icons.Rounded.Vibration,
            checked = s.soundAndVibration,
            onCheckedChange = { v -> actions.update { it.copy(soundAndVibration = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_break_silence),
            description = stringResource(R.string.settings_break_silence_desc),
            icon = Icons.Rounded.NotificationsActive,
            checked = s.breakThroughSilence,
            onCheckedChange = { v -> actions.update { it.copy(breakThroughSilence = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_lock_screen_hide),
            description = stringResource(R.string.settings_lock_screen_hide_desc),
            icon = Icons.Rounded.Lock,
            checked = s.hideOnLockScreen,
            onCheckedChange = { v -> actions.update { it.copy(hideOnLockScreen = v) } }
        )
    }
    SettingsGroup(stringResource(R.string.settings_sub_list)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_overdue),
            description = stringResource(R.string.settings_show_overdue_desc),
            icon = Icons.Rounded.Visibility,
            checked = s.showOverdueOnStart,
            onCheckedChange = { v -> actions.update { it.copy(showOverdueOnStart = v) } }
        )
    }
}

@Composable
private fun VaultPage(
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit
) {
    val s = state.settings
    val context = LocalContext.current
    SettingsGroup(stringResource(R.string.settings_sub_folders)) {
        SettingsValueRow(
            title = stringResource(R.string.settings_vault_folder),
            value = state.vaultFolderName?.takeIf { s.vaultUri != null } ?: stringResource(R.string.settings_vault_none),
            icon = Icons.Rounded.Folder,
            onClick = actions.onPickVault
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_vault_name),
            value = s.vaultName.ifBlank { "–" },
            icon = Icons.Rounded.Badge,
            onClick = { openDialog(SettingsDialog.VAULT_NAME) }
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_include),
            value = s.includeFolders.joinToString(", ").ifEmpty { stringResource(R.string.settings_include_all) },
            icon = Icons.Rounded.Folder,
            onClick = { openDialog(SettingsDialog.INCLUDE) }
        )
        SettingsDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_exclude),
            value = s.excludeFolders.joinToString(", ").ifEmpty { stringResource(R.string.settings_exclude_none) },
            icon = Icons.Rounded.Block,
            onClick = { openDialog(SettingsDialog.EXCLUDE) }
        )
    }
    SettingsGroup(stringResource(R.string.settings_sub_scan)) {
        val intervals = AppSettings.SCAN_INTERVALS
        var intervalIndex by remember(s.scanIntervalMinutes) {
            mutableFloatStateOf(intervals.indexOf(s.scanIntervalMinutes).coerceAtLeast(0).toFloat())
        }
        SettingsSliderRow(
            title = stringResource(R.string.settings_scan_interval),
            description = stringResource(R.string.settings_scan_interval_desc),
            icon = Icons.Rounded.Sync,
            value = intervalIndex,
            valueLabel = formatMinutes(context, intervals[intervalIndex.toInt()]),
            onValueChange = { intervalIndex = it },
            onValueChangeFinished = { actions.onScanInterval(intervals[intervalIndex.toInt()]) },
            valueRange = 0f..(intervals.size - 1).toFloat(),
            steps = intervals.size - 2
        )
        SettingsDivider()
        ScanNowRow(state, actions.onScanNow)
        InfoCard(
            text = stringResource(R.string.settings_sync_hint),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun DetectionPage(
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit
) {
    val s = state.settings
    SettingsGroup(title = null) {
        SettingsSwitchRow(
            title = stringResource(R.string.format_reminder_plugin),
            description = stringResource(R.string.settings_format_example, "- [ ] Text (@2026-11-14 18:30)"),
            icon = Icons.Rounded.Code,
            checked = s.formatReminderPlugin,
            onCheckedChange = { v -> actions.updateAndRescan { it.copy(formatReminderPlugin = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.format_dataview),
            description = stringResource(R.string.settings_format_example, "- [ ] Text [due:: 2026-11-14 18:30]"),
            icon = Icons.Rounded.Code,
            checked = s.formatDataview,
            onCheckedChange = { v -> actions.updateAndRescan { it.copy(formatDataview = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.format_tasks),
            description = stringResource(R.string.settings_format_example, "- [ ] Text ⏰ 2026-11-14 18:30 · 📅 2026-11-14"),
            icon = Icons.Rounded.Code,
            checked = s.formatTasks,
            onCheckedChange = { v -> actions.updateAndRescan { it.copy(formatTasks = v) } }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.format_property),
            description = stringResource(R.string.settings_format_example, "reminder: 2026-11-14T18:30"),
            icon = Icons.Rounded.Code,
            checked = s.formatProperties,
            onCheckedChange = { v -> actions.updateAndRescan { it.copy(formatProperties = v) } }
        )
        if (s.formatProperties) {
            SettingsValueRow(
                title = stringResource(R.string.settings_property_names),
                value = s.propertyNames.joinToString(", "),
                icon = Icons.Rounded.Badge,
                onClick = { openDialog(SettingsDialog.PROPERTY_NAMES) }
            )
        }
    }
}

@Composable
private fun DoneAndLogPage(
    state: SettingsUiState,
    actions: SettingsActions,
    openDialog: (SettingsDialog) -> Unit
) {
    val s = state.settings
    SettingsGroup(stringResource(R.string.settings_sub_one_time)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_write_done),
            description = stringResource(R.string.settings_write_done_desc),
            icon = Icons.Rounded.Edit,
            checked = s.writeDoneToFile,
            onCheckedChange = { v ->
                if (v) openDialog(SettingsDialog.WRITE_BACK) else actions.update { it.copy(writeDoneToFile = false) }
            }
        )
    }
    SettingsGroup(stringResource(R.string.settings_sub_repeating)) {
        SettingsRow(
            title = stringResource(R.string.settings_repeat_log),
            description = repeatLogState(state.repeatLogPending),
            icon = Icons.Rounded.EventRepeat,
            trailing = {
                FilledTonalButton(onClick = actions.onExportRepeatLog, enabled = state.repeatLogPending > 0) {
                    Text(stringResource(R.string.action_export))
                }
            }
        )
        InfoCard(
            text = stringResource(R.string.settings_repeat_log_info),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun SystemPage(
    state: SettingsUiState,
    actions: SettingsActions
) {
    SettingsGroup(stringResource(R.string.settings_sub_status)) {
        StatusRow(
            icon = Icons.Rounded.Notifications,
            title = stringResource(R.string.status_notifications),
            ok = state.status.notificationsAllowed,
            missingLevel = StatusLevel.ERROR,
            onFix = actions.onOpenNotificationSettings
        )
        SettingsDivider()
        StatusRow(
            icon = Icons.Rounded.Alarm,
            title = stringResource(R.string.status_exact_alarms),
            ok = state.status.exactAlarmsAllowed,
            missingLevel = StatusLevel.WARNING,
            onFix = actions.onOpenExactAlarmSettings
        )
        SettingsDivider()
        StatusRow(
            icon = Icons.Rounded.BatteryAlert,
            title = stringResource(R.string.status_battery),
            ok = state.status.batteryUnrestricted,
            missingLevel = StatusLevel.WARNING,
            okText = stringResource(R.string.status_battery_ok),
            missingText = stringResource(R.string.status_battery_missing),
            onFix = actions.onOpenBatterySettings
        )
    }
    SettingsGroup(stringResource(R.string.settings_sub_info)) {
        SettingsRow(
            title = stringResource(R.string.privacy_title),
            description = stringResource(R.string.privacy_short),
            icon = Icons.Rounded.PrivacyTip,
            onClick = actions.onOpenPrivacy,
            trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) }
        )
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.about_title),
            description = stringResource(R.string.about_short),
            icon = Icons.Rounded.Info,
            onClick = actions.onOpenAbout,
            trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) }
        )
    }
}

@Composable
private fun SettingsDialogs(
    dialog: SettingsDialog?,
    state: SettingsUiState,
    actions: SettingsActions,
    onDismiss: () -> Unit
) {
    val s = state.settings
    val context = LocalContext.current
    when (dialog) {
        null -> Unit
        SettingsDialog.THEME ->
            ChoiceDialog(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = s.themeMode,
                label = { themeLabel(it) },
                onSelect = { m -> actions.update { it.copy(themeMode = m) } },
                onDismiss = onDismiss
            )
        SettingsDialog.LANGUAGE ->
            ChoiceDialog(
                title = stringResource(R.string.settings_language),
                options = AppLanguage.entries,
                selected = state.language,
                label = { languageLabel(it) },
                onSelect = actions.onLanguage,
                onDismiss = onDismiss
            )
        SettingsDialog.SNOOZE_SHORT ->
            ChoiceDialog(
                title = stringResource(R.string.settings_snooze_short),
                options = AppSettings.SNOOZE_SHORT_OPTIONS,
                selected = s.snoozeShortMinutes,
                label = { formatMinutes(context, it) },
                onSelect = { m -> actions.update { it.copy(snoozeShortMinutes = m) } },
                onDismiss = onDismiss
            )
        SettingsDialog.SNOOZE_LONG ->
            ChoiceDialog(
                title = stringResource(R.string.settings_snooze_long),
                options = AppSettings.SNOOZE_LONG_OPTIONS,
                selected = s.snoozeLongMinutes,
                label = { formatMinutes(context, it) },
                onSelect = { m -> actions.update { it.copy(snoozeLongMinutes = m) } },
                onDismiss = onDismiss
            )
        SettingsDialog.DEFAULT_TIME ->
            TimeDialog(
                initial = s.defaultTime,
                onConfirm = { t -> actions.updateAndRescan { it.copy(defaultTimeMinutes = t.hour * 60 + t.minute) } },
                onDismiss = onDismiss
            )
        SettingsDialog.VAULT_NAME ->
            TextDialog(
                title = stringResource(R.string.settings_vault_name),
                hint = stringResource(R.string.onboarding_vault_name_hint),
                initial = s.vaultName,
                multiline = false,
                onConfirm = { v -> actions.update { it.copy(vaultName = v.trim()) } },
                onDismiss = onDismiss
            )
        SettingsDialog.INCLUDE ->
            TextDialog(
                title = stringResource(R.string.settings_include),
                hint = stringResource(R.string.settings_folders_hint),
                initial = s.includeFolders.joinToString("\n"),
                multiline = true,
                onConfirm = { v -> actions.updateAndRescan { it.copy(includeFolders = v.toFolders()) } },
                onDismiss = onDismiss
            )
        SettingsDialog.EXCLUDE ->
            TextDialog(
                title = stringResource(R.string.settings_exclude),
                hint = stringResource(R.string.settings_folders_hint),
                initial = s.excludeFolders.joinToString("\n"),
                multiline = true,
                onConfirm = { v -> actions.updateAndRescan { it.copy(excludeFolders = v.toFolders()) } },
                onDismiss = onDismiss
            )
        SettingsDialog.PROPERTY_NAMES ->
            TextDialog(
                title = stringResource(R.string.settings_property_names),
                hint = stringResource(R.string.settings_property_names_hint),
                initial = s.propertyNames.joinToString("\n"),
                multiline = true,
                onConfirm = { v ->
                    val names = v.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    actions.updateAndRescan { it.copy(propertyNames = names.ifEmpty { ParserOptions.DEFAULT_PROPERTY_NAMES }) }
                },
                onDismiss = onDismiss
            )
        SettingsDialog.WRITE_BACK ->
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                title = { Text(stringResource(R.string.writeback_dialog_title)) },
                text = { Text(stringResource(R.string.writeback_dialog_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        actions.update { it.copy(writeDoneToFile = true) }
                        onDismiss()
                    }) { Text(stringResource(R.string.action_enable)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
            )
    }
}

private fun String.toFolders(): List<String> = split('\n', ',').map { it.trim().trim('/') }.filter { it.isNotEmpty() }

@Composable
private fun themeLabel(mode: ThemeMode): String =
    stringResource(
        when (mode) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        }
    )

@Composable
private fun languageLabel(language: AppLanguage): String =
    stringResource(
        when (language) {
            AppLanguage.SYSTEM -> R.string.language_system
            AppLanguage.GERMAN -> R.string.language_german
            AppLanguage.ENGLISH -> R.string.language_english
        }
    )

@Composable
private fun accentLabel(accent: AccentColor): String =
    stringResource(
        when (accent) {
            AccentColor.SAGE -> R.string.accent_sage
            AccentColor.OCEAN -> R.string.accent_ocean
            AccentColor.AMBER -> R.string.accent_amber
            AccentColor.ROSE -> R.string.accent_rose
            AccentColor.VIOLET -> R.string.accent_violet
        }
    )

@Composable
private fun AccentRow(
    selected: AccentColor,
    enabled: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val haptics = rememberHaptics()
    SettingsRow(
        title = stringResource(R.string.settings_accent),
        description = stringResource(if (enabled) R.string.settings_accent_desc else R.string.settings_accent_disabled),
        icon = Icons.Rounded.Palette,
        enabled = enabled
    )
    LazyRow(
        contentPadding = PaddingValues(start = 64.dp, end = Spacing.md, bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        items(AccentColor.entries) { accent ->
            val isSelected = accent == selected
            val label = accentLabel(accent)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.tick()
                            onSelect(accent)
                        }
                    ).semantics { contentDescription = label },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.swatch.copy(alpha = if (enabled) 1f else 0.35f))
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun ScanNowRow(
    state: SettingsUiState,
    onScanNow: () -> Unit
) {
    val s = state.settings
    val status =
        when {
            state.scanning -> stringResource(R.string.scan_running)
            s.lastScanError != null -> stringResource(R.string.scan_error)
            s.lastScanAt == 0L -> stringResource(R.string.scan_never)
            else ->
                stringResource(
                    R.string.scan_status,
                    DateUtils.getRelativeTimeSpanString(s.lastScanAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                        .inSentence(LocalContext.current),
                    pluralStringResource(R.plurals.reminders_count, state.activeCount, state.activeCount)
                )
        }
    SettingsRow(
        title = stringResource(R.string.action_scan_now),
        description = status,
        icon = Icons.Rounded.NotificationsActive,
        trailing = {
            if (state.scanning) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                FilledTonalButton(onClick = onScanNow, enabled = s.vaultUri != null) { Text(stringResource(R.string.action_scan)) }
            }
        }
    )
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    missingLevel: StatusLevel,
    onFix: () -> Unit,
    okText: String = stringResource(R.string.status_allowed),
    missingText: String = stringResource(R.string.status_not_allowed)
) {
    val label = if (ok) okText else missingText
    SettingsRow(
        title = title,
        description = label,
        icon = icon,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                StatusLight(if (ok) StatusLevel.OK else missingLevel, label)
                TextButton(onClick = onFix) { Text(stringResource(if (ok) R.string.action_open else R.string.action_fix)) }
            }
        }
    )
}

@Composable
internal fun TimeDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.settings_default_time)
) {
    val context = LocalContext.current
    val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = picker) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(picker.hour, picker.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
internal fun TextDialog(
    title: String,
    hint: String,
    initial: String,
    multiline: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                InfoCard(hint)
                Spacer(Modifier.size(Spacing.sm))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = !multiline,
                    minLines = if (multiline) 3 else 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(value)
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

private val previewState =
    SettingsUiState(
        loaded = true,
        settings =
        AppSettings(
            onboardingDone = true,
            vaultUri = "content://preview",
            vaultName = PreviewData.VAULT_NAME,
            excludeFolders = listOf("Archiv", "Templates"),
            planEnabled = true,
            lastScanAt = System.currentTimeMillis() - 3 * 60_000,
            dynamicColor = false
        ),
        status = SystemStatus(notificationsAllowed = true, exactAlarmsAllowed = false, batteryUnrestricted = false),
        activeCount = 12,
        vaultFolderName = PreviewData.VAULT_NAME,
        repeatLogPending = 3
    )

@ThemePreviews
@Composable
private fun SettingsPreview() {
    VaultbellTheme(dynamicColor = false) { SettingsScreen(previewState, SettingsActions()) }
}

@ThemePreviews
@Composable
private fun SettingsDoneAndLogPreview() {
    VaultbellTheme(dynamicColor = false) { SettingsScreen(previewState, SettingsActions(), initialSection = SettingsSection.DONE_AND_LOG) }
}
