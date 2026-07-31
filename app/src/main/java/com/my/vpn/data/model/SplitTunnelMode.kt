package com.my.vpn.data.model

enum class SplitTunnelMode(val label: String) {
    FULL_TUNNEL("Весь трафик через VPN"),
    SELECTED_APPS("Только выбранные приложения")
}
