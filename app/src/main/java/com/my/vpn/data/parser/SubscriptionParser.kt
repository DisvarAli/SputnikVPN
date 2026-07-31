package com.my.vpn.data.parser

import android.util.Base64
import com.google.gson.JsonParser
import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.Base64Util
import com.my.vpn.util.ConfigFilters
import com.my.vpn.util.ShareLinkSupport
import com.my.vpn.util.CountryNames
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object SubscriptionParser {

    private val COUNTRY_PATTERN = Pattern.compile(
        "([\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF])\\s*([^,|]+)"
    )

    private val SCHEMES = AppConstants.SUPPORTED_PROTOCOLS.map { it.scheme }
    private val LINE_SCHEMES = SCHEMES + listOf("hy2")

    fun parseSubscription(raw: String): List<VpnConfig> {
        val lines = decodeSubscription(raw)
        return lines
            .mapNotNull { parseLine(it.trim()) }
            .filter { ConfigFilters.isInSubscriptionList(it) }
    }

    private fun decodeSubscription(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            XrayProfileSubscriptionParser.parseProfiles(trimmed)?.let { return it }
            parseJsonSubscription(trimmed)?.let { return it }
        }
        if (trimmed.contains("://")) {
            return trimmed.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        }
        return try {
            val decoded = Base64Util.decodeSubscription(trimmed)
            if (decoded.startsWith("[") || decoded.startsWith("{")) {
                XrayProfileSubscriptionParser.parseProfiles(decoded)?.let { return it }
                parseJsonSubscription(decoded)?.let { return it }
            }
            decoded.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        } catch (_: Exception) {
            trimmed.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        }
    }

    /** v2rayN / sing-box JSON: массив outbounds или объект с полем outbounds. */
    private fun parseJsonSubscription(json: String): List<String>? = runCatching {
        val root = JsonParser.parseString(json)
        val elements = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject && root.asJsonObject.has("outbounds") ->
                root.asJsonObject.getAsJsonArray("outbounds")
            else -> return@runCatching null
        }
        val links = mutableListOf<String>()
        elements.forEach { el ->
            if (!el.isJsonObject) return@forEach
            val obj = el.asJsonObject
            obj.get("uri")?.asString?.let { if (it.contains("://")) links.add(it) }
            obj.get("link")?.asString?.let { if (it.contains("://")) links.add(it) }
        }
        links.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun parseLine(line: String): VpnConfig? {
        if (line.startsWith("#")) return null
        val normalized = when {
            line.startsWith("hy2://", ignoreCase = true) ->
                "hysteria2://${line.removePrefix("hy2://")}"
            else -> line
        }
        val scheme = LINE_SCHEMES.firstOrNull { normalized.startsWith("$it://", ignoreCase = true) }
            ?: return null
        return when (scheme.lowercase()) {
            "vless" -> parseVless(normalized)
            "vmess" -> parseVmess(normalized)
            "trojan" -> parseTrojan(normalized)
            "ss" -> parseShadowsocks(normalized)
            "hysteria2", "hy2" -> parseHysteria2(normalized)
            else -> null
        }
    }

    private fun parseVless(link: String): VpnConfig? = runCatching {
        val uri = URI(fixUrl(link))
        val host = resolveHost(uri) ?: return@runCatching null
        val remark = decodeRemark(uri.fragment)
        val (country, name) = extractCountryAndName(remark)
        VpnConfig(
            name = name,
            country = country,
            protocol = ConfigProtocol.VLESS,
            shareLink = link,
            server = host,
            port = uri.port.takeIf { it > 0 } ?: 443
        )
    }.getOrNull()

    private fun parseVmess(link: String): VpnConfig? = runCatching {
        val payload = link.removePrefix("vmess://")
        val jsonStr = if (payload.startsWith("{")) {
            payload
        } else {
            String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        }
        val json = JsonParser.parseString(jsonStr).asJsonObject
        val remark = json.get("ps")?.asString ?: json.get("remarks")?.asString ?: "Сервер"
        val (country, name) = extractCountryAndName(remark)
        val host = json.get("add")?.asString?.trim().orEmpty()
        if (host.isBlank()) return@runCatching null
        val port = json.get("port")?.asString?.toIntOrNull()
            ?: json.get("port")?.asInt ?: 443
        val uuid = json.get("id")?.asString?.trim().orEmpty()
        if (uuid.isBlank()) return@runCatching null
        VpnConfig(
            name = name,
            country = country,
            protocol = ConfigProtocol.VMESS,
            shareLink = link,
            server = host,
            port = port
        )
    }.getOrNull()

    private fun parseTrojan(link: String): VpnConfig? = runCatching {
        val uri = URI(fixUrl(link))
        val host = resolveHost(uri) ?: return@runCatching null
        if (uri.userInfo.isNullOrBlank()) return@runCatching null
        val remark = decodeRemark(uri.fragment)
        val (country, name) = extractCountryAndName(remark)
        VpnConfig(
            name = name,
            country = country,
            protocol = ConfigProtocol.TROJAN,
            shareLink = link,
            server = host,
            port = uri.port.takeIf { it > 0 } ?: 443
        )
    }.getOrNull()

    private fun parseShadowsocks(link: String): VpnConfig? = runCatching {
        val body = link.removePrefix("ss://")
        val hashIdx = body.indexOf('#')
        val main = if (hashIdx >= 0) body.substring(0, hashIdx) else body
        val fragment = if (hashIdx >= 0) body.substring(hashIdx + 1) else null

        val decoded = decodeSsMain(main)
        val at = decoded.lastIndexOf('@')
        if (at <= 0) return@runCatching null
        val hostPort = decoded.substring(at + 1)
        val (host, port) = parseHostPort(hostPort) ?: return@runCatching null
        if (host.isBlank()) return@runCatching null

        val remark = decodeRemark(fragment)
        val (country, name) = extractCountryAndName(remark)
        VpnConfig(
            name = name,
            country = country,
            protocol = ConfigProtocol.SHADOWSOCKS,
            shareLink = link,
            server = host,
            port = port
        )
    }.getOrNull()

    private fun decodeSsMain(main: String): String {
        if (main.contains('@')) return main
        val flags = intArrayOf(
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            Base64.NO_WRAP or Base64.NO_PADDING,
            Base64.DEFAULT
        )
        for (flag in flags) {
            val decoded = runCatching {
                String(Base64.decode(main, flag), Charsets.UTF_8)
            }.getOrNull()
            if (!decoded.isNullOrBlank() && decoded.contains('@')) return decoded
        }
        return main
    }

    private fun parseHysteria2(link: String): VpnConfig? = runCatching {
        val uri = URI(fixUrl(link))
        val host = resolveHost(uri) ?: return@runCatching null
        if (ShareLinkSupport.hysteria2Password(link).isBlank()) return@runCatching null
        val remark = decodeRemark(uri.fragment)
        val (country, name) = extractCountryAndName(remark)
        VpnConfig(
            name = name,
            country = country,
            protocol = ConfigProtocol.HYSTERIA2,
            shareLink = link,
            server = host,
            port = uri.port.takeIf { it > 0 } ?: 443
        )
    }.getOrNull()

    private fun resolveHost(uri: URI): String? {
        val host = uri.host?.trim().orEmpty()
        return host.ifBlank { null }
    }

    fun extractCountryAndName(remark: String): Pair<String, String> {
        val cleaned = remark.trim()
        val matcher = COUNTRY_PATTERN.matcher(cleaned)
        if (matcher.find()) {
            val flag = matcher.group(1) ?: ""
            val countryPart = matcher.group(2)?.trim().orEmpty()
            val country = countryPart.split(",").firstOrNull()?.trim().orEmpty()
            val displayCountry = if (flag.isNotBlank() && country.isNotBlank()) "$flag $country" else country.ifBlank { "Другое" }
            return CountryNames.toRussian(displayCountry) to cleaned
        }

        val withoutTags = cleaned.replace(Regex("\\[.*?]"), "").trim()
        val countryGuess = withoutTags.split(",").firstOrNull()?.trim().orEmpty()
        return if (countryGuess.isNotBlank()) {
            CountryNames.toRussian(countryGuess) to cleaned
        } else {
            "Другое" to cleaned.ifBlank { "Сервер" }
        }
    }

    private fun decodeRemark(fragment: String?): String {
        if (fragment.isNullOrBlank()) return "Сервер"
        return URLDecoder.decode(fragment, StandardCharsets.UTF_8)
    }

    private fun parseHostPort(hostPort: String): Pair<String, Int>? {
        val trimmed = hostPort.trim()
        if (trimmed.startsWith("[")) {
            val end = trimmed.indexOf("]:")
            if (end > 0) {
                val host = trimmed.substring(1, end)
                val port = trimmed.substring(end + 2).toIntOrNull() ?: return null
                return host to port
            }
        }
        val colon = trimmed.lastIndexOf(':')
        if (colon <= 0) return null
        val host = trimmed.substring(0, colon).trim()
        val port = trimmed.substring(colon + 1).toIntOrNull() ?: return null
        return host to port
    }

    private fun fixUrl(url: String): String = url.replace(" ", "%20")
}
