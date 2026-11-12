package io.github.descomanian.vaultbell.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.descomanian.vaultbell.BuildConfig
import io.github.descomanian.vaultbell.R
import io.github.descomanian.vaultbell.designsystem.ThemePreviews
import io.github.descomanian.vaultbell.designsystem.component.SettingsDivider
import io.github.descomanian.vaultbell.designsystem.component.SettingsGroup
import io.github.descomanian.vaultbell.designsystem.component.SettingsRow
import io.github.descomanian.vaultbell.designsystem.component.TonalIcon
import io.github.descomanian.vaultbell.designsystem.theme.Spacing
import io.github.descomanian.vaultbell.designsystem.theme.VaultbellTheme

@Composable
private fun InfoScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg),
            content = content
        )
    }
}

@Composable
private fun PromiseRow(
    icon: ImageVector,
    title: String,
    text: String
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        TonalIcon(icon, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    InfoScaffold(stringResource(R.string.privacy_title), onBack) {
        Text(
            stringResource(R.string.privacy_intro, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        )
        PromiseRow(
            Icons.Rounded.CloudOff,
            stringResource(R.string.privacy_no_internet_title),
            stringResource(R.string.privacy_no_internet_text)
        )
        PromiseRow(
            Icons.Rounded.VisibilityOff,
            stringResource(R.string.privacy_no_account_title),
            stringResource(R.string.privacy_no_account_text)
        )
        PromiseRow(Icons.Rounded.Folder, stringResource(R.string.privacy_saf_title), stringResource(R.string.privacy_saf_text))
        PromiseRow(Icons.Rounded.Lock, stringResource(R.string.privacy_local_title), stringResource(R.string.privacy_local_text))
        PromiseRow(
            Icons.Rounded.NotificationsActive,
            stringResource(R.string.privacy_permissions_title),
            stringResource(R.string.privacy_permissions_text)
        )
    }
}

private data class License(val name: String, val license: String)

private val licenses =
    listOf(
        License("AndroidX (Core, Activity, AppCompat, Lifecycle, Navigation)", "Apache License 2.0"),
        License("Jetpack Compose & Material 3", "Apache License 2.0"),
        License("Room, DataStore, WorkManager", "Apache License 2.0"),
        License("Kotlin & kotlinx.coroutines", "Apache License 2.0"),
        License("Material Symbols / Icons", "Apache License 2.0")
    )

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    InfoScaffold(stringResource(R.string.about_title), onBack) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalAlignment = Alignment.Start) {
            TonalIcon(
                Icons.Rounded.NotificationsActive,
                size = 72.dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.not_affiliated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SettingsGroup(stringResource(R.string.about_group_app)) {
            SettingsRow(title = stringResource(R.string.about_version), description = BuildConfig.VERSION_NAME, icon = Icons.Rounded.Tag)
            SettingsDivider()
            SettingsRow(title = stringResource(R.string.about_license), description = "GNU GPL v3.0", icon = Icons.Rounded.Gavel)
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.about_source),
                description = BuildConfig.REPO_URL.removePrefix("https://"),
                icon = Icons.Rounded.Code,
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.REPO_URL)))
                    } catch (e: ActivityNotFoundException) {
                        // No browser installed: nothing to do.
                    }
                },
                trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) }
            )
        }
        SettingsGroup(stringResource(R.string.about_group_licenses)) {
            licenses.forEachIndexed { index, l ->
                if (index > 0) SettingsDivider()
                SettingsRow(title = l.name, description = l.license)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PrivacyPreview() {
    VaultbellTheme(dynamicColor = false) { PrivacyScreen {} }
}

@ThemePreviews
@Composable
private fun AboutPreview() {
    VaultbellTheme(dynamicColor = false) { AboutScreen {} }
}
