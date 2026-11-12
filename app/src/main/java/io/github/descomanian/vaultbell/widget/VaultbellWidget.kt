package io.github.descomanian.vaultbell.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.data.Reminder
import io.github.descomanian.vaultbell.designsystem.theme.AccentColor
import io.github.descomanian.vaultbell.designsystem.theme.fallbackColorScheme
import io.github.descomanian.vaultbell.ui.MainActivity
import io.github.descomanian.vaultbell.ui.common.formatDay
import io.github.descomanian.vaultbell.ui.common.formatTime
import java.time.LocalDateTime

/**
 * Home screen widget with the next reminders.
 * Small: next reminder only. Medium: next three. Large: up to eight in a scrollable list.
 * Uses Dynamic Color on Android 12+ (if enabled in the app), otherwise the chosen accent palette.
 */
class VaultbellWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val container = (context.applicationContext as VaultbellApplication).container
        val settings = container.settings.current()
        val reminders = container.repository.upcoming(limit = 8)
        val hasVault = settings.vaultUri != null
        val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val accent = settings.accent

        provideContent {
            GlanceTheme(colors = if (useDynamic) GlanceTheme.colors else accentColors(accent)) {
                WidgetContent(reminders, hasVault, LocalDateTime.now())
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 150.dp)
        val LARGE = DpSize(250.dp, 300.dp)

        suspend fun refresh(context: Context) {
            runCatching { VaultbellWidget().updateAll(context) }
        }
    }
}

class VaultbellWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VaultbellWidget()
}

/** Refresh button: rescans the vault, the repository then updates all widgets. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        (context.applicationContext as VaultbellApplication).container.repository.scan()
    }
}

private fun accentColors(accent: AccentColor) =
    ColorProviders(
        light = fallbackColorScheme(accent, dark = false),
        dark = fallbackColorScheme(accent, dark = true)
    )

private fun openApp(
    context: Context,
    reminderId: Long? = null
): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .apply { if (reminderId != null) putExtra(MainActivity.EXTRA_OPEN_REMINDER, reminderId) }

@Composable
private fun WidgetContent(
    reminders: List<Reminder>,
    hasVault: Boolean,
    now: LocalDateTime
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val small = size.width < VaultbellWidget.MEDIUM.width || size.height < VaultbellWidget.MEDIUM.height

    Column(
        modifier =
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(14.dp)
    ) {
        Header(compact = small)
        Spacer(GlanceModifier.height(if (small) 6.dp else 10.dp))
        when {
            !hasVault -> Message(context.getString(R.string.widget_no_vault), openApp(context))
            reminders.isEmpty() -> Message(context.getString(R.string.widget_empty), openApp(context))
            small -> NextReminder(reminders.first(), now)
            size.height < VaultbellWidget.LARGE.height ->
                Column {
                    reminders.take(3).forEach { ReminderRow(it, now) }
                }
            else ->
                LazyColumn {
                    items(reminders, itemId = { it.id }) { ReminderRow(it, now) }
                }
        }
    }
}

@Composable
private fun Header(compact: Boolean) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_notification),
            contentDescription = null,
            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(18.dp)
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = context.getString(if (compact) R.string.widget_next else R.string.app_name),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openApp(context)))
        )
        if (!compact) {
            Box(
                modifier =
                GlanceModifier
                    .size(32.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .clickable(actionRunCallback<RefreshAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = context.getString(R.string.action_scan_now),
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                    modifier = GlanceModifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NextReminder(
    reminder: Reminder,
    now: LocalDateTime
) {
    val context = LocalContext.current
    val overdue = reminder.due.isBefore(now)
    Column(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(openApp(context, reminder.id)))) {
        Text(
            text = reminder.due.formatTime(context),
            style =
            TextStyle(
                color = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = reminder.due.formatDay(context, now),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = reminder.title,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 2
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    now: LocalDateTime
) {
    val context = LocalContext.current
    val overdue = reminder.due.isBefore(now)
    val accent: ColorProvider = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.primary
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier =
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity(openApp(context, reminder.id))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.width(64.dp)) {
                Text(
                    text = reminder.due.formatTime(context),
                    style = TextStyle(color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = reminder.due.formatDay(context, now),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                    maxLines = 1
                )
            }
            Box(modifier = GlanceModifier.width(3.dp).height(30.dp).cornerRadius(2.dp).background(accent)) {}
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = reminder.title,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                    maxLines = 1
                )
                Text(
                    text = reminder.fileName,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Message(
    text: String,
    intent: Intent
) {
    Box(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
        )
    }
}
