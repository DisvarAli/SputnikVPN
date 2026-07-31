package com.my.vpn.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.vpn.AppConstants
import com.my.vpn.R
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.CustomSubscription
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.ui.components.DeleteSubscriptionConfirmDialog
import com.my.vpn.ui.components.SubscriptionTrafficBar
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.theme.TunnelAqua
import com.my.vpn.ui.theme.TunnelConnected
import com.my.vpn.ui.theme.TunnelDelete
import com.my.vpn.ui.viewmodel.ListFilter
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.protocolFilterLabelLocalized
import com.my.vpn.ui.selectedListLabelLocalized
import com.my.vpn.ui.viewmodel.MainViewModel
import com.my.vpn.util.CountryNames
import com.my.vpn.util.FormatUtil
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshConnectionState()
    }

    if (state.showInstructionDialog) {
        InstructionReminderDialog(
            onOpenHelp = { viewModel.dismissInstructionDialog(openHelp = true) },
            onDismiss = { viewModel.dismissInstructionDialog(openHelp = false) }
        )
    }

    if (state.showRestartDialog) {
        RestartDialog(onDismiss = viewModel::dismissRestartDialog)
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

    state.pendingDeleteSubscriptionTitle?.let { title ->
        DeleteSubscriptionConfirmDialog(
            title = title,
            onDismiss = viewModel::dismissDeleteSubscriptionConfirm,
            onConfirm = viewModel::confirmDeleteCustomSubscription
        )
    }

    if (state.showDiagnosticsDialog) {
        val diag = state.connectionDiagnostics
        AlertDialog(
            onDismissRequest = viewModel::dismissDiagnosticsDialog,
            title = { Text(stringResource(R.string.diagnostics_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dpScaled())) {
                    diag?.serverLabel?.let {
                        Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(diag?.summary ?: "", style = MaterialTheme.typography.bodyMedium)
                    Text(diag?.suggestion ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDiagnosticsDialog) {
                    Text(stringResource(R.string.diagnostics_close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.selectedListLabelLocalized(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "v${AppConstants.APP_VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TunnelAqua.copy(alpha = 0.85f),
                                modifier = Modifier.padding(start = 6.dpScaled())
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(
                        onClick = viewModel::fullRefreshFromMenu,
                        enabled = !state.isLoading && !state.isUpdating
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_all_subscriptions),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = viewModel::openAppMenu) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = state.isPinging,
            onRefresh = { viewModel.onMainPullRefresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MainScrollContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun MainScrollContent(
    state: MainUiState,
    viewModel: MainViewModel
) {
    when {
        (state.isLoading || state.isUpdating) && state.configs.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.loadingMessage ?: stringResource(R.string.loading_configs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "connection") {
                    ConnectionCard(
                        state = state,
                        onConnect = viewModel::connectSelected,
                        onDisconnect = viewModel::disconnect,
                        onCancelConnect = viewModel::cancelConnect,
                        onChooseConfig = viewModel::openConfigPicker
                    )
                }
                item(key = "pull-hint") {
                    Text(
                        text = stringResource(R.string.pull_refresh_ping),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.statusMessage?.let { msg ->
                    item(key = "status") {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item(key = "subscriptions-row") {
                    SubscriptionsRow(
                        expanded = state.showSubscriptionsExpanded,
                        selectedLabel = state.selectedListLabelLocalized(),
                        allConfigsSelected = state.showAllConfigsSelected && state.listFilter is ListFilter.All,
                        protocolLabel = state.protocolFilterLabelLocalized(),
                        protocolSelected = state.protocolFilter != null,
                        protocolPickerExpanded = state.showProtocolPickerExpanded,
                        onToggleSubscriptions = viewModel::toggleSubscriptions,
                        onToggleProtocol = viewModel::toggleProtocolPicker,
                        onShowAllConfigs = viewModel::showAllConfigs
                    )
                }
                if (state.showProtocolPickerExpanded) {
                    item(key = "protocol-filter") {
                        ProtocolFilterRow(
                            selected = state.protocolFilter,
                            onSelect = viewModel::selectProtocolFilter
                        )
                    }
                }
                if (state.showSubscriptionsExpanded) {
                    item(key = "sources-grid") {
                        SourcesGrid(
                            state = state,
                            onSelectCustom = viewModel::selectCustomSubscription,
                            onRemoveCustom = viewModel::requestDeleteCustomSubscription
                        )
                    }
                }
                val displayedCount = state.countryGroups.sumOf { it.configs.size }
                val cachedCount = state.configs.size
                if (cachedCount > 0 && displayedCount < cachedCount) {
                    item(key = "list-count-hint") {
                        Text(
                            text = stringResource(R.string.list_shown_count, displayedCount, cachedCount),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.errorMessage?.let { err ->
                    item(key = "error") {
                        Text(
                            text = err,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (state.isLoading || state.isUpdating || state.isRecovering || state.isPinging) {
                    item(key = "progress") {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (state.isPinging) {
                            Text(
                                text = state.loadingMessage ?: stringResource(R.string.ping_in_progress),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                configListItems(
                    state = state,
                    onSelect = viewModel::selectConfig,
                    onToggleCountry = viewModel::toggleCountryExpanded
                )
            }
        }
    }
}

@Composable
private fun SubscriptionsRow(
    expanded: Boolean,
    selectedLabel: String,
    allConfigsSelected: Boolean,
    protocolLabel: String,
    protocolSelected: Boolean,
    protocolPickerExpanded: Boolean,
    onToggleSubscriptions: () -> Unit,
    onToggleProtocol: () -> Unit,
    onShowAllConfigs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dpScaled(), vertical = 4.dpScaled()),
        horizontalArrangement = Arrangement.spacedBy(6.dpScaled())
    ) {
        SubscriptionsToggleButton(
            modifier = Modifier.weight(1.1f),
            expanded = expanded,
            selectedLabel = selectedLabel,
            onClick = onToggleSubscriptions
        )
        ProtocolFilterButton(
            modifier = Modifier.weight(0.9f),
            label = protocolLabel,
            selected = protocolSelected,
            expanded = protocolPickerExpanded,
            onClick = onToggleProtocol
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dpScaled()))
                .clickable(onClick = onShowAllConfigs),
            shape = RoundedCornerShape(12.dpScaled()),
            color = if (allConfigsSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
            border = BorderStroke(
                if (allConfigsSelected) 2.dp else 1.dp,
                if (allConfigsSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.filter_all_configs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (allConfigsSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProtocolFilterButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dpScaled()))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dpScaled()),
        color = when {
            expanded -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            selected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            if (selected || expanded) 2.dp else 1.dp,
            if (selected || expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dpScaled(), vertical = 12.dpScaled()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dpScaled())
            )
            Spacer(modifier = Modifier.height(4.dpScaled()))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProtocolFilterRow(
    selected: ConfigProtocol?,
    onSelect: (ConfigProtocol?) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dpScaled(), vertical = 4.dpScaled()),
        horizontalArrangement = Arrangement.spacedBy(6.dpScaled())
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.protocol_all)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        AppConstants.SUPPORTED_PROTOCOLS.forEach { protocol ->
            FilterChip(
                selected = selected == protocol,
                onClick = { onSelect(protocol) },
                label = { Text(protocol.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun SubscriptionsToggleButton(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    selectedLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (expanded) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        },
        border = BorderStroke(
            1.5.dp,
            if (expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tab_subscriptions_short),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (expanded) {
                        stringResource(R.string.filter_pick_list)
                    } else {
                        stringResource(R.string.filter_current, selectedLabel)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    stringResource(R.string.hide)
                } else {
                    stringResource(R.string.show)
                },
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SourcesGrid(
    state: MainUiState,
    onSelectCustom: (String) -> Unit,
    onRemoveCustom: (String) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dpScaled(), vertical = 4.dpScaled()),
        horizontalArrangement = Arrangement.spacedBy(8.dpScaled())
    ) {
        if (state.customSubscriptions.isEmpty()) {
            Text(
                text = stringResource(R.string.no_custom_subscriptions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dpScaled(), vertical = 8.dpScaled())
            )
        }
        state.customSubscriptions.forEach { sub ->
            CustomSourceTile(
                modifier = Modifier.width(148.dpScaled()),
                sub = sub,
                count = state.configs.count { it.customSourceId == sub.id },
                selected = state.listFilter is ListFilter.Custom && state.listFilter.id == sub.id,
                onClick = { onSelectCustom(sub.id) },
                onRemove = { onRemoveCustom(sub.id) }
            )
        }
    }
}

@Composable
private fun SourceTile(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    selected: Boolean,
    traffic: SubscriptionTraffic?,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            if (count > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.servers_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            traffic?.let {
                Spacer(modifier = Modifier.height(6.dp))
                SubscriptionTrafficBar(traffic = it, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CustomSourceTile(
    modifier: Modifier = Modifier,
    sub: CustomSubscription,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = modifier) {
        SourceTile(
            modifier = Modifier.fillMaxWidth(),
            title = sub.title,
            count = count,
            selected = selected,
            traffic = sub.traffic,
            onClick = onClick
        )
        val deleteInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clickable(
                    interactionSource = deleteInteraction,
                    indication = null,
                    onClick = onRemove
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_subscription),
                tint = TunnelDelete,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    state: MainUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCancelConnect: () -> Unit,
    onChooseConfig: () -> Unit
) {
    val statusColor = when {
        state.isConnected -> TunnelConnected
        state.isConnecting || state.isDisconnecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), shape)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(1.5.dp, statusColor.copy(alpha = 0.35f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            state.isDisconnecting -> stringResource(R.string.disconnecting_button)
                            state.isConnecting -> state.connectionButtonLabel
                                ?: stringResource(R.string.connecting_button)
                            state.isConnected -> stringResource(R.string.status_connected_short)
                            else -> stringResource(R.string.status_disconnected_short)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "↓ ${FormatUtil.formatBytes(state.rxBytes)}  ↑ ${FormatUtil.formatBytes(state.txBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(
                        enabled = !state.isLoading,
                        onClick = onChooseConfig
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                state.selectedConfig?.let { config ->
                    Column {
                        Text(
                            text = CountryNames.toDisplay(config.country, state.appLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${config.protocol.displayName} · ${config.displayPing}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } ?: Text(
                    text = stringResource(R.string.config_picker_none_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onChooseConfig,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !state.isLoading
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.config_picker_choose))
            }

            Spacer(modifier = Modifier.height(10.dp))
            val blockingBusy = state.isDisconnecting
            val buttonText = when {
                state.isDisconnecting -> stringResource(R.string.disconnecting_button)
                state.isConnecting -> stringResource(R.string.cancel_connect_button)
                state.isConnected -> stringResource(R.string.disconnect_button)
                else -> stringResource(R.string.connect_button)
            }
            Button(
                onClick = {
                    when {
                        state.isDisconnecting -> return@Button
                        state.isConnecting -> onCancelConnect()
                        state.isConnected -> onDisconnect()
                        else -> onConnect()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.selectedConfig != null &&
                    !state.isLoading &&
                    !blockingBusy &&
                    (state.isConnecting || state.isConnected || state.selectedConfig?.connectSupported == true),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        state.isConnected -> MaterialTheme.colorScheme.error
                        state.isConnecting -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        state.isConnected -> Color.White
                        state.isConnecting -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                when {
                    state.isConnecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontWeight = FontWeight.SemiBold)
                    }
                    blockingBusy -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    else -> {
                        Icon(
                            if (state.isConnected) Icons.Default.Close else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            state.displaySubscriptionTraffic?.let { traffic ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.subscription_traffic),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SubscriptionTrafficBar(traffic = traffic, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.configListItems(
    state: MainUiState,
    onSelect: (VpnConfig) -> Unit,
    onToggleCountry: (String) -> Unit
) {
    if (state.countryGroups.isEmpty()) {
        item(key = "empty") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.listFilter !is ListFilter.All) {
                        stringResource(R.string.empty_subscription_servers)
                    } else {
                        stringResource(R.string.empty_configs_pull)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    state.countryGroups.forEach { group ->
        val expanded = group.country in state.expandedCountries
        item(key = "header-${group.country}") {
            CountryHeader(
                group = group,
                expanded = expanded,
                appLanguage = state.appLanguage,
                onToggle = { onToggleCountry(group.country) }
            )
        }
        if (expanded) {
            items(group.configs.size, key = { group.configs[it].id }) { index ->
                val config = group.configs[index]
                ConfigItem(
                    config = config,
                    selected = state.selectedConfig?.id == config.id,
                    connected = state.connectedConfigId == config.id,
                    customSubscriptions = state.customSubscriptions,
                    onClick = { onSelect(config) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CountryHeader(
    group: com.my.vpn.data.model.CountryGroup,
    expanded: Boolean,
    appLanguage: com.my.vpn.data.model.AppLanguage,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    stringResource(R.string.collapse)
                } else {
                    stringResource(R.string.expand)
                },
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = CountryNames.toDisplay(group.country, appLanguage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "${group.configs.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfigItem(
    config: VpnConfig,
    selected: Boolean,
    connected: Boolean,
    customSubscriptions: List<CustomSubscription>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${config.server}:${config.port}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = config.protocol.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (config.proxyVerified) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = TunnelConnected,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    (config.sourceLabel ?: customSubscriptions
                        .find { it.id == config.customSourceId }?.title)?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (connected) {
                        Text(
                            text = stringResource(R.string.server_active),
                            color = TunnelConnected,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = config.displayPing,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    config.pingMs == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    config.pingMs < 0 -> MaterialTheme.colorScheme.error
                    config.pingMs < 150 -> TunnelConnected
                    config.pingMs <= AppConstants.MAX_PING_MS -> Color(0xFFD4C483)
                    config.pingMs <= 3000 -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun InstructionReminderDialog(
    onOpenHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text(stringResource(R.string.help_required_title)) },
        text = { Text(stringResource(R.string.help_required_body)) },
        confirmButton = {
            TextButton(onClick = onOpenHelp) { Text(stringResource(R.string.open_help)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.understood)) }
        }
    )
}

@Composable
private fun RestartDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_cleared_title)) },
        text = { Text(stringResource(R.string.data_cleared_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.understood)) }
        }
    )
}
