package com.my.vpn.util

import com.my.vpn.data.model.PrivacyTimezoneMode
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.vpn.VpnController
import java.util.TimeZone

object PrivacyTimezoneManager {

    @Volatile
    var mode: PrivacyTimezoneMode = PrivacyTimezoneMode.AUTO

    @Volatile
    var activeZoneId: String? = null

    private var savedDefault: TimeZone? = null

    fun applyFromConfig(config: VpnConfig) {
        if (mode == PrivacyTimezoneMode.OFF) {
            restoreDefault()
            return
        }
        val target = CountryTimezoneResolver.resolve(config)
        if (savedDefault == null) {
            savedDefault = TimeZone.getDefault()
        }
        TimeZone.setDefault(target)
        activeZoneId = target.id
    }

    fun applyForVpnState(vpnConnected: Boolean, config: VpnConfig? = null) {
        if (!vpnConnected || mode == PrivacyTimezoneMode.OFF) {
            restoreDefault()
            return
        }
        val cfg = config ?: lastConnectedConfig
        if (cfg != null) {
            applyFromConfig(cfg)
        }
    }

    fun onVpnConnectedChanged(config: VpnConfig? = null) {
        applyForVpnState(VpnController.isConnected, config)
    }

    fun restoreDefault() {
        savedDefault?.let { TimeZone.setDefault(it) }
        savedDefault = null
        activeZoneId = null
        lastConnectedConfig = null
    }

    fun displayLabel(): String? = activeZoneId

    @Volatile
    private var lastConnectedConfig: VpnConfig? = null

    fun rememberConnectedConfig(config: VpnConfig) {
        lastConnectedConfig = config
    }
}
