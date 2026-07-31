package com.my.vpn.ui.viewmodel

import com.my.vpn.data.model.VpnConfig
import com.my.vpn.vpn.VpnController

/**
 * Координатор VPN-подключения: отделяет логику connect/disconnect от [MainViewModel].
 */
object VpnConnectionCoordinator {

    fun isConnected(): Boolean = VpnController.isConnected

    fun connectedConfigId(): String? = VpnController.connectedConfigId

    fun connect(context: android.content.Context, config: VpnConfig) {
        VpnController.connect(context, config)
    }

    fun disconnect(context: android.content.Context, userRequested: Boolean = true) {
        VpnController.disconnect(context, userRequested)
    }

    fun reconnect(context: android.content.Context, config: VpnConfig) {
        VpnController.reconnect(context, config)
    }
}
