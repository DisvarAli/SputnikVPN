package com.my.vpn.util

import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig

object ConfigFilters {

    fun isParsable(config: VpnConfig): Boolean {
        if (!config.protocol.connectViaXray) return false
        if (!ShareLinkSupport.isConnectable(config)) return false
        if (config.server.isBlank()) return false
        if (config.port !in 1..65535) return false
        return true
    }

    /** Мягче, чем [isParsable]: для «Все конфиги» — всё, что распарсили из подписок. */
    fun isInSubscriptionList(config: VpnConfig): Boolean {
        if (config.shareLink.isBlank() || config.server.isBlank()) return false
        if (config.port !in 1..65535) return false
        return config.protocol in AppConstants.SUPPORTED_PROTOCOLS
    }

    /**
     * @param allowPendingPing пока идёт пинг — показываем серверы с «…», без N/A и без проваливших проверку
     * @param strictProxyVerified только проверенные VPN-тестом серверы
     */
    /** VPN-тест (✓); «…» (пинг ещё идёт) не считается подтверждением. */
    fun isTunnelConfirmedForList(config: VpnConfig): Boolean = config.proxyVerified

    /** Прошёл проверку для «строгого списка» (только реальный VPN-тест). */
    fun isStrictVerified(config: VpnConfig): Boolean {
        val pingOk = config.pingMs != null && config.pingMs in 0..AppConstants.MAX_PING_MS
        if (!config.isAvailable || !pingOk) return false
        return config.proxyVerified
    }

    /** Есть приемлемая задержка или проверка ещё не выполнялась. */
    fun hasDisplayableLatency(config: VpnConfig): Boolean {
        if (config.pingMs == null) return true
        if (config.pingMs in 0..AppConstants.MAX_PING_MS && config.isAvailable) return true
        val tcp = config.tcpPingMs
        if (!config.proxyVerified && tcp != null && tcp in 0..AppConstants.MAX_PING_MS) return true
        return false
    }

    fun isListable(
        config: VpnConfig,
        allowPendingPing: Boolean = false,
        strictProxyVerified: Boolean = false,
        showAllWithoutPingFilter: Boolean = false,
        includeUnconfirmedTunnel: Boolean = true
    ): Boolean {
        // В режиме «Все серверы подписки» показываем всё распознанное из подписки, даже если
        // текущая версия не умеет подключаться к конкретной вариации ссылки (connectable=false).
        // В остальных режимах оставляем строгую проверку "подключаемости".
        if (showAllWithoutPingFilter) {
            if (!isInSubscriptionList(config)) return false
        } else {
            if (!isParsable(config)) return false
        }
        if (strictProxyVerified) {
            if (allowPendingPing && config.pingMs == null) return true
            if (!isStrictVerified(config)) return false
        }
        if (!includeUnconfirmedTunnel && config.pingMs != null && !isTunnelConfirmedForList(config)) {
            return false
        }
        if (showAllWithoutPingFilter) return true
        if (allowPendingPing && config.pingMs == null) return true
        return hasDisplayableLatency(config)
    }

    fun filterForDisplay(
        configs: List<VpnConfig>,
        allowPendingPing: Boolean,
        strictProxyVerified: Boolean = false,
        showAllWithoutPingFilter: Boolean = false,
        includeUnconfirmedTunnel: Boolean = true
    ): List<VpnConfig> =
        configs.filter {
            isListable(
                it,
                allowPendingPing,
                strictProxyVerified,
                showAllWithoutPingFilter,
                includeUnconfirmedTunnel
            )
        }
}
