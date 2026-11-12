package io.github.descomanian.vaultbell.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.descomanian.vaultbell.AppContainer
import io.github.descomanian.vaultbell.data.DoneResult
import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.data.ReminderGroup
import io.github.descomanian.vaultbell.data.groupAt
import io.github.descomanian.vaultbell.system.SystemStatus
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter { ALL, TODAY, OVERDUE, SNOOZED }

data class HomeSection(
    val group: ReminderGroup,
    val items: List<Reminder>,
    val collapsed: Boolean = false
)

data class HomeUiState(
    val loading: Boolean = true,
    val sections: List<HomeSection> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val filter: HomeFilter = HomeFilter.ALL,
    val scanning: Boolean = false,
    val lastScanAt: Long = 0L,
    val vaultName: String = "",
    val hasVault: Boolean = true,
    val scanError: Boolean = false,
    val exactAlarmsAllowed: Boolean = true,
    val now: LocalDateTime = LocalDateTime.now()
) {
    val isEmpty: Boolean get() = sections.all { it.items.isEmpty() }
}

sealed interface HomeEvent {
    data class Done(val id: Long, val result: DoneResult) : HomeEvent

    data class Snoozed(val minutes: Int) : HomeEvent
}

class HomeViewModel(
    private val container: AppContainer,
    private val statusProvider: () -> SystemStatus
) : ViewModel() {
    private val repo = container.repository
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(HomeFilter.ALL)
    private val overdueExpanded = MutableStateFlow<Boolean?>(null)
    private val exactAllowed = MutableStateFlow(true)
    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    /** Ticks every 30 s so groups (Today → Overdue) stay correct while the screen is open. */
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    private val inputs =
        combine(query, filter, overdueExpanded, exactAllowed, clock) { q, f, o, e, now -> Inputs(q, f, o, e, now) }

    val state: StateFlow<HomeUiState> =
        combine(repo.activeReminders, container.settings.settings, repo.scanning, inputs) { reminders, s, scanning, i ->
            val q = i.query.trim()
            val filtered =
                reminders.filter { r ->
                    (q.isEmpty() || r.title.contains(q, ignoreCase = true) || r.relativePath.contains(q, ignoreCase = true)) &&
                        when (i.filter) {
                            HomeFilter.ALL -> true
                            HomeFilter.TODAY -> r.groupAt(i.now).let { it == ReminderGroup.TODAY || it == ReminderGroup.OVERDUE }
                            HomeFilter.OVERDUE -> r.groupAt(i.now) == ReminderGroup.OVERDUE
                            HomeFilter.SNOOZED -> r.isSnoozed
                        }
                }
            val grouped = filtered.groupBy { it.groupAt(i.now) }
            val showOverdue = i.overdueExpanded ?: s.showOverdueOnStart
            HomeUiState(
                loading = false,
                sections =
                ReminderGroup.entries.mapNotNull { g ->
                    grouped[g]?.let { items ->
                        HomeSection(
                            group = g,
                            items = items,
                            collapsed = g == ReminderGroup.OVERDUE && !showOverdue && i.filter == HomeFilter.ALL && q.isEmpty()
                        )
                    }
                },
                totalCount = reminders.size,
                query = i.query,
                filter = i.filter,
                scanning = scanning,
                lastScanAt = s.lastScanAt,
                vaultName = s.vaultName,
                hasVault = s.vaultUri != null,
                scanError = s.lastScanError != null,
                exactAlarmsAllowed = i.exactAllowed,
                now = i.now
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: HomeFilter) {
        filter.value = value
    }

    fun onToggleOverdue() {
        val collapsed = state.value.sections.firstOrNull { it.group == ReminderGroup.OVERDUE }?.collapsed ?: false
        overdueExpanded.value = collapsed
    }

    fun refreshStatus() {
        exactAllowed.value = statusProvider().exactAlarmsAllowed
    }

    fun refresh() {
        viewModelScope.launch { repo.scan() }
    }

    fun markDone(id: Long) {
        viewModelScope.launch {
            repo.markDone(id)?.let { events.send(HomeEvent.Done(id, it)) }
        }
    }

    fun undoDone(id: Long) {
        viewModelScope.launch { repo.undoDone(id) }
    }

    fun snooze(id: Long) {
        viewModelScope.launch {
            val minutes = container.settings.current().snoozeShortMinutes
            repo.snooze(id, minutes)
            events.send(HomeEvent.Snoozed(minutes))
        }
    }

    private data class Inputs(
        val query: String,
        val filter: HomeFilter,
        val overdueExpanded: Boolean?,
        val exactAllowed: Boolean,
        val now: LocalDateTime
    )
}
