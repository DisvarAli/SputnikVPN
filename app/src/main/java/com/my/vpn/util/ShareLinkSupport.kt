package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ShareLinkSupport {

    fun isConnectable(config: VpnConfig): Boolean {
        if (!config.protocol.connectViaXray) return false
        return when (config.protocol) {
            ConfigProtocol.HYSTERIA2 -> hysteria2Password(config.shareLink).isNotBlank()
            ConfigProtocol.SHADOWSOCKS, ConfigProtocol.VMESS, ConfigProtocol.TROJAN, ConfigProtocol.VLESS -> true
            else -> true
        }
    }

    fun hysteria2Password(link: String): String {
        val uri = runCatching { URI(link.replace(" ", "%20")) }.getOrNull() ?: return ""
        val q = parseQuery(uri)
        return uri.userInfo?.takeIf { it.isNotBlank() }
            ?: q["auth"].orEmpty()
            ?: q["password"].orEmpty()
    }

    fun extractPlugin(link: String): String? {
        val query = link.substringAfter("?", "").substringBefore("#")
        if (query.isBlank()) return null
        return parseQueryFromString(query)["plugin"]
    }

    fun pluginOptions(plugin: String): Map<String, String> {
        val body = plugin.substringAfter("v2ray-plugin").trimStart(';')
        if (body.isEmpty()) return emptyMap()
        return body.split(';')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx > 0) {
                    part.substring(0, idx).trim() to part.substring(idx + 1).trim()
                } else if (part.isNotBlank()) {
                    part.trim() to "true"
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun parseQuery(uri: URI): Map<String, String> =
        parseQueryFromString(uri.rawQuery.orEmpty())

    private fun parseQueryFromString(raw: String): Map<String, String> =
        raw.split("&")
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size == 2) {
                    pieces[0] to URLDecoder.decode(pieces[1], StandardCharsets.UTF_8)
                } else {
                    null
                }
            }
            .toMap()
}
