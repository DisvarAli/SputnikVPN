package com.my.vpn.util

import com.my.vpn.data.model.VpnConfig

object ConfigListProcessor {

    /** Доступные и с лучшим пингом — выше; без пинга — в конце. */
    fun sortByPing(configs: List<VpnConfig>): List<VpnConfig> =
        configs.sortedWith(
            compareBy<VpnConfig> { cfg ->
                when {
                    cfg.proxyVerified -> 0
                    cfg.isAvailable && cfg.pingMs != null && cfg.pingMs >= 0 -> 1
                    cfg.pingMs != null && cfg.pingMs >= 0 -> 2
                    else -> 3
                }
            }.thenBy { it.pingMs ?: Long.MAX_VALUE }
        )
}
