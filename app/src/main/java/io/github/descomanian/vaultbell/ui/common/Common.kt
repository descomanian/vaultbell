package io.github.descomanian.vaultbell.ui.common

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.data.ReminderGroup
import io.github.descomanian.vaultbell.data.toEpochMillis
import io.github.descomanian.vaultbell.parser.ReminderFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------------------------
// Shared element plumbing: screens read these locals; previews simply leave them null.
// ---------------------------------------------------------------------------------------------

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Container transform between a list card and the detail card. No-op outside a shared transition. */
@Composable
fun Modifier.sharedContainer(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------------------------

/**
 * Clock time in the app language. An explicit 12/24-hour system setting wins;
 * otherwise the app language decides (de: 18:30, en: 6:30 PM), like Android itself does.
 */
fun LocalDateTime.formatTime(context: Context): String {
    val locale = context.resources.configuration.locales[0]
    val formatter =
        when (android.provider.Settings.System.getString(context.contentResolver, android.provider.Settings.System.TIME_12_24)) {
            "24" -> DateTimeFormatter.ofPattern("HH:mm", locale)
            "12" -> DateTimeFormatter.ofPattern("h:mm a", locale)
            else -> DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale)
        }
    return format(formatter)
}

/** Platform relative times start upper-case ("Vor 4 Minuten"); inside a sentence they must not. */
fun CharSequence.inSentence(context: Context): String =
    toString().replaceFirstChar { it.lowercase(context.resources.configuration.locales[0]) }

/** "Today", "Tomorrow" or a short weekday + date. */
fun LocalDateTime.formatDay(
    context: Context,
    now: LocalDateTime
): String =
    when (toLocalDate()) {
        now.toLocalDate() -> context.getString(R.string.day_today)
        now.toLocalDate().plusDays(1) -> context.getString(R.string.day_tomorrow)
        now.toLocalDate().minusDays(1) -> context.getString(R.string.day_yesterday)
        else ->
            DateUtils.formatDateTime(
                context,
                toEpochMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
            )
    }

/** "in 2 hours" / "5 min. ago", localized by the platform, lower-cased for use inside a sentence. */
fun LocalDateTime.formatRelative(
    context: Context,
    now: LocalDateTime
): String =
    DateUtils.getRelativeTimeSpanString(
        toEpochMillis(),
        now.toEpochMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).inSentence(context)

fun formatMinutes(
    context: Context,
    minutes: Int
): String =
    when {
        minutes < 60 -> context.getString(R.string.duration_minutes, minutes)
        minutes % 60 == 0 -> context.getString(R.string.duration_hours, minutes / 60)
        else -> context.getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }

@Composable
fun ReminderGroup.label(): String =
    stringResource(
        when (this) {
            ReminderGroup.OVERDUE -> R.string.group_overdue
            ReminderGroup.TODAY -> R.string.group_today
            ReminderGroup.TOMORROW -> R.string.group_tomorrow
            ReminderGroup.UPCOMING -> R.string.group_upcoming
        }
    )

@Composable
fun ReminderFormat.label(): String =
    stringResource(
        when (this) {
            ReminderFormat.REMINDER_PLUGIN -> R.string.format_reminder_plugin
            ReminderFormat.DATAVIEW -> R.string.format_dataview
            ReminderFormat.TASKS_EMOJI -> R.string.format_tasks
        }
    )

// ---------------------------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------------------------

/** Single-choice dialog with accessible radio rows (whole row is the target). */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                }
                            ).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
