package com.my.vpn.domain

import android.content.Context
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.ConnectLog
import com.my.vpn.vpn.BypassProfileHolder
import com.my.vpn.vpn.TunnelConnectivityChecker
import com.my.vpn.vpn.V2RayConfigBuilder
import com.my.vpn.vpn.VpnCoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Проверка готовности VPN после старта сервиса и ядра Xray.
 */
class ConnectVpnUseCase {

    suspend fun validateConfigBuild(context: Context, config: VpnConfig): String? =
        withContext(Dispatchers.Default) {
            runCatching {
                V2RayConfigBuilder.buildVpnConfig(config, BypassProfileHolder.profile)
            }.exceptionOrNull()?.message
        }

    /** Быстрая проверка при connect: локальный SOCKS поднялся (без HTTP). */
    suspend fun verifyTunnelSocksUp(config: VpnConfig): Boolean = withContext(Dispatchers.IO) {
        val port = socksPortFor(config)
        TunnelConnectivityChecker.awaitSocksPort(
            port = port,
            maxAttempts = com.my.vpn.AppConstants.TUNNEL_CONNECT_SOCKS_MAX_ATTEMPTS,
            intervalMs = com.my.vpn.AppConstants.TUNNEL_CONNECT_SOCKS_RETRY_MS
        )
    }

    /** HTTP через SOCKS — в фоне после «Подключено» (health monitor). */
    suspend fun verifyTunnelHttp(config: VpnConfig): Boolean = withContext(Dispatchers.IO) {
        TunnelConnectivityChecker.verifyHttpViaSocks(socksPortFor(config))
    }

    private fun socksPortFor(config: VpnConfig): Int = com.my.vpn.AppConstants.XRAY_SOCKS_VPN_PORT

    suspend fun awaitLateCoreReady(): Boolean = withContext(Dispatchers.IO) {
        VpnCoreManager.awaitCoreRunning(
            timeoutMs = com.my.vpn.AppConstants.VPN_CORE_LATE_START_GRACE_MS,
            lateGraceMs = 0L
        )
    }

    fun logConnectStart(config: VpnConfig) {
        ConnectLog.stage(
            "connect",
            "${config.protocol.displayName} ${config.server}:${config.port}"
        )
    }
}
