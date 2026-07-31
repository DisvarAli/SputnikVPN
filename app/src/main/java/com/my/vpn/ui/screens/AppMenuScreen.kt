package com.my.vpn.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.my.vpn.AppConstants
import com.my.vpn.R
import com.my.vpn.ui.components.TunnelMenuRow
import com.my.vpn.ui.theme.TunnelAqua
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuScreen(
    viewModel: MainViewModel,
    @Suppress("UNUSED_PARAMETER") state: MainUiState,
    onBack: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.menu_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.menu_subtitle),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dpScaled(), vertical = 8.dpScaled()),
            verticalArrangement = Arrangement.spacedBy(6.dpScaled())
        ) {
            TunnelMenuRow(
                icon = Icons.Default.MenuBook,
                title = stringResource(R.string.help),
                subtitle = stringResource(R.string.help_menu_subtitle),
                onClick = { viewModel.openHelp(fromMenu = true) },
                accent = true
            )
            TunnelMenuRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.section_appearance),
                subtitle = stringResource(R.string.menu_appearance_subtitle),
                onClick = viewModel::openMenuAppearance
            )
            TunnelMenuRow(
                icon = Icons.Default.Subscriptions,
                title = stringResource(R.string.section_subscriptions),
                subtitle = stringResource(R.string.menu_subscriptions_subtitle),
                onClick = viewModel::openMenuSubscriptions
            )
            TunnelMenuRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.section_app_update),
                subtitle = stringResource(R.string.menu_app_update_subtitle),
                onClick = viewModel::openMenuAppUpdate
            )
            TunnelMenuRow(
                icon = Icons.Default.Shield,
                title = stringResource(R.string.section_advanced),
                subtitle = stringResource(R.string.menu_advanced_subtitle),
                onClick = viewModel::openMenuAdvanced
            )

            Spacer(modifier = Modifier.height(8.dpScaled()))
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dpScaled())
            ) {
                Text(stringResource(R.string.exit))
            }
            Spacer(modifier = Modifier.height(16.dpScaled()))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.version_label_full, AppConstants.APP_VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${AppConstants.APP_GITHUB_OWNER}/${AppConstants.APP_GITHUB_REPO}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TunnelAqua.copy(alpha = 0.8f)
                    )
                    Text(
                        stringResource(R.string.inspiration_credit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dpScaled())
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dpScaled()))
        }
    }
}
