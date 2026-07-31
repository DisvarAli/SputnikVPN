package com.my.vpn.vpn

import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.VpnConfig
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

/**
 * Проверка туннеля: при connect — HTTP через локальный SOCKS (реальный трафик).
 */
object TunnelConnectivityChecker {

    private const val SOCKS_PORT_CHECK_TIMEOUT_MS = 350

    /** Блокирующая проверка при connect: интернет через SOCKS работает. */
    suspend fun awaitTunnelReady(
        port: Int = AppConstants.XRAY_SOCKS_VPN_PORT,
        maxAttempts: Int = AppConstants.TUNNEL_CONNECT_HTTP_MAX_ATTEMPTS,
        intervalMs: Long = AppConstants.TUNNEL_CONNECT_HTTP_RETRY_MS,
        httpTimeoutMs: Int = AppConstants.TUNNEL_CONNECT_HTTP_TIMEOUT_MS
    ): Boolean {
        // Сначала ждём появление локального SOCKS (дешевле, чем делать HTTP на каждом цикле).
        if (!awaitSocksPort(port = port, maxAttempts = 12, intervalMs = 50L)) return false
        return verifyHttpViaSocks(port, maxAttempts, intervalMs, httpTimeoutMs)
    }

    suspend fun awaitSocksPort(
        port: Int = AppConstants.XRAY_SOCKS_VPN_PORT,
        maxAttempts: Int = 12,
        intervalMs: Long = 50L
    ): Boolean {
        repeat(maxAttempts) { attempt ->
            if (isSocksPortOpen(port)) return true
            if (attempt < maxAttempts - 1) delay(intervalMs)
        }
        return false
    }

    /** Быстрый HTTP через SOCKS (фон, health, ручной аудит). */
    suspend fun verifyHttpViaSocks(
        port: Int = AppConstants.XRAY_SOCKS_VPN_PORT,
        maxAttempts: Int = AppConstants.TUNNEL_HTTP_MAX_ATTEMPTS,
        intervalMs: Long = AppConstants.TUNNEL_HTTP_RETRY_MS,
        httpTimeoutMs: Int = AppConstants.TUNNEL_HTTP_TIMEOUT_MS
    ): Boolean {
        repeat(maxAttempts) { attempt ->
            if (isSocksPortOpen(port) && socksHttpProbe(port, httpTimeoutMs)) return true
            if (attempt < maxAttempts - 1) delay(intervalMs)
        }
        return false
    }

    /** @deprecated Используйте [awaitSocksPort] + [verifyHttpViaSocks] в фоне. */
    suspend fun verifyActiveTunnel(
        config: VpnConfig,
        bypass: ConnectionBypassProfile,
        maxAttempts: Int = AppConstants.TUNNEL_HTTP_MAX_ATTEMPTS,
        intervalMs: Long = AppConstants.TUNNEL_HTTP_RETRY_MS
    ): Boolean = verifyHttpViaSocks(socksPortFor(config), maxAttempts, intervalMs)

    fun verifyViaSocksOnly(): Boolean =
        verifyHttpViaSocksBlocking(AppConstants.XRAY_SOCKS_VPN_PORT)

    private fun verifyHttpViaSocksBlocking(port: Int): Boolean {
        if (!isSocksPortOpen(port)) return false
        return socksHttpProbe(port, AppConstants.TUNNEL_HTTP_TIMEOUT_MS)
    }

    private fun socksPortFor(config: VpnConfig): Int = AppConstants.XRAY_SOCKS_VPN_PORT

    fun isSocksPortOpen(port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), SOCKS_PORT_CHECK_TIMEOUT_MS)
        }
        true
    }.getOrDefault(false)

    private fun socksHttpProbe(port: Int, timeoutMs: Int): Boolean {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
        // Несколько URL, чтобы не упираться в единственный домен/блокировку.
        for (url in AppConstants.CONNECTIVITY_TEST_URLS) {
            if (httpProbeThroughProxy(proxy, url, timeoutMs)) return true
        }
        return false
    }

    private fun httpProbeThroughProxy(proxy: Proxy, urlStr: String, timeoutMs: Int): Boolean =
        runCatching {
            val conn = (URL(urlStr).openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Connection", "close")
            }
            try {
                val code = conn.responseCode
                code in 200..399 || code == 204
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
}
