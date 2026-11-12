package io.github.descomanian.vaultbell.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun VaultbellTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    accent: AccentColor = AccentColor.SAGE,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    val context = LocalContext.current
    val colorScheme =
        remember(dark, dynamicColor, amoledBlack, accent, context) {
            val base =
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    fallbackColorScheme(accent, dark)
                }
            if (dark && amoledBlack) base.toAmoled() else base
        }
    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VaultbellTypography,
            shapes = VaultbellShapes,
            content = content,
        )
    }
}

object VaultbellThemeExtras {
    val status: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
