package com.my.vpn.data.parser

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Подписки Happ / sing-box: JSON-массив полных Xray-профилей (как s.ruvps.pro).
 */
object XrayProfileSubscriptionParser {

    fun parseProfiles(raw: String): List<String>? = runCatching {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return@runCatching null
        val arr = JsonParser.parseString(trimmed).asJsonArray
        if (arr.size() == 0) return@runCatching null
        val first = arr[0]
        if (!first.isJsonObject || !first.asJsonObject.has("outbounds")) return@runCatching null

        val links = mutableListOf<String>()
        arr.forEach { el ->
            if (!el.isJsonObject) return@forEach
            buildVlessLink(el.asJsonObject)?.let { links.add(it) }
        }
        links.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun buildVlessLink(profile: JsonObject): String? {
        val remark = profile.get("remarks")?.asString?.trim().orEmpty().ifBlank { "Сервер" }
        val outbound = pickVlessOutbound(profile.getAsJsonArray("outbounds")) ?: return null
        if (outbound.get("protocol")?.asString != "vless") return null

        val vnextArr = outbound.getAsJsonObject("settings")?.getAsJsonArray("vnext") ?: return null
        if (vnextArr.size() == 0) return null
        val vnext = vnextArr[0].asJsonObject
        val host = vnext.get("address")?.asString?.trim().orEmpty()
        if (host.isBlank()) return null
        val port = vnext.get("port")?.asInt ?: 443
        val user = vnext.getAsJsonArray("users")?.firstOrNull()?.asJsonObject ?: return null
        val uuid = user.get("id")?.asString?.trim().orEmpty()
        if (uuid.isBlank()) return null

        val stream = outbound.getAsJsonObject("streamSettings") ?: JsonObject()
        val params = linkedMapOf<String, String>()
        params["encryption"] = user.get("encryption")?.asString ?: "none"
        user.get("flow")?.asString?.takeIf { it.isNotBlank() }?.let { params["flow"] = it }

        val network = stream.get("network")?.asString ?: "tcp"
        params["type"] = network
        when (network) {
            "ws" -> stream.getAsJsonObject("wsSettings")?.let { ws ->
                ws.get("path")?.asString?.let { params["path"] = it }
                val hostHeader = ws.getAsJsonObject("headers")?.get("Host")?.asString
                    ?: ws.get("host")?.asString
                hostHeader?.let { params["host"] = it }
            }
            "grpc" -> stream.getAsJsonObject("grpcSettings")?.let { grpc ->
                grpc.get("serviceName")?.asString?.let { params["serviceName"] = it }
            }
            "xhttp" -> stream.getAsJsonObject("xhttpSettings")?.let { xh ->
                xh.get("path")?.asString?.let { params["path"] = it }
                xh.get("host")?.asString?.takeIf { it.isNotBlank() }?.let { params["host"] = it }
            }
        }

        when (stream.get("security")?.asString) {
            "reality" -> {
                params["security"] = "reality"
                stream.getAsJsonObject("realitySettings")?.let { rs ->
                    rs.get("serverName")?.asString?.let { params["sni"] = it }
                    rs.get("publicKey")?.asString?.let { params["pbk"] = it }
                    rs.get("fingerprint")?.asString?.let { params["fp"] = it }
                    rs.get("shortId")?.asString?.takeIf { it.isNotBlank() }?.let { params["sid"] = it }
                }
            }
            "tls" -> {
                params["security"] = "tls"
                stream.getAsJsonObject("tlsSettings")?.get("serverName")?.asString?.let {
                    params["sni"] = it
                }
                stream.getAsJsonObject("tlsSettings")?.get("fingerprint")?.asString?.let {
                    params["fp"] = it
                }
            }
            else -> params["security"] = "none"
        }

        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }
        val fragment = URLEncoder.encode(remark, StandardCharsets.UTF_8)
        return "vless://$uuid@$host:$port?$query#$fragment"
    }

    private fun pickVlessOutbound(outbounds: JsonArray): JsonObject? {
        val list = outbounds.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            el.asJsonObject
        }
        return list.firstOrNull { it.get("tag")?.asString == "proxy" && it.get("protocol")?.asString == "vless" }
            ?: list.firstOrNull { ob ->
                val proto = ob.get("protocol")?.asString
                val tag = ob.get("tag")?.asString.orEmpty()
                proto == "vless" &&
                    tag !in setOf("direct", "block", "dns-out") &&
                    ob.getAsJsonObject("settings")?.has("vnext") == true
            }
    }

    private fun JsonArray.firstOrNull(): JsonElement? =
        if (size() > 0) get(0) else null
}
