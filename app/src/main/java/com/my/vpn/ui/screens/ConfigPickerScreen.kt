package com.my.vpn.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.vpn.R
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.ui.theme.TunnelConnected
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.viewmodel.MainViewModel
import com.my.vpn.util.CountryNames
import com.my.vpn.util.SubscriptionImportUtil

/**
 * Выбор сервера + импорт файла с конфигами/подпиской.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPickerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var protocolFilter by remember { mutableStateOf<ConfigProtocol?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = SubscriptionImportUtil.readTextFromUri(context, uri) ?: return@rememberLauncherForActivityResult
        val title = uriDisplayName(context, uri)
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.config_picker_import_default_title)
        viewModel.addCustomSubscriptionFromImport(
            title,
            SubscriptionImportUtil.extractSubscriptionText(text, context.applicationContext)
        )
    }

    val flatConfigs = remember(state.countryGroups) {
        state.countryGroups.flatMap { it.configs }
    }
    val filtered = remember(flatConfigs, query, protocolFilter, state.appLanguage) {
        flatConfigs.filter { cfg ->
            val protoOk = protocolFilter == null || cfg.protocol == protocolFilter
            val q = query.trim()
            val textOk = q.isEmpty() ||
                cfg.name.contains(q, ignoreCase = true) ||
                cfg.server.contains(q, ignoreCase = true) ||
                CountryNames.toDisplay(cfg.country, state.appLanguage).contains(q, ignoreCase = true) ||
                cfg.protocol.displayName.contains(q, ignoreCase = true)
            protoOk && textOk && cfg.connectSupported
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.config_picker_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.config_picker_subtitle, filtered.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            filePicker.launch(arrayOf("text/*", "application/json", "application/octet-stream", "*/*"))
                        }
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.config_picker_import_file)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dpScaled())
        ) {
            OutlinedButton(
                onClick = {
                    filePicker.launch(arrayOf("text/*", "application/json", "application/octet-stream", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dpScaled())
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dpScaled()))
                Spacer(modifier = Modifier.width(8.dpScaled()))
                Text(stringResource(R.string.config_picker_import_file))
            }
            Spacer(modifier = Modifier.height(10.dpScaled()))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.config_picker_search)) },
                shape = RoundedCornerShape(14.dpScaled())
            )
            Spacer(modifier = Modifier.height(10.dpScaled()))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dpScaled())
            ) {
                FilterChip(
                    selected = protocolFilter == null,
                    onClick = { protocolFilter = null },
                    label = { Text(stringResource(R.string.protocol_all)) }
                )
                listOf(
                    ConfigProtocol.VLESS,
                    ConfigProtocol.VMESS,
                    ConfigProtocol.TROJAN,
                    ConfigProtocol.SHADOWSOCKS,
                    ConfigProtocol.HYSTERIA2
                ).forEach { p ->
                    FilterChip(
                        selected = protocolFilter == p,
                        onClick = {
                            protocolFilter = if (protocolFilter == p) null else p
                        },
                        label = { Text(p.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dpScaled()))
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dpScaled()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.config_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dpScaled()),
                    verticalArrangement = Arrangement.spacedBy(6.dpScaled())
                ) {
                    items(filtered, key = { it.id }) { config ->
                        ConfigPickerRow(
                            config = config,
                            selected = state.selectedConfig?.id == config.id,
                            connected = state.connectedConfigId == config.id,
                            countryLabel = CountryNames.toDisplay(config.country, state.appLanguage),
                            onClick = {
                                viewModel.selectConfig(config)
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun uriDisplayName(context: android.content.Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
    }
    return uri.lastPathSegment
}

@Composable
private fun ConfigPickerRow(
    config: VpnConfig,
    selected: Boolean,
    connected: Boolean,
    countryLabel: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dpScaled())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            )
            .then(
                if (selected) Modifier.border(1.dpScaled(), MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dpScaled(), vertical = 12.dpScaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dpScaled()))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = countryLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = config.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${config.protocol.displayName} · ${config.server}:${config.port} · ${config.displayPing}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (connected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = TunnelConnected,
                modifier = Modifier.size(22.dpScaled())
            )
        } else if (config.proxyVerified) {
            Box(
                modifier = Modifier
                    .size(10.dpScaled())
                    .clip(CircleShape)
                    .background(TunnelConnected)
            )
        }
    }
}
