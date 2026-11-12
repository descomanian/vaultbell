package io.github.descomanian.vaultbell.designsystem

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Multi-preview: every screen is checked in light and dark. */
@Preview(name = "Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/** Large screen / foldable preview. */
@Preview(name = "Tablet", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
annotation class TabletPreview
