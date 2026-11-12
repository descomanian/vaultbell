package io.github.descomanian.vaultbell.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Accent palettes used when Dynamic Color is off or unavailable (Android < 12). */
enum class AccentColor {
    SAGE,
    OCEAN,
    AMBER,
    ROSE,
    VIOLET
}

private data class Tones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color
)

private data class Accent(val light: Tones, val dark: Tones)

private val accents =
    mapOf(
        AccentColor.SAGE to
            Accent(
                light = Tones(Color(0xFF3F6656), Color.White, Color(0xFFC1ECD8), Color(0xFF002116), Color(0xFFD0E8DC), Color(0xFF0B1F17)),
                dark =
                Tones(
                    Color(0xFFA6D0BC),
                    Color(0xFF0F3729),
                    Color(0xFF274E3F),
                    Color(0xFFC1ECD8),
                    Color(0xFF344B41),
                    Color(0xFFD0E8DC)
                )
            ),
        AccentColor.OCEAN to
            Accent(
                light = Tones(Color(0xFF2F628C), Color.White, Color(0xFFCEE5FF), Color(0xFF001D33), Color(0xFFD6E3F2), Color(0xFF101C2B)),
                dark =
                Tones(
                    Color(0xFF9BCBFB),
                    Color(0xFF003354),
                    Color(0xFF124A72),
                    Color(0xFFCEE5FF),
                    Color(0xFF3A4857),
                    Color(0xFFD6E3F2)
                )
            ),
        AccentColor.AMBER to
            Accent(
                light = Tones(Color(0xFF7A5900), Color.White, Color(0xFFFFDEA3), Color(0xFF261900), Color(0xFFF4E0BF), Color(0xFF241A04)),
                dark =
                Tones(
                    Color(0xFFF6BE48),
                    Color(0xFF402D00),
                    Color(0xFF5C4300),
                    Color(0xFFFFDEA3),
                    Color(0xFF52452A),
                    Color(0xFFF4E0BF)
                )
            ),
        AccentColor.ROSE to
            Accent(
                light = Tones(Color(0xFF8E4957), Color.White, Color(0xFFFFD9DF), Color(0xFF3A0716), Color(0xFFF6DDE0), Color(0xFF2B1519)),
                dark =
                Tones(
                    Color(0xFFFFB1C0),
                    Color(0xFF561D2A),
                    Color(0xFF713340),
                    Color(0xFFFFD9DF),
                    Color(0xFF5B3F44),
                    Color(0xFFF6DDE0)
                )
            ),
        AccentColor.VIOLET to
            Accent(
                light = Tones(Color(0xFF65558F), Color.White, Color(0xFFE9DDFF), Color(0xFF201047), Color(0xFFE8DEF8), Color(0xFF1D192B)),
                dark =
                Tones(
                    Color(0xFFCFBDFE),
                    Color(0xFF36275D),
                    Color(0xFF4D3D75),
                    Color(0xFFE9DDFF),
                    Color(0xFF4A4458),
                    Color(0xFFE8DEF8)
                )
            )
    )

/** Swatch shown in the accent picker. */
val AccentColor.swatch: Color get() = accents.getValue(this).light.primary

/** Own palette for [accent]; also used by the home screen widget. */
fun fallbackColorScheme(
    accent: AccentColor,
    dark: Boolean
): ColorScheme {
    val a = accents.getValue(accent)
    return if (dark) {
        val t = a.dark
        darkColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            primaryContainer = t.primaryContainer,
            onPrimaryContainer = t.onPrimaryContainer,
            inversePrimary = a.light.primary,
            secondary = Color(0xFFB4CCBF),
            onSecondary = Color(0xFF20352B),
            secondaryContainer = t.secondaryContainer,
            onSecondaryContainer = t.onSecondaryContainer,
            tertiary = Color(0xFFA5CCDF),
            onTertiary = Color(0xFF073543),
            tertiaryContainer = Color(0xFF244C5B),
            onTertiaryContainer = Color(0xFFC1E8FB),
            background = Color(0xFF101412),
            onBackground = Color(0xFFE0E3DF),
            surface = Color(0xFF101412),
            onSurface = Color(0xFFE0E3DF),
            surfaceVariant = Color(0xFF404944),
            onSurfaceVariant = Color(0xFFBFC9C3),
            surfaceTint = t.primary,
            inverseSurface = Color(0xFFE0E3DF),
            inverseOnSurface = Color(0xFF2E312F),
            outline = Color(0xFF8A938E),
            outlineVariant = Color(0xFF404944),
            surfaceBright = Color(0xFF363A38),
            surfaceDim = Color(0xFF101412),
            surfaceContainerLowest = Color(0xFF0B0F0D),
            surfaceContainerLow = Color(0xFF191C1B),
            surfaceContainer = Color(0xFF1D201F),
            surfaceContainerHigh = Color(0xFF272B29),
            surfaceContainerHighest = Color(0xFF323634)
        )
    } else {
        val t = a.light
        lightColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            primaryContainer = t.primaryContainer,
            onPrimaryContainer = t.onPrimaryContainer,
            inversePrimary = a.dark.primary,
            secondary = Color(0xFF4D6358),
            onSecondary = Color.White,
            secondaryContainer = t.secondaryContainer,
            onSecondaryContainer = t.onSecondaryContainer,
            tertiary = Color(0xFF3D6373),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFC1E8FB),
            onTertiaryContainer = Color(0xFF001F29),
            background = Color(0xFFF8FAF7),
            onBackground = Color(0xFF191C1B),
            surface = Color(0xFFF8FAF7),
            onSurface = Color(0xFF191C1B),
            surfaceVariant = Color(0xFFDCE5DF),
            onSurfaceVariant = Color(0xFF404944),
            surfaceTint = t.primary,
            inverseSurface = Color(0xFF2E312F),
            inverseOnSurface = Color(0xFFEFF1EE),
            outline = Color(0xFF707974),
            outlineVariant = Color(0xFFBFC9C3),
            surfaceBright = Color(0xFFF8FAF7),
            surfaceDim = Color(0xFFD8DBD8),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF2F4F1),
            surfaceContainer = Color(0xFFECEFEB),
            surfaceContainerHigh = Color(0xFFE6E9E5),
            surfaceContainerHighest = Color(0xFFE1E3E0)
        )
    }
}

/** True black surfaces for OLED screens, keeping a subtle elevation ladder. */
internal fun ColorScheme.toAmoled(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF191919),
        surfaceContainerHighest = Color(0xFF222222)
    )

/** Semantic status colours (traffic light) with sufficient contrast in both themes. */
data class StatusColors(val ok: Color, val warning: Color, val error: Color)

internal val LightStatusColors = StatusColors(ok = Color(0xFF2E7D32), warning = Color(0xFF9A5B00), error = Color(0xFFBA1A1A))
internal val DarkStatusColors = StatusColors(ok = Color(0xFF81C784), warning = Color(0xFFFFB74D), error = Color(0xFFFFB4AB))
