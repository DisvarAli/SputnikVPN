package com.my.vpn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.my.vpn.R
import com.my.vpn.data.model.CustomSubscription
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.ui.theme.TunnelAqua
import com.my.vpn.ui.theme.TunnelDelete
import com.my.vpn.ui.theme.TunnelAquaBright
import com.my.vpn.ui.theme.TunnelConnected
import com.my.vpn.util.FormatUtil

@Composable
fun TunnelGradientBackdrop(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(TunnelAqua, TunnelAquaBright, TunnelConnected)
                )
            )
    )
}

@Composable
fun SubscriptionTrafficBar(
    traffic: SubscriptionTraffic,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = FormatUtil.formatTrafficLabel(traffic.usedBytes, traffic.totalBytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (traffic.isUnlimited) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                progress = { 1f },
                color = TunnelConnected.copy(alpha = 0.45f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = stringResource(R.string.traffic_unlimited),
                style = MaterialTheme.typography.labelSmall,
                color = TunnelConnected,
                fontWeight = FontWeight.Medium
            )
        } else {
            val fraction = traffic.usageFraction ?: 0f
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                progress = { fraction },
                color = when {
                    fraction > 0.9f -> MaterialTheme.colorScheme.error
                    fraction > 0.7f -> Color(0xFFD4C483)
                    else -> TunnelAqua
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirmUrl: (title: String, url: String) -> Unit,
    onPickFile: (title: String) -> Unit,
    onScanQr: (title: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subscription)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.subscription_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.import_url)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.import_file)) })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.import_qr)) })
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (tab) {
                    0 -> OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.subscription_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    1 -> OutlinedButton(
                        onClick = { onPickFile(title) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                        Text(stringResource(R.string.pick_subscription_file), modifier = Modifier.padding(start = 8.dp))
                    }
                    2 -> OutlinedButton(
                        onClick = { onScanQr(title) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Text(stringResource(R.string.scan_subscription_qr), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (tab == 0) {
                TextButton(onClick = { onConfirmUrl(title, url) }) {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ManageSubscriptionsDialog(
    subscriptions: List<CustomSubscription>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_subscriptions)) },
        text = {
            if (subscriptions.isEmpty()) {
                Text(
                    stringResource(R.string.no_custom_subscriptions),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subscriptions, key = { it.id }) { sub ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = sub.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    if (!sub.traffic.isUnlimited || sub.traffic.totalBytes != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SubscriptionTrafficBar(
                                            traffic = sub.traffic,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                IconButton(onClick = { onDelete(sub.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_subscription),
                                        tint = TunnelDelete
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun DeleteSubscriptionConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = TunnelDelete)
        },
        title = { Text(stringResource(R.string.delete_subscription)) },
        text = {
            Text(stringResource(R.string.delete_subscription_confirm, title))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_subscription), color = TunnelDelete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
