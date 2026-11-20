package io.github.descomanian.vaultbell.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.data.settings.AppSettings
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme
import io.github.descomanian.vaultbell.designsystem.theme.isDark
import io.github.descomanian.vaultbell.ui.common.LocalNavAnimatedScope
import io.github.descomanian.vaultbell.ui.common.LocalSharedTransitionScope
import io.github.descomanian.vaultbell.ui.settings.AboutScreen
import io.github.descomanian.vaultbell.ui.settings.PrivacyScreen
import io.github.descomanian.vaultbell.work.ScanWork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: VaultbellApplication) : ViewModel() {
    val settings: StateFlow<AppSettings?> =
        app.container.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Scan on every app start (cheap: only changed files are read).
        viewModelScope.launch {
            val s = app.container.settings.current()
            if (s.onboardingDone && s.vaultUri != null) {
                ScanWork.schedulePeriodic(app, s.scanIntervalMinutes)
                app.container.repository.scan()
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(application as VaultbellApplication) } }
    }
    private val openReminder = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }
        enableEdgeToEdge()
        openReminder.value = intent.reminderId()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val s = settings
            if (s != null) {
                val dark = s.themeMode.isDark()
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                    )
                    onDispose {}
                }
                val windowSize = calculateWindowSizeClass(this)
                val startOnboarding = remember { !s.onboardingDone }
                VaultbellTheme(
                    themeMode = s.themeMode,
                    dynamicColor = s.dynamicColor,
                    amoledBlack = s.amoledBlack,
                    accent = s.accent
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        VaultbellNavHost(
                            startOnboarding = startOnboarding,
                            widthClass = windowSize.widthSizeClass,
                            openReminderId = openReminder.value,
                            onOpenConsumed = { openReminder.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.reminderId()?.let { openReminder.value = it }
    }

    private fun Intent.reminderId(): Long? = getLongExtra(EXTRA_OPEN_REMINDER, -1L).takeIf { it >= 0 }

    companion object {
        const val EXTRA_OPEN_REMINDER = "open_reminder"

        // Same scrims as the platform default for 3-button navigation.
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    fun detail(id: Long) = "detail/$id"
}

@Composable
fun VaultbellNavHost(
    startOnboarding: Boolean,
    widthClass: WindowWidthSizeClass,
    openReminderId: Long?,
    onOpenConsumed: () -> Unit
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val wide = widthClass != WindowWidthSizeClass.Compact
    val twoPane = widthClass == WindowWidthSizeClass.Expanded
    val showRail = wide && route != null && route != Routes.ONBOARDING

    LaunchedEffect(openReminderId) {
        if (openReminderId != null && !startOnboarding) {
            nav.navigate(Routes.detail(openReminderId)) { launchSingleTop = true }
            onOpenConsumed()
        }
    }

    Row {
        if (showRail) AppRail(nav, route)
        SharedTransitionLayout(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = nav,
                    startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME,
                    enterTransition = { fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.96f) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(280)) },
                    popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(280), targetScale = 0.96f) }
                ) {
                    composable(Routes.ONBOARDING) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            OnboardingRoute(
                                onFinished = {
                                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                                }
                            )
                        }
                    }
                    composable(Routes.HOME) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            HomeRoute(
                                twoPane = twoPane,
                                showSettingsAction = !wide,
                                onOpen = { id -> nav.navigate(Routes.detail(id)) },
                                onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } }
                            )
                        }
                    }
                    composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            DetailRoute(id = entry.arguments?.getLong("id") ?: -1L, onBack = { nav.popBackStack() })
                        }
                    }
                    composable(Routes.SETTINGS) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            SettingsRoute(
                                showBack = !wide,
                                twoPane = twoPane,
                                onBack = { nav.popBackStack() },
                                onOpenPrivacy = { nav.navigate(Routes.PRIVACY) },
                                onOpenAbout = { nav.navigate(Routes.ABOUT) }
                            )
                        }
                    }
                    composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}

@Composable
private fun AppRail(
    nav: NavHostController,
    route: String?
) {
    NavigationRail(
        header = {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
        }
    ) {
        Spacer(Modifier.weight(1f))
        val homeSelected = route == Routes.HOME || route == Routes.DETAIL
        NavigationRailItem(
            selected = homeSelected,
            onClick = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_reminders)) }
        )
        NavigationRailItem(
            selected = !homeSelected,
            onClick = {
                nav.navigate(Routes.SETTINGS) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings_title)) }
        )
        Spacer(Modifier.weight(1f))
    }
}
