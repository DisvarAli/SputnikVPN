package com.my.vpn.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Уровень 3 ≈ прежний 0.75 + ⅓ (1.0) */
const val UI_SCALE_LEVEL_DEFAULT = 3

val LocalUiScale = compositionLocalOf { 1f }

fun scaleFactorFromLevel(level: Int): Float = when (level.coerceIn(1, 5)) {
    1 -> 0.70f
    2 -> 0.85f
    3 -> 1.00f
    4 -> 1.15f
    5 -> 1.30f
    else -> 1.00f
}

@Composable
fun rememberEffectiveUiScale(userLevel: Int): Float {
    val configuration = LocalConfiguration.current
    val base = scaleFactorFromLevel(userLevel)
    val shortest = minOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
    val orientationFactor = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        0.94f
    } else {
        1f
    }
    val screenFactor = when {
        shortest < 360f -> 0.90f
        shortest < 400f -> 0.96f
        shortest > 720f -> 1.05f
        else -> 1f
    }
    return remember(userLevel, configuration.orientation, shortest) {
        (base * orientationFactor * screenFactor).coerceIn(0.62f, 1.45f)
    }
}

@Composable
fun Dp.scaled(): Dp = (value * LocalUiScale.current).dp

@Composable
fun TextUnit.scaled(): TextUnit = (value * LocalUiScale.current).sp

@Composable
fun Int.dpScaled(): Dp = (this * LocalUiScale.current).dp

@Composable
fun Int.spScaled(): TextUnit = (this * LocalUiScale.current).sp

@Composable
fun ScaledUi(
    userScaleLevel: Int,
    content: @Composable () -> Unit
) {
    val scale = rememberEffectiveUiScale(userScaleLevel)
    CompositionLocalProvider(LocalUiScale provides scale) {
        content()
    }
}
