package com.my.vpn.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.vpn.R
import com.my.vpn.data.model.DnsBypassMode
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.SniBypassMode
import com.my.vpn.data.model.TtlBypassMode
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.localizedLabel
import com.my.vpn.ui.viewmodel.MainViewModel
import com.my.vpn.util.BypassPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.bypassProfile
    val busy = state.bypassProbing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bypass_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dpScaled()),
            verticalArrangement = Arrangement.spacedBy(10.dpScaled())
        ) {
            Text(
                stringResource(R.string.bypass_dns_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (busy) {
                BypassProbeProgressCard(
                    index = state.bypassProbeIndex,
                    total = state.bypassProbeTotal,
                    detail = state.bypassProbeDetail ?: state.bypassStatusMessage
                )
            }

            Button(
                onClick = viewModel::autoPickFullBypass,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bypass_combo_auto))
            }

            BypassSection(Icons.Default.Dns, stringResource(R.string.bypass_dns_section)) {
                ModeChipRow(busy) {
                    DnsBypassMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.dnsMode == mode,
                            onClick = { if (!busy) viewModel.setDnsBypassMode(mode) },
                            label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                            enabled = !busy
                        )
                    }
                }
                if (profile.dnsMode == DnsBypassMode.MANUAL) {
                    Text(stringResource(R.string.bypass_direct_doh), style = MaterialTheme.typography.labelMedium)
                    DohChipColumn(BypassPresets.directDohServers.map { it.url }, profile.dnsDirectDoh, viewModel::setDnsDirectDoh)
                    Text(stringResource(R.string.bypass_proxy_doh), style = MaterialTheme.typography.labelMedium)
                    DohChipColumn(BypassPresets.proxyDohServers.map { it.url }, profile.dnsProxyDoh, viewModel::setDnsProxyDoh)
                }
                if (profile.dnsMode == DnsBypassMode.AUTO) {
                    SmallButton(stringResource(R.string.bypass_auto_dns), busy) { viewModel.autoPickDnsBypass() }
                }
                ToggleRow(stringResource(R.string.bypass_split_ru), profile.splitRuDirect, busy, viewModel::setSplitRuDirect)
            }

            BypassSection(Icons.Default.Security, stringResource(R.string.bypass_sni_section)) {
                ModeChipRow(busy) {
                    SniBypassMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.sniMode == mode,
                            onClick = { if (!busy) viewModel.setSniBypassMode(mode) },
                            label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                            enabled = !busy
                        )
                    }
                }
                if (profile.sniMode == SniBypassMode.MANUAL) {
                    OutlinedTextField(
                        value = profile.manualSni,
                        onValueChange = viewModel::setManualSni,
                        label = { Text(stringResource(R.string.bypass_manual_sni)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !busy
                    )
                }
                if (profile.sniMode == SniBypassMode.AUTO) {
                    SmallButton(stringResource(R.string.bypass_auto_sni), busy) { viewModel.autoPickSniForSelectedConfig() }
                    profile.selectedSni?.let {
                        Text("SNI: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    stringResource(
                        R.string.bypass_presets_sni,
                        BypassPresets.sniCandidates.take(8).joinToString()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BypassSection(Icons.Default.Speed, stringResource(R.string.bypass_dpi_section)) {
                ModeChipRow(busy) {
                    DpiBypassMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.dpiMode == mode,
                            onClick = { if (!busy) viewModel.setDpiBypassMode(mode) },
                            label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                            enabled = !busy
                        )
                    }
                }
                if (profile.dpiMode != DpiBypassMode.OFF) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dpScaled())
                    ) {
                        BypassPresets.dpiPresets.forEach { preset ->
                            FilterChip(
                                selected = profile.dpiPresetId == preset.id,
                                onClick = { if (!busy) viewModel.setDpiPreset(preset.id) },
                                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                                enabled = !busy
                            )
                        }
                    }
                }
                if (profile.dpiMode == DpiBypassMode.MANUAL) {
                    OutlinedTextField(profile.dpiPackets, viewModel::setDpiPackets, label = { Text("packets") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(profile.dpiLength, viewModel::setDpiLength, label = { Text("length") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(profile.dpiInterval, viewModel::setDpiInterval, label = { Text("interval") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            BypassSection(Icons.Default.Timer, stringResource(R.string.bypass_ttl_section)) {
                ModeChipRow(busy) {
                    TtlBypassMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.ttlMode == mode,
                            onClick = { if (!busy) viewModel.setTtlBypassMode(mode) },
                            label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                            enabled = !busy
                        )
                    }
                }
                if (profile.ttlMode == TtlBypassMode.MANUAL) {
                    OutlinedTextField(
                        value = profile.manualTtl.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.setManualTtl(it) } },
                        label = { Text(stringResource(R.string.bypass_manual_keepalive)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                ToggleRow(stringResource(R.string.bypass_tcp_nodelay), profile.tcpNoDelay, busy, viewModel::setTcpNoDelay)
            }

            BypassSection(Icons.Default.Security, "Mux") {
                ToggleRow(stringResource(R.string.bypass_mux_padding), profile.muxPadding, busy, viewModel::setMuxPadding)
            }

            Text(
                stringResource(R.string.privacy_no_stats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.disclaimer_no_warranty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!busy) {
                state.bypassStatusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Button(onClick = viewModel::saveBypassAndApply, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun BypassProbeProgressCard(index: Int, total: Int, detail: String?) {
    val progress = if (total > 0) index.toFloat() / total.toFloat() else 0f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dpScaled()),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dpScaled()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dpScaled())
        ) {
            Text(
                text = stringResource(R.string.bypass_probe_running),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (total > 0) {
                Text(
                    text = stringResource(R.string.bypass_probe_counter, index, total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dpScaled())
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(40.dpScaled()))
            }
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BypassSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dpScaled())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dpScaled())) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        content()
        Spacer(Modifier.height(4.dpScaled()))
    }
}

@Composable
private fun ModeChipRow(busy: Boolean, chips: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dpScaled()),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        chips()
    }
}

@Composable
private fun DohChipColumn(urls: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dpScaled())) {
        urls.forEach { url ->
            FilterChip(
                selected = selected == url,
                onClick = { onSelect(url) },
                label = { Text(url.removePrefix("https://").take(36), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, busy: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange, enabled = !busy)
    }
}

@Composable
private fun SmallButton(text: String, busy: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(text)
    }
}
