package io.github.descomanian.vaultbell.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.data.ReminderGroup
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.EmptyState
import io.github.descomanian.vaultbell.designsystem.component.InfoCard
import io.github.descomanian.vaultbell.designsystem.component.SectionHeader
import io.github.descomanian.vaultbell.designsystem.rememberHaptics
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme
import io.github.descomanian.vaultbell.ui.common.formatDay
import io.github.descomanian.vaultbell.ui.common.formatTime
import io.github.descomanian.vaultbell.ui.common.label
import io.github.descomanian.vaultbell.ui.common.sharedContainer
import io.github.descomanian.vaultbell.ui.preview.PreviewData
import java.time.LocalDateTime

data class HomeActions(
    val onQueryChange: (String) -> Unit = {},
    val onFilterChange: (HomeFilter) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpen: (Long) -> Unit = {},
    val onDone: (Long) -> Unit = {},
    val onSnooze: (Long) -> Unit = {},
    val onToggleOverdue: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onFixExactAlarms: () -> Unit = {},
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showSettingsAction: Boolean = true,
    selectedId: Long? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (showSettingsAction) {
                        IconButton(onClick = actions.onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.scanning,
            onRefresh = actions.onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ReminderList(state, actions, selectedId)
        }
    }
}

@Composable
private fun ReminderList(
    state: HomeUiState,
    actions: HomeActions,
    selectedId: Long?,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
    ) {
        item(key = "summary") {
            val summary =
                if (state.lastScanAt > 0) {
                    stringResource(
                        R.string.home_summary,
                        state.totalCount,
                        android.text.format.DateUtils.getRelativeTimeSpanString(
                            state.lastScanAt,
                            System.currentTimeMillis(),
                            android.text.format.DateUtils.MINUTE_IN_MILLIS,
                        ),
                    )
                } else {
                    stringResource(R.string.home_summary_never, state.totalCount)
                }
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm),
            )
        }
        item(key = "search") { SearchField(state.query, actions.onQueryChange) }
        item(key = "filters") { FilterRow(state.filter, actions.onFilterChange) }

        if (!state.exactAlarmsAllowed) {
            item(key = "exact-warning") {
                Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                    InfoCard(
                        text = stringResource(R.string.home_exact_warning),
                        icon = Icons.Rounded.Warning,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    )
                    TextButton(onClick = actions.onFixExactAlarms, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.action_allow))
                    }
                }
            }
        }

        if (!state.loading && state.isEmpty) {
            item(key = "empty") {
                when {
                    !state.hasVault ->
                        EmptyState(
                            icon = Icons.Rounded.FolderOpen,
                            title = stringResource(R.string.empty_no_vault_title),
                            message = stringResource(R.string.empty_no_vault_text),
                            action = { Button(onClick = actions.onOpenSettings) { Text(stringResource(R.string.action_choose_vault)) } },
                        )
                    state.query.isNotBlank() || state.filter != HomeFilter.ALL ->
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            title = stringResource(R.string.empty_no_match_title),
                            message = stringResource(R.string.empty_no_match_text),
                        )
                    else ->
                        EmptyState(
                            icon = Icons.Rounded.NotificationsNone,
                            title = stringResource(R.string.empty_title),
                            message = stringResource(R.string.empty_text),
                            action = {
                                TextButton(onClick = actions.onRefresh) { Text(stringResource(R.string.action_scan_now)) }
                            },
                        )
                }
            }
        }

        state.sections.forEach { section ->
            stickyHeader(key = "header-${section.group}", contentType = "header") {
                SectionHeader(
                    title = section.group.label(),
                    count = section.items.size,
                    accent = section.group.accent(),
                    onClick = if (section.group == ReminderGroup.OVERDUE) actions.onToggleOverdue else null,
                    trailing =
                        if (section.group == ReminderGroup.OVERDUE) {
                            {
                                Icon(
                                    if (section.collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                                    contentDescription =
                                        stringResource(if (section.collapsed) R.string.action_expand else R.string.action_collapse),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
            if (!section.collapsed) {
                items(section.items, key = { it.id }, contentType = { "reminder" }) { reminder ->
                    SwipeableReminder(
                        reminder = reminder,
                        group = section.group,
                        now = state.now,
                        selected = reminder.id == selectedId,
                        actions = actions,
                        dayLabel = reminder.due.formatDay(context, state.now),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        item(key = "insets") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun ReminderGroup.accent(): Color =
    when (this) {
        ReminderGroup.OVERDUE -> MaterialTheme.colorScheme.error
        ReminderGroup.TODAY -> MaterialTheme.colorScheme.primary
        ReminderGroup.TOMORROW -> MaterialTheme.colorScheme.tertiary
        ReminderGroup.UPCOMING -> MaterialTheme.colorScheme.outline
    }

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear_search))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun FilterRow(
    selected: HomeFilter,
    onChange: (HomeFilter) -> Unit,
) {
    val haptics = rememberHaptics()
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(HomeFilter.entries) { f ->
            FilterChip(
                selected = f == selected,
                onClick = {
                    haptics.tick()
                    onChange(f)
                },
                label = {
                    Text(
                        stringResource(
                            when (f) {
                                HomeFilter.ALL -> R.string.filter_all
                                HomeFilter.TODAY -> R.string.filter_today
                                HomeFilter.OVERDUE -> R.string.filter_overdue
                                HomeFilter.SNOOZED -> R.string.filter_snoozed
                            },
                        ),
                    )
                },
                leadingIcon =
                    if (f == selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun SwipeableReminder(
    reminder: Reminder,
    group: ReminderGroup,
    now: LocalDateTime,
    selected: Boolean,
    actions: HomeActions,
    dayLabel: String,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val doneLabel = stringResource(R.string.action_done)
    val snoozeLabel = stringResource(R.string.action_snooze)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        haptics.confirm()
                        actions.onDone(reminder.id)
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        haptics.confirm()
                        actions.onSnooze(reminder.id)
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> true
                }
            },
        )
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) haptics.threshold()
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.xxs),
        backgroundContent = { SwipeBackground(dismissState.dismissDirection, dismissState.targetValue) },
    ) {
        ReminderCard(
            reminder = reminder,
            group = group,
            now = now,
            selected = selected,
            dayLabel = dayLabel,
            onClick = { actions.onOpen(reminder.id) },
            modifier =
                Modifier.semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(doneLabel) {
                                actions.onDone(reminder.id)
                                true
                            },
                            CustomAccessibilityAction(snoozeLabel) {
                                actions.onSnooze(reminder.id)
                                true
                            },
                        )
                },
        )
    }
}

@Composable
private fun SwipeBackground(
    direction: SwipeToDismissBoxValue,
    target: SwipeToDismissBoxValue,
) {
    val done = direction == SwipeToDismissBoxValue.StartToEnd
    val armed = target != SwipeToDismissBoxValue.Settled
    val container by animateColorAsState(
        when {
            direction == SwipeToDismissBoxValue.Settled -> Color.Transparent
            done -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer
        },
        label = "swipeBg",
    )
    val iconScale by animateFloatAsState(if (armed) 1.15f else 0.85f, label = "swipeIcon")
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .padding(horizontal = Spacing.lg),
        contentAlignment = if (done) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(
                    if (done) Icons.Rounded.Check else Icons.Rounded.Snooze,
                    contentDescription = null,
                    tint = if (done) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.scale(iconScale),
                )
                Text(
                    stringResource(if (done) R.string.action_done else R.string.action_snooze),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    group: ReminderGroup,
    now: LocalDateTime,
    dayLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val overdue = group == ReminderGroup.OVERDUE
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().sharedContainer("reminder-${reminder.id}"),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.Start) {
                Text(
                    reminder.due.formatTime(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .padding(horizontal = Spacing.sm)
                    .width(3.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        reminder.fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (reminder.isSnoozed) SnoozedBadge()
                    if (!reminder.hasExplicitTime) AllDayBadge()
                }
            }
        }
    }
}

@Composable
private fun SnoozedBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(stringResource(R.string.badge_snoozed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun AllDayBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(Icons.Rounded.AlarmOff, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.badge_default_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Placeholder for the second pane on large screens when nothing is selected. */
@Composable
fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.NotificationsNone,
            title = stringResource(R.string.detail_placeholder_title),
            message = stringResource(R.string.detail_placeholder_text),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

private fun previewState(): HomeUiState {
    val now = PreviewData.now
    val sections =
        PreviewData.reminders
            .groupBy { r ->
                when {
                    r.due.isBefore(now) -> ReminderGroup.OVERDUE
                    r.due.toLocalDate() == now.toLocalDate() -> ReminderGroup.TODAY
                    r.due.toLocalDate() == now.toLocalDate().plusDays(1) -> ReminderGroup.TOMORROW
                    else -> ReminderGroup.UPCOMING
                }
            }.map { HomeSection(it.key, it.value) }
            .sortedBy { it.group.ordinal }
    return HomeUiState(
        loading = false,
        sections = sections,
        totalCount = PreviewData.reminders.size,
        lastScanAt = System.currentTimeMillis() - 4 * 60_000,
        vaultName = "SebiMain",
        now = now,
    )
}

@ThemePreviews
@Composable
private fun HomePreview() {
    VaultbellTheme(dynamicColor = false) { HomeScreen(previewState(), HomeActions()) }
}

@ThemePreviews
@Composable
private fun HomeExactWarningPreview() {
    VaultbellTheme(dynamicColor = false) { HomeScreen(previewState().copy(exactAlarmsAllowed = false), HomeActions()) }
}

@ThemePreviews
@Composable
private fun HomeEmptyPreview() {
    VaultbellTheme(dynamicColor = false) { HomeScreen(HomeUiState(loading = false, lastScanAt = 0), HomeActions()) }
}

@ThemePreviews
@Composable
private fun HomeAmoledPreview() {
    VaultbellTheme(dynamicColor = false, amoledBlack = true) { HomeScreen(previewState(), HomeActions()) }
}
