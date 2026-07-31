package com.my.vpn.ui.screens.menu

import android.app.Activity
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.my.vpn.R
import com.my.vpn.ui.components.AddSubscriptionDialog
import com.my.vpn.ui.components.DeleteSubscriptionConfirmDialog
import com.my.vpn.ui.components.ManageSubscriptionsDialog
import com.my.vpn.ui.components.MenuScreenScaffold
import com.my.vpn.ui.components.TunnelMenuRow
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.viewmodel.MainViewModel
import com.my.vpn.util.QrScanUtil
import com.my.vpn.util.SubscriptionImportUtil

@Composable
fun MenuSubscriptionsScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pendingImportTitle by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val t = pendingImportTitle
        if (uri == null || t.isBlank()) return@rememberLauncherForActivityResult
        val text = SubscriptionImportUtil.readTextFromUri(context, uri)
        if (text != null) {
            viewModel.addCustomSubscriptionFromImport(
                t,
                SubscriptionImportUtil.extractSubscriptionText(text, context.applicationContext)
            )
        } else {
            viewModel.dismissAddSubscriptionDialog()
        }
    }

    if (state.showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            error = state.addSubscriptionError,
            onDismiss = viewModel::dismissAddSubscriptionDialog,
            onConfirmUrl = viewModel::addCustomSubscription,
            onPickFile = { title ->
                pendingImportTitle = title
                filePicker.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
            },
            onScanQr = { title ->
                pendingImportTitle = title
                val activity = context as? Activity
                if (activity != null) {
                    QrScanUtil.startScan(activity) { raw ->
                        if (!raw.isNullOrBlank()) {
                            viewModel.addCustomSubscriptionFromImport(
                                title.ifBlank { context.getString(R.string.sub_qr_default_title) },
                                SubscriptionImportUtil.extractSubscriptionText(raw, context.applicationContext)
                            )
                        }
                    }
                }
            }
        )
    }
    if (state.showManageSubscriptionsDialog) {
        ManageSubscriptionsDialog(
            subscriptions = state.customSubscriptions,
            onDismiss = viewModel::dismissManageSubscriptionsDialog,
            onDelete = viewModel::requestDeleteCustomSubscription
        )
    }
    state.pendingDeleteSubscriptionTitle?.let { title ->
        DeleteSubscriptionConfirmDialog(
            title = title,
            onDismiss = viewModel::dismissDeleteSubscriptionConfirm,
            onConfirm = viewModel::confirmDeleteCustomSubscription
        )
    }
    if (state.showConfigFetchBypassDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfigFetchBypassDialog,
            title = { Text(stringResource(R.string.config_fetch_bypass_title)) },
            text = { Text(stringResource(R.string.config_fetch_bypass_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::startConfigFetchBypassAutoPick) {
                    Text(stringResource(R.string.config_fetch_bypass_start))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfigFetchBypassDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    MenuScreenScaffold(
        title = stringResource(R.string.section_subscriptions),
        subtitle = stringResource(R.string.subscriptions_menu_hint),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dpScaled(), vertical = 8.dpScaled()),
            verticalArrangement = Arrangement.spacedBy(6.dpScaled())
        ) {
            TunnelMenuRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.refresh_all_subscriptions),
                onClick = viewModel::fullRefreshFromMenu
            )
            TunnelMenuRow(
                icon = Icons.Default.Add,
                title = stringResource(R.string.add_subscription),
                subtitle = stringResource(R.string.add_subscription_subtitle),
                onClick = viewModel::openAddSubscriptionDialog
            )
            if (state.customSubscriptions.isNotEmpty()) {
                TunnelMenuRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.manage_subscriptions),
                    onClick = viewModel::openManageSubscriptionsDialog
                )
            }
            OutlinedButton(
                onClick = viewModel::checkSubscriptionUpdatesManual,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dpScaled())
            ) {
                Text(stringResource(R.string.check_subscription_updates))
            }
            state.subscriptionUpdateCheckMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dpScaled()))
        }
    }
}
