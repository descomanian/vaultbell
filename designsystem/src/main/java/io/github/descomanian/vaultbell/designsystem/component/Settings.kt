package io.github.descomanian.vaultbell.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.designsystem.rememberHaptics
import io.github.descomanian.vaultbell.designsystem.theme.Spacing

/** A card that groups related settings rows, with an optional title (null when the screen title already names it). */
@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.md)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.lg, bottom = Spacing.xs).semantics { heading() }
            )
        } else {
            Spacer(Modifier.height(Spacing.md))
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(vertical = Spacing.xxs), content = content)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = Spacing.md),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/** Base row: leading icon, title, optional description, trailing slot. */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                modifier = Modifier.padding(horizontal = Spacing.xxs).size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (description != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
        trailing?.invoke(this)
    }
}

/** Whole row is the toggle target; Switch is visual only (single TalkBack focus). */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    enabled: Boolean = true
) {
    val haptics = rememberHaptics()
    SettingsRow(
        title = title,
        icon = icon,
        description = description,
        enabled = enabled,
        modifier =
        modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = {
                haptics.toggle(it)
                onCheckedChange(it)
            }
        ),
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) }
    )
}

/** Row showing the current value of a choice, opening a dialog on click. */
@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    SettingsRow(
        title = title,
        description = value,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.semantics { stateDescription = value },
        trailing = trailing
    )
}

/** Slider with label and live value. [steps] follows Material semantics (values between ends). */
@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    visible: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    AnimatedVisibility(visible = visible) {
        Column(modifier.fillMaxWidth().padding(bottom = Spacing.xs)) {
            SettingsRow(
                title = title,
                icon = icon,
                description = description,
                enabled = enabled,
                trailing = {
                    Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            )
            Slider(
                value = value,
                onValueChange = {
                    if (steps > 0) haptics.tick()
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                modifier =
                Modifier
                    .padding(start = 72.dp, end = Spacing.md)
                    .semantics { stateDescription = valueLabel }
            )
        }
    }
}
