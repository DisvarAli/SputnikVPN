package com.my.vpn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkStandardScheme = darkColorScheme(
    primary = StdPrimary,
    onPrimary = Color(0xFF1C1B1F),
    primaryContainer = StdSurfaceVariant,
    onPrimaryContainer = StdPrimaryBright,
    secondary = StdOnSurfaceVariant,
    tertiary = TunnelConnected,
    background = StdBackground,
    onBackground = StdOnBackground,
    surface = StdSurface,
    onSurface = StdOnBackground,
    surfaceVariant = StdSurfaceVariant,
    onSurfaceVariant = StdOnSurfaceVariant,
    outline = TunnelOutline,
    error = TunnelAlert,
    onError = Color.White
)

private val DarkRedScheme = darkColorScheme(
    primary = TunnelAqua,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = TunnelAquaBright,
    secondary = DarkOnSurfaceVariant,
    tertiary = TunnelConnected,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = TunnelAlert,
    onError = Color.White
)

private val LightDayScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF8E0000),
    secondary = LightOnSurfaceVariant,
    tertiary = TunnelConnected,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MyVPNTheme(
    uiScaleLevel: Int = UI_SCALE_LEVEL_DEFAULT,
    themeMode: AppThemeMode = AppThemeMode.DARK_STANDARD,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightDayScheme
        AppThemeMode.DARK_RED -> DarkRedScheme
        AppThemeMode.DARK_STANDARD -> DarkStandardScheme
        AppThemeMode.SYSTEM -> if (systemDark) DarkStandardScheme else LightDayScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val lightBars = themeMode == AppThemeMode.LIGHT ||
                (themeMode == AppThemeMode.SYSTEM && !systemDark)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    ScaledUi(userScaleLevel = uiScaleLevel) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(),
            content = content
        )
    }
}
