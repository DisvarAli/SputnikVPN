package com.my.vpn.ui.screens.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.my.vpn.R
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.ui.components.MenuScreenScaffold
import com.my.vpn.ui.components.TunnelMenuRow
import com.my.vpn.ui.components.TunnelMenuSectionTitle
import com.my.vpn.ui.theme.TunnelDelete
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.localizedLabel
import com.my.vpn.ui.viewmodel.MainViewModel

@Composable
fun MenuAdvancedScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    onBack: () -> Unit
) {
    val toggleOn = stringResource(R.string.toggle_on)
    val toggleOff = stringResource(R.string.toggle_off)
    val wifiOnly = stringResource(R.string.bg_ping_wifi_only_value)
    val anyNetwork = stringResource(R.string.bg_ping_any_network_value)

    if (state.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            icon = { Icon(Icons.Default.Delete, null, tint = TunnelDelete) },
            title = { Text(stringResource(R.string.clear_all_data_confirm_title)) },
            text = { Text(stringResource(R.string.clear_all_data_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::deleteAllData) {
                    Text(stringResource(R.string.delete_action), color = TunnelDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    MenuScreenScaffold(
        title = stringResource(R.string.section_advanced),
        subtitle = stringResource(R.string.menu_advanced_subtitle),
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
            Text(
                stringResource(R.string.security_warning_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
            )

            TunnelMenuSectionTitle(stringResource(R.string.section_list_servers))
            Text(
                stringResource(R.string.section_list_servers_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AdvancedSwitchRow(
                title = stringResource(R.string.show_all_subscription_title),
                hint = stringResource(R.string.show_all_subscription_hint),
                checked = state.showAllFromSubscription,
                onCheckedChange = viewModel::setShowAllFromSubscription
            )
            AdvancedSwitchRow(
                title = stringResource(R.string.show_unconfirmed_tunnel_title),
                hint = stringResource(R.string.show_unconfirmed_tunnel_hint),
                checked = state.showUnconfirmedTunnelConfigs,
                onCheckedChange = viewModel::setShowUnconfirmedTunnelConfigs
            )

            TunnelMenuSectionTitle(stringResource(R.string.section_vpn_security))
            AdvancedSwitchRow(
                title = stringResource(R.string.auto_failover_title),
                hint = stringResource(R.string.auto_failover_hint),
                checked = state.autoFailoverEnabled,
                onCheckedChange = viewModel::setAutoFailoverEnabled
            )

            TunnelMenuSectionTitle(stringResource(R.string.config_test_mode_title))
            Text(
                stringResource(R.string.config_test_mode_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ConfigTestMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.configTestMode == mode,
                    onClick = { viewModel.setConfigTestMode(mode) },
                    label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(end = 4.dpScaled())
                )
            }

            TunnelMenuSectionTitle(stringResource(R.string.section_background_tasks))
            TunnelMenuRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.bg_config_update),
                subtitle = if (state.backgroundConfigUpdate) toggleOn else toggleOff,
                onClick = { viewModel.setBackgroundConfigUpdate(!state.backgroundConfigUpdate) }
            )
            TunnelMenuRow(
                icon = Icons.Default.Wifi,
                title = stringResource(R.string.bg_ping_wifi_only),
                subtitle = if (state.backgroundPingWifiOnly) wifiOnly else anyNetwork,
                onClick = { viewModel.setBackgroundPingWifiOnly(!state.backgroundPingWifiOnly) }
            )
            OutlinedButton(
                onClick = viewModel::scheduleFullCachePing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dpScaled())
            ) {
                Text(stringResource(R.string.schedule_full_cache_ping))
            }

            TunnelMenuRow(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.clear_all_data),
                subtitle = stringResource(R.string.clear_all_data_subtitle),
                onClick = viewModel::requestDeleteAll
            )
            Spacer(modifier = Modifier.height(24.dpScaled()))
        }
    }
}

@Composable
private fun AdvancedSwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dpScaled())) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
