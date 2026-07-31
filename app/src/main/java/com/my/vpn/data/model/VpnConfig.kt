package com.my.vpn.data.model

import java.util.UUID

enum class ConfigProtocol(val scheme: String, val connectViaXray: Boolean) {
    VLESS("vless", true),
    VMESS("vmess", true),
    TROJAN("trojan", true),
    SHADOWSOCKS("ss", true),
    HYSTERIA2("hysteria2", true);

    val displayName: String
        get() = when (this) {
            SHADOWSOCKS -> "SS"
            HYSTERIA2 -> "Hy2"
            else -> name
        }
}

data class VpnConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val country: String,
    val protocol: ConfigProtocol,
    val shareLink: String,
    val server: String,
    val port: Int,
    val pingMs: Long? = null,
    val isAvailable: Boolean = true,
    val source: SubscriptionListType? = null,
    val customSourceId: String? = null,
    /** Задержка TCP-скрининга (мс), если выполнялся. */
    val tcpPingMs: Long? = null,
    /** Успешный VPN-тест через Xray. */
    val proxyVerified: Boolean = false,
    val lastTestError: ConfigTestError = ConfigTestError.NONE,
    val lastTestedAtEpochMs: Long = 0L
) {
    val connectSupported: Boolean
        get() = protocol.connectViaXray && com.my.vpn.util.ShareLinkSupport.isConnectable(this)

    val dedupeKey: String
        get() = "$protocol|$server|$port|${extractIdentity()}"

    private fun extractIdentity(): String {
        return when (protocol) {
            ConfigProtocol.VLESS ->
                shareLink.substringAfter("://").substringBefore("@")
            ConfigProtocol.VMESS -> {
                val payload = shareLink.substringAfter("vmess://")
                if (payload.startsWith("{")) payload.hashCode().toString() else payload.take(32)
            }
            ConfigProtocol.TROJAN, ConfigProtocol.SHADOWSOCKS, ConfigProtocol.HYSTERIA2 ->
                shareLink.substringAfter("://").substringBefore("@").ifBlank { shareLink.hashCode().toString() }
        }
    }

    val displayPing: String
        get() = when {
            pingMs == null -> "…"
            pingMs == -1L -> "N/A"
            proxyVerified -> "${pingMs} ms VPN"
            pingMs != null && pingMs >= 0 -> "${pingMs} ms TCP"
            else -> "${pingMs} ms"
        }

    val sourceLabel: String?
        get() = source?.title
}

data class CountryGroup(
    val country: String,
    val configs: List<VpnConfig>
)
