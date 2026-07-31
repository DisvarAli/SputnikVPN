package com.my.vpn.ui.screens.menu

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.my.vpn.AppConstants
import com.my.vpn.R
import com.my.vpn.ui.components.MenuScreenScaffold
import com.my.vpn.ui.components.ReleaseNotesDropdown
import com.my.vpn.ui.components.TunnelMenuRow
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.viewmodel.MainViewModel
import com.my.vpn.update.BundledChangelog
import com.my.vpn.update.ChangelogFormatter

@Composable
fun MenuAppUpdateScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bundledChangelog = remember { BundledChangelog.load(context) }
    val whatsNewChangelog = remember(state.appReleaseInfo, bundledChangelog) {
        val remote = state.appReleaseInfo
        if (remote != null && remote.changelog.isNotBlank()) remote.changelog else bundledChangelog
    }
    val whatsNewVersion = state.appReleaseInfo?.versionName ?: AppConstants.APP_VERSION_NAME

    val apkInstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.clearApkInstallIntent() }

    LaunchedEffect(state.apkInstallIntent) {
        state.apkInstallIntent?.let { apkInstallLauncher.launch(it) }
    }

    if (state.showAppUpdateDialog && state.appReleaseInfo != null) {
        val release = state.appReleaseInfo
        val entries = ChangelogFormatter.formatEntries(release.changelog)
        AlertDialog(
            onDismissRequest = { viewModel.dismissAppUpdateDialog(markDismissed = true) },
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
            title = { Text(stringResource(R.string.app_update_available, release.versionName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dpScaled())) {
                    entries.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissAppUpdateDialog(markDismissed = false)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)))
                }) {
                    Text(stringResource(R.string.open_on_github))
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { viewModel.postponeAppUpdate() }) {
                        Text(stringResource(R.string.update_later))
                    }
                    TextButton(onClick = { viewModel.neverPromptAppUpdate() }) {
                        Text(stringResource(R.string.never_ask_update))
                    }
                    TextButton(onClick = { viewModel.dismissAppUpdateDialog(markDismissed = true) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    MenuScreenScaffold(
        title = stringResource(R.string.section_app_update),
        subtitle = stringResource(R.string.app_update_hint),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dpScaled(), vertical = 8.dpScaled()),
            verticalArrangement = Arrangement.spacedBy(8.dpScaled())
        ) {
            TunnelMenuRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.check_app_update),
                subtitle = state.appUpdateStatusMessage,
                onClick = viewModel::checkAppUpdateManual
            )
            OutlinedButton(
                onClick = viewModel::downloadAndInstallAppUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dpScaled()),
                enabled = state.apkDownloadProgress == null
            ) {
                Text(
                    if (state.apkDownloadProgress != null) {
                        stringResource(R.string.apk_download_progress, state.apkDownloadProgress!!)
                    } else {
                        stringResource(R.string.download_install_apk)
                    }
                )
            }
            ReleaseNotesDropdown(
                versionName = whatsNewVersion,
                changelogRaw = whatsNewChangelog,
                initiallyExpanded = state.showAppUpdateDialog,
                modifier = Modifier.padding(top = 8.dpScaled())
            )
            Spacer(modifier = Modifier.height(24.dpScaled()))
        }
    }
}
