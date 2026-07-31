package com.my.vpn.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.my.vpn.R
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.data.model.DnsBypassMode
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.SniBypassMode
import com.my.vpn.data.model.SplitTunnelMode
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.TtlBypassMode
import com.my.vpn.ui.theme.AppThemeMode
import com.my.vpn.ui.viewmodel.ListFilter
import com.my.vpn.ui.viewmodel.MainUiState

@Composable
fun AppThemeMode.localizedLabel(): String = stringResource(
    when (this) {
        AppThemeMode.DARK_STANDARD -> R.string.theme_dark_standard
        AppThemeMode.DARK_RED -> R.string.theme_dark_red
        AppThemeMode.LIGHT -> R.string.theme_light
        AppThemeMode.SYSTEM -> R.string.theme_system
    }
)

@Composable
fun SplitTunnelMode.localizedLabel(): String = stringResource(
    when (this) {
        SplitTunnelMode.FULL_TUNNEL -> R.string.split_tunnel_mode_full
        SplitTunnelMode.SELECTED_APPS -> R.string.split_tunnel_mode_apps
    }
)

@Composable
fun ConfigTestMode.localizedLabel(): String = stringResource(
    when (this) {
        ConfigTestMode.TCP_ONLY -> R.string.config_test_tcp
        ConfigTestMode.TCP_THEN_PROXY -> R.string.config_test_full
        ConfigTestMode.PROXY_ONLY -> R.string.config_test_proxy
    }
)

@Composable
fun DnsBypassMode.localizedLabel(): String = stringResource(
    when (this) {
        DnsBypassMode.OFF -> R.string.bypass_off
        DnsBypassMode.AUTO -> R.string.bypass_auto
        DnsBypassMode.MANUAL -> R.string.bypass_manual
    }
)

@Composable
fun SniBypassMode.localizedLabel(): String = stringResource(
    when (this) {
        SniBypassMode.OFF -> R.string.bypass_off
        SniBypassMode.AUTO -> R.string.bypass_auto
        SniBypassMode.MANUAL -> R.string.bypass_manual
    }
)

@Composable
fun DpiBypassMode.localizedLabel(): String = stringResource(
    when (this) {
        DpiBypassMode.OFF -> R.string.bypass_off
        DpiBypassMode.LIGHT -> R.string.dpi_light
        DpiBypassMode.MEDIUM -> R.string.dpi_medium
        DpiBypassMode.AGGRESSIVE -> R.string.dpi_aggressive
        DpiBypassMode.AUTO -> R.string.bypass_auto
        DpiBypassMode.MANUAL -> R.string.bypass_manual
    }
)

@Composable
fun TtlBypassMode.localizedLabel(): String = stringResource(
    when (this) {
        TtlBypassMode.OFF -> R.string.bypass_off
        TtlBypassMode.AUTO -> R.string.bypass_auto
        TtlBypassMode.TTL_64, TtlBypassMode.TTL_128, TtlBypassMode.TTL_255 -> R.string.bypass_auto
        TtlBypassMode.MANUAL -> R.string.ttl_manual
    }
)

@Composable
fun SubscriptionListType.localizedTitle(): String = stringResource(titleRes)

val SubscriptionListType.titleRes: Int
    @StringRes get() = when (this) {
        SubscriptionListType.BLACK_MOBILE -> R.string.sub_black_mobile
        SubscriptionListType.BLACK_VLESS_FULL -> R.string.sub_black_vless
        SubscriptionListType.BLACK_SS_ALL -> R.string.sub_black_ss
        SubscriptionListType.WHITE_MOBILE_1 -> R.string.sub_white_1
        SubscriptionListType.WHITE_MOBILE_2 -> R.string.sub_white_2
        SubscriptionListType.WHITE_SNI_ALL -> R.string.sub_white_sni
        SubscriptionListType.WHITE_CIDR_ALL -> R.string.sub_white_cidr
        SubscriptionListType.WHITE_CIDR_CHECKED -> R.string.sub_white_cidr_checked
    }

@Composable
fun MainUiState.selectedListLabelLocalized(): String {
    val base = when (val filter = listFilter) {
        ListFilter.All -> stringResource(R.string.filter_all_configs)
        is ListFilter.Builtin -> filter.type.localizedTitle()
        is ListFilter.Custom ->
            customSubscriptions.find { it.id == filter.id }?.title
                ?: stringResource(R.string.filter_custom_subscription)
    }
    val proto = protocolFilter?.displayName
    return if (proto != null) "$base · $proto" else base
}

@Composable
fun MainUiState.protocolFilterLabelLocalized(): String =
    protocolFilter?.displayName ?: stringResource(R.string.filter_protocol)
