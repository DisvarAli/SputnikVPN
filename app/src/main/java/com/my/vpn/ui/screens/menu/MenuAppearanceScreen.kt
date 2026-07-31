package com.my.vpn.ui.screens.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.vpn.MyVpnApp
import com.my.vpn.R
import com.my.vpn.data.model.AppLanguage
import com.my.vpn.ui.components.MenuScreenScaffold
import com.my.vpn.ui.components.TunnelMenuSectionTitle
import com.my.vpn.ui.theme.AppThemeMode
import com.my.vpn.ui.theme.TunnelAqua
import com.my.vpn.ui.theme.UI_SCALE_LEVEL_DEFAULT
import com.my.vpn.ui.theme.dpScaled
import com.my.vpn.ui.theme.rememberEffectiveUiScale
import com.my.vpn.ui.theme.scaleFactorFromLevel
import com.my.vpn.ui.viewmodel.MainUiState
import com.my.vpn.ui.localizedLabel
import com.my.vpn.ui.viewmodel.MainViewModel

@Composable
fun MenuAppearanceScreen(
    viewModel: MainViewModel,
    @Suppress("UNUSED_PARAMETER") state: MainUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyVpnApp
    val uiScaleLevel by app.appSettingsStorage.uiScaleLevelFlow.collectAsStateWithLifecycle(
        initialValue = UI_SCALE_LEVEL_DEFAULT
    )
    val themeMode by app.appSettingsStorage.themeModeFlow.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.DARK_STANDARD
    )
    val appLanguage by app.appSettingsStorage.appLanguageFlow.collectAsStateWithLifecycle(
        initialValue = AppLanguage.SYSTEM
    )
    var sliderLevel by remember(uiScaleLevel) { mutableFloatStateOf(uiScaleLevel.toFloat()) }
    val effectiveScale = rememberEffectiveUiScale(sliderLevel.toInt())

    MenuScreenScaffold(
        title = stringResource(R.string.section_appearance),
        subtitle = stringResource(R.string.menu_appearance_subtitle),
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
            TunnelMenuSectionTitle(stringResource(R.string.language_section_title))
            Text(
                stringResource(R.string.language_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dpScaled())
            )
            LanguagePicker(selected = appLanguage, onSelect = viewModel::setAppLanguage)
            TunnelMenuSectionTitle(stringResource(R.string.theme_section_title))
            ThemeModePicker(selected = themeMode, onSelect = viewModel::setThemeMode)
            TunnelMenuSectionTitle(stringResource(R.string.ui_scale_title))
            UiScaleSettingsCard(
                level = sliderLevel.toInt(),
                effectiveScale = effectiveScale,
                onLevelChange = { level ->
                    sliderLevel = level.toFloat()
                    viewModel.setUiScaleLevel(level)
                }
            )
            Spacer(modifier = Modifier.height(24.dpScaled()))
        }
    }
}

@Composable
private fun LanguagePicker(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dpScaled())
    ) {
        AppLanguage.entries.forEach { language ->
            val label = when (language) {
                AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                AppLanguage.RUSSIAN -> stringResource(R.string.language_russian)
                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
            }
            val content: @Composable () -> Unit = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dpScaled())
                    )
                    Spacer(modifier = Modifier.size(8.dpScaled()))
                    Text(
                        label,
                        fontWeight = if (selected == language) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            if (selected == language) {
                Button(
                    onClick = { onSelect(language) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dpScaled()),
                    content = { content() }
                )
            } else {
                OutlinedButton(
                    onClick = { onSelect(language) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dpScaled()),
                    content = { content() }
                )
            }
        }
    }
}

@Composable
private fun ThemeModePicker(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dpScaled()),
        horizontalArrangement = Arrangement.spacedBy(6.dpScaled())
    ) {
        AppThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.localizedLabel(), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = when (mode) {
                    AppThemeMode.LIGHT -> {
                        {
                            Icon(
                                Icons.Default.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dpScaled())
                            )
                        }
                    }
                    AppThemeMode.DARK_STANDARD, AppThemeMode.DARK_RED -> {
                        {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dpScaled())
                            )
                        }
                    }
                    AppThemeMode.SYSTEM -> null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun UiScaleSettingsCard(
    level: Int,
    effectiveScale: Float,
    onLevelChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dpScaled()),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dpScaled(),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dpScaled())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dpScaled())
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = TunnelAqua)
                Column {
                    Text(
                        stringResource(R.string.ui_scale_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.ui_scale_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dpScaled()))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1", style = MaterialTheme.typography.labelSmall)
                Text(
                    stringResource(R.string.ui_scale_value, level, (scaleFactorFromLevel(level) * 100).toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TunnelAqua
                )
                Text("5", style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = level.toFloat(),
                onValueChange = { onLevelChange(it.toInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3
            )
            Text(
                stringResource(R.string.ui_scale_effective, (effectiveScale * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
