package com.my.vpn.vpn

import com.my.vpn.data.model.ConnectionBypassProfile

/**
 * Профиль для сборки Xray JSON.
 * Обходы РКН отключены: всегда нейтральный профиль.
 */
object BypassProfileHolder {
    private val neutral = ConnectionBypassProfile(splitRuDirect = false)

    var profile: ConnectionBypassProfile
        get() = neutral
        set(@Suppress("UNUSED_PARAMETER") value) {
            // neutral profile only
        }
}
