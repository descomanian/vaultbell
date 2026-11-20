package io.github.descomanian.vaultbell.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.data.DoneResult
import io.github.descomanian.vaultbell.data.RepeatLogFormat
import io.github.descomanian.vaultbell.system.SystemIntents
import io.github.descomanian.vaultbell.system.SystemStatus
import io.github.descomanian.vaultbell.ui.common.formatMinutes
import io.github.descomanian.vaultbell.ui.detail.DetailActions
import io.github.descomanian.vaultbell.ui.detail.DetailScreen
import io.github.descomanian.vaultbell.ui.detail.DetailViewModel
import io.github.descomanian.vaultbell.ui.home.DetailPlaceholder
import io.github.descomanian.vaultbell.ui.home.HomeActions
import io.github.descomanian.vaultbell.ui.home.HomeEvent
import io.github.descomanian.vaultbell.ui.home.HomeScreen
import io.github.descomanian.vaultbell.ui.home.HomeViewModel
import io.github.descomanian.vaultbell.ui.onboarding.OnboardingActions
import io.github.descomanian.vaultbell.ui.onboarding.OnboardingScreen
import io.github.descomanian.vaultbell.ui.onboarding.OnboardingViewModel
import io.github.descomanian.vaultbell.ui.settings.SettingsActions
import io.github.descomanian.vaultbell.ui.settings.SettingsScreen
import io.github.descomanian.vaultbell.ui.settings.SettingsViewModel

@Composable
private fun vaultbellApp(): VaultbellApplication = LocalContext.current.applicationContext as VaultbellApplication

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(SystemIntents.appDetails(this))
    }
}

/** Direct exemption dialog; some ROMs block it, then the list of all apps opens instead. */
private fun Context.requestBackground() {
    try {
        startActivity(SystemIntents.requestBatteryExemption(this))
    } catch (e: ActivityNotFoundException) {
        startSafely(SystemIntents.batterySettings())
    }
}

/** SAF picker starts in Documents/ where most Android vaults live. */
private fun documentsFolderHint(): Uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Documents")

@Composable
fun HomeRoute(
    twoPane: Boolean,
    showSettingsAction: Boolean,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val app = vaultbellApp()
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel { HomeViewModel(app.container) { SystemStatus.read(app) } }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshStatus()
        vm.scanSilently()
    }
    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            when (event) {
                is HomeEvent.Done -> {
                    val message =
                        context.getString(
                            when (event.result) {
                                DoneResult.WRITTEN_TO_FILE -> R.string.snackbar_done_written
                                DoneResult.CONFLICT_NOT_WRITTEN -> R.string.snackbar_done_conflict
                                DoneResult.NOT_UTF8 -> R.string.snackbar_done_not_utf8
                                DoneResult.WRITE_FAILED -> R.string.snackbar_done_write_failed
                                DoneResult.NEXT_INSTANCE -> R.string.snackbar_done_series
                                DoneResult.REPEATING_LOGGED -> R.string.snackbar_done_repeating
                                DoneResult.LOCAL_ONLY -> R.string.snackbar_done
                            }
                        )
                    val undo = event.result != DoneResult.WRITTEN_TO_FILE
                    val result =
                        snackbar.showSnackbar(
                            message = message,
                            actionLabel = if (undo) context.getString(R.string.action_undo) else null,
                            duration = SnackbarDuration.Short
                        )
                    if (result == SnackbarResult.ActionPerformed) vm.undoDone(event.id)
                }
                is HomeEvent.Snoozed ->
                    snackbar.showSnackbar(context.getString(R.string.snackbar_snoozed, formatMinutes(context, event.minutes)))
            }
        }
    }

    val actions =
        HomeActions(
            onQueryChange = vm::onQueryChange,
            onFilterChange = vm::onFilterChange,
            onRefresh = vm::refresh,
            onOpen = { id -> if (twoPane) selected = id else onOpen(id) },
            onDone = { id ->
                if (selected == id) selected = null
                vm.markDone(id)
            },
            onSnooze = vm::snooze,
            onToggleOverdue = vm::onToggleOverdue,
            onOpenSettings = onOpenSettings,
            onFixExactAlarms = { context.startSafely(SystemIntents.exactAlarmSettings(context)) }
        )

    if (twoPane) {
        Row {
            HomeScreen(state, actions, Modifier.weight(1f), snackbar, showSettingsAction, selected)
            VerticalDivider()
            Box(Modifier.weight(1.2f).fillMaxHeight()) {
                val id = selected
                if (id == null) {
                    DetailPlaceholder()
                } else {
                    DetailRoute(id = id, showBack = false, onBack = { selected = null })
                }
            }
        }
    } else {
        HomeScreen(state, actions, snackbarHostState = snackbar, showSettingsAction = showSettingsAction)
    }
}

@Composable
fun DetailRoute(
    id: Long,
    onBack: () -> Unit,
    showBack: Boolean = true
) {
    val app = vaultbellApp()
    val vm: DetailViewModel = viewModel(key = "detail-$id") { DetailViewModel(id, app.container) }
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        actions =
        DetailActions(
            onBack = onBack,
            onDone = { vm.done(onBack) },
            onSnooze = { minutes -> vm.snooze(minutes, onBack) }
        ),
        showBack = showBack
    )
}

@Composable
fun OnboardingRoute(onFinished: () -> Unit) {
    val app = vaultbellApp()
    val context = LocalContext.current
    val vm: OnboardingViewModel = viewModel { OnboardingViewModel(app, app.container) }
    val state by vm.state.collectAsStateWithLifecycle()
    var denials by remember { mutableIntStateOf(0) }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) vm.onVaultPicked(uri)
        }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.refreshPermissions()
            if (!granted && ++denials >= 2) context.startSafely(SystemIntents.notificationSettings(context))
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }

    OnboardingScreen(
        state = state,
        actions =
        OnboardingActions(
            onNext = vm::next,
            onBack = vm::back,
            onPickVault = { picker.launch(documentsFolderHint()) },
            onVaultNameChange = vm::onVaultNameChange,
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startSafely(SystemIntents.notificationSettings(context))
                }
            },
            onRequestExactAlarms = { context.startSafely(SystemIntents.exactAlarmSettings(context)) },
            onRequestBackground = { context.requestBackground() },
            onFinish = { vm.finish(onFinished) }
        )
    )
}

@Composable
fun SettingsRoute(
    showBack: Boolean,
    twoPane: Boolean,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val app = vaultbellApp()
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(app, app.container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) vm.changeVault(uri)
        }
    val logLabels = RepeatLogFormat.Labels(stringResource(R.string.repeat_log_heading), stringResource(R.string.repeat_log_intro))
    // "Save as" starts in the vault, so the log can go straight next to the notes.
    val exportLog =
        rememberLauncherForActivityResult(CreateMarkdownDocument { state.settings.vaultUri?.let(::vaultRootDocument) }) { uri ->
            if (uri != null) vm.exportRepeatLog(uri, logLabels)
        }
    LaunchedEffect(vm) {
        vm.exportResults.collect { result ->
            val count = result.count
            val text =
                if (count == null) {
                    context.getString(R.string.repeat_log_export_failed)
                } else {
                    context.resources.getQuantityString(R.plurals.repeat_log_exported, count, count)
                }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshStatus() }

    SettingsScreen(
        state = state,
        showBack = showBack,
        twoPane = twoPane,
        actions =
        SettingsActions(
            onBack = onBack,
            update = vm::update,
            updateAndRescan = vm::updateAndRescan,
            updateAndReschedule = vm::updateAndReschedule,
            onScanInterval = vm::setScanInterval,
            onLanguage = vm::setLanguage,
            onPickVault = { picker.launch(state.settings.vaultUri?.let(Uri::parse) ?: documentsFolderHint()) },
            onScanNow = vm::scanNow,
            onOpenNotificationSettings = { context.startSafely(SystemIntents.notificationSettings(context)) },
            onOpenExactAlarmSettings = { context.startSafely(SystemIntents.exactAlarmSettings(context)) },
            onOpenBatterySettings = { context.requestBackground() },
            onOpenPrivacy = onOpenPrivacy,
            onOpenAbout = onOpenAbout,
            onExportRepeatLog = { exportLog.launch(vm.repeatLogFileName()) }
        )
    )
}

/** "Save as" for Markdown with an optional start folder ([DocumentsContract.EXTRA_INITIAL_URI]). */
private class CreateMarkdownDocument(
    private val initialUri: () -> Uri?
) : ActivityResultContracts.CreateDocument("text/markdown") {
    override fun createIntent(
        context: Context,
        input: String
    ): Intent = super.createIntent(context, input).apply { initialUri()?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) } }
}

/** Document URI of the vault root, usable as start folder for system file dialogs. */
private fun vaultRootDocument(treeUri: String): Uri? =
    runCatching {
        val tree = Uri.parse(treeUri)
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    }.getOrNull()
