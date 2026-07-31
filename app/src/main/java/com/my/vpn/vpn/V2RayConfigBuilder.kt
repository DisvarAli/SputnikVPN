package com.my.vpn.vpn

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.DpiFragmentPreset
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.BypassPresets
import com.my.vpn.util.ShareLinkSupport
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2RayConfigBuilder {

    private val gson = Gson()

    fun buildVpnConfig(config: VpnConfig): String {
        return buildVpnConfig(config, BypassProfileHolder.profile)
    }

    fun buildVpnConfig(
        config: VpnConfig,
        bypass: ConnectionBypassProfile
    ): String {
        require(config.connectSupported) { "Протокол ${config.protocol.displayName} пока не поддерживается для подключения" }
        val outbound = when (config.protocol) {
            ConfigProtocol.VLESS -> buildVlessOutbound(parseVless(config.shareLink), bypass)
            ConfigProtocol.VMESS -> buildVmessOutbound(parseVmess(config.shareLink), bypass)
            ConfigProtocol.TROJAN -> buildTrojanOutbound(parseTrojan(config.shareLink), bypass)
            ConfigProtocol.SHADOWSOCKS -> buildShadowsocksOutbound(parseShadowsocks(config.shareLink), bypass)
            ConfigProtocol.HYSTERIA2 -> buildHysteria2Outbound(parseHysteria2(config.shareLink), bypass)
        }
        return gson.toJson(baseTemplate(includeProxy = outbound, bypass = bypass))
    }

    /** Минимальный конфиг для measureOutboundDelay (без TUN). */
    fun buildLatencyTestConfig(config: VpnConfig, bypass: ConnectionBypassProfile): String {
        val outbound = buildOutboundForConfig(config, bypass)
        return gson.toJson(latencyTestTemplate(outbound, bypass))
    }

    fun buildLatencyTestConfigWithDpiPreset(
        config: VpnConfig,
        bypass: ConnectionBypassProfile,
        preset: DpiFragmentPreset
    ): String {
        val withDpi = bypass.copy(
            dpiMode = DpiBypassMode.MANUAL,
            dpiPresetId = preset.id,
            dpiPackets = preset.packets,
            dpiLength = preset.length,
            dpiInterval = preset.interval
        )
        val outbound = buildOutboundForConfig(config, withDpi)
        return gson.toJson(latencyTestTemplate(outbound, withDpi))
    }

    private fun buildOutboundForConfig(config: VpnConfig, bypass: ConnectionBypassProfile): JsonObject {
        require(config.connectSupported) {
            "Протокол ${config.protocol.displayName} не поддерживается для теста"
        }
        return when (config.protocol) {
            ConfigProtocol.VLESS -> buildVlessOutbound(parseVless(config.shareLink), bypass)
            ConfigProtocol.VMESS -> buildVmessOutbound(parseVmess(config.shareLink), bypass)
            ConfigProtocol.TROJAN -> buildTrojanOutbound(parseTrojan(config.shareLink), bypass)
            ConfigProtocol.SHADOWSOCKS -> buildShadowsocksOutbound(parseShadowsocks(config.shareLink), bypass)
            ConfigProtocol.HYSTERIA2 -> buildHysteria2Outbound(parseHysteria2(config.shareLink), bypass)
            else -> error("Тест не поддерживается для ${config.protocol}")
        }
    }

    private fun latencyTestTemplate(includeProxy: JsonObject, bypass: ConnectionBypassProfile): JsonObject {
        val root = JsonObject()
        root.add("log", JsonObject().apply { addProperty("loglevel", "warning") })
        val inbounds = JsonArray()
        inbounds.add(JsonObject().apply {
            addProperty("tag", "socks-in")
            addProperty("port", AppConstants.XRAY_SOCKS_TEST_PORT)
            addProperty("listen", "127.0.0.1")
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply {
                addProperty("auth", "noauth")
                addProperty("udp", true)
            })
        })
        root.add("inbounds", inbounds)
        val outbounds = JsonArray()
        outbounds.add(includeProxy)
        outbounds.add(JsonObject().apply {
            addProperty("protocol", "freedom")
            addProperty("tag", "direct")
        })
        root.add("outbounds", outbounds)
        root.add("routing", JsonObject().apply {
            addProperty("domainStrategy", "AsIs")
            add("rules", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("outboundTag", "proxy")
                    addProperty("network", "tcp,udp")
                })
            })
        })
        root.add("dns", buildDns(bypass))
        return root
    }

    private fun baseTemplate(
        includeProxy: JsonObject,
        bypass: ConnectionBypassProfile,
    ): JsonObject {
        val root = JsonObject()
        root.add("log", JsonObject().apply { addProperty("loglevel", "warning") })
        root.add("stats", JsonObject())

        val inbounds = JsonArray()
        inbounds.add(JsonObject().apply {
            addProperty("tag", "socks-in")
            addProperty("listen", "127.0.0.1")
            addProperty("port", AppConstants.XRAY_SOCKS_VPN_PORT)
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply {
                addProperty("auth", "noauth")
                addProperty("udp", true)
                addProperty("ip", "127.0.0.1")
            })
            add("sniffing", JsonObject().apply {
                addProperty("enabled", true)
                add("destOverride", JsonArray().apply {
                    add("http"); add("tls"); add("quic")
                })
            })
        })
        inbounds.add(JsonObject().apply {
            addProperty("tag", "tun-in")
            addProperty("protocol", "tun")
            add("settings", JsonObject().apply {
                addProperty("name", "xray0")
                addProperty("MTU", AppConstants.VPN_MTU)
            })
            add("sniffing", JsonObject().apply {
                addProperty("enabled", true)
                add("destOverride", JsonArray().apply {
                    add("http"); add("tls"); add("quic")
                })
            })
        })
        root.add("inbounds", inbounds)

        val outbounds = JsonArray()
        outbounds.add(includeProxy)
        outbounds.add(JsonObject().apply {
            addProperty("protocol", "freedom")
            addProperty("tag", "direct")
        })
        outbounds.add(JsonObject().apply {
            addProperty("protocol", "blackhole")
            addProperty("tag", "block")
        })
        root.add("outbounds", outbounds)

        root.add("routing", buildRouting(bypass))
        root.add("dns", buildDns(bypass))
        return root
    }

    private fun buildRouting(bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        addProperty("domainStrategy", "IPIfNonMatch")
        add("rules", JsonArray().apply {
            // DNS/DoT/mDNS всегда через proxy — иначе резолвинг уходит в direct и ломает интернет в TUN.
            AppConstants.DNS_LEAK_PORTS.forEach { port ->
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("port", port)
                    addProperty("network", "tcp,udp")
                    addProperty("outboundTag", "proxy")
                })
            }
            if (bypass.dnsEnabled && bypass.splitRuDirect) {
                add(JsonObject().apply {
                    addProperty("type", "field")
                    add("domain", JsonArray().apply {
                        BypassPresets.ruDomainSuffixes.forEach { add(it) }
                        BypassPresets.ruDirectExtraDomains.forEach { add(it) }
                    })
                    addProperty("outboundTag", "direct")
                })
            }
            add(JsonObject().apply {
                addProperty("type", "field")
                addProperty("outboundTag", "proxy")
                addProperty("network", "tcp,udp")
            })
        })
    }

    private fun buildDns(bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        addProperty("queryStrategy", "UseIPv4")
        addProperty("disableFallback", false)
        addProperty("disableCache", false)
        add("servers", JsonArray().apply {
            if (bypass.dnsEnabled) {
                val direct = bypass.dnsDirectDoh ?: "https://common.dot.dns.yandex.net/dns-query"
                val proxy = bypass.dnsProxyDoh ?: "https://1.1.1.1/dns-query"
                if (bypass.splitRuDirect) {
                    addDnsServer(
                        address = direct,
                        detour = "direct",
                        domains = JsonArray().apply {
                            BypassPresets.ruDomainSuffixes.forEach { add(it) }
                        }
                    )
                }
                addDnsServer(address = proxy, detour = "proxy")
            } else {
                addDnsServer(address = "https://${AppConstants.DNS_SERVER}/dns-query", detour = "proxy")
                addDnsServer(address = "https://${AppConstants.DNS_SERVER_SECONDARY}/dns-query", detour = "proxy")
                addDnsServer(address = AppConstants.DNS_SERVER, detour = "proxy")
                addDnsServer(address = AppConstants.DNS_SERVER_SECONDARY, detour = "proxy")
            }
        })
    }

    private fun JsonArray.addDnsServer(
        address: String,
        detour: String,
        domains: JsonArray? = null
    ) {
        add(JsonObject().apply {
            addProperty("address", address)
            domains?.let { add("domains", it) }
            addProperty("detour", detour)
            addProperty("skipFallback", false)
        })
    }

    private fun applyMux(outbound: JsonObject, bypass: ConnectionBypassProfile) {
        outbound.add("mux", JsonObject().apply {
            addProperty("enabled", bypass.muxPadding)
            if (bypass.muxPadding) {
                addProperty("concurrency", 8)
            }
        })
    }

    private data class VlessParams(
        val uuid: String, val address: String, val port: Int,
        val encryption: String, val flow: String?, val network: String,
        val security: String?, val sni: String?, val fp: String?,
        val pbk: String?, val sid: String?, val spx: String?,
        val host: String?, val path: String?, val serviceName: String?,
        val mode: String?, val xhttpMode: String?, val xhttpExtra: String?,
        val insecure: Boolean
    )

    private data class VmessParams(
        val uuid: String, val address: String, val port: Int,
        val alterId: Int, val network: String, val security: String?,
        val sni: String?, val host: String?, val path: String?, val insecure: Boolean
    )

    private data class TrojanParams(
        val password: String, val address: String, val port: Int,
        val sni: String?, val network: String, val host: String?, val path: String?, val insecure: Boolean
    )

    private data class ShadowsocksParams(
        val method: String,
        val password: String,
        val address: String,
        val port: Int,
        val pluginTransport: PluginTransport? = null
    ) {
        data class PluginTransport(
            val network: String,
            val host: String?,
            val path: String?,
            val sni: String?,
            val insecure: Boolean
        )
    }

    private data class Hysteria2Params(
        val password: String,
        val address: String,
        val port: Int,
        val sni: String?,
        val insecure: Boolean,
        val alpn: List<String>,
        val obfsPassword: String?,
        val portHopping: String?,
        val portHoppingInterval: Int?,
        val pinnedPeerCertSha256: String?
    )

    private fun parseQuery(uri: URI): Map<String, String> =
        uri.rawQuery?.split("&").orEmpty()
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to URLDecoder.decode(parts[1], StandardCharsets.UTF_8) else null
            }.toMap()

    private fun parseVless(link: String): VlessParams {
        val uri = URI(link.replace(" ", "%20"))
        val q = parseQuery(uri)
        val uuid = uri.userInfo?.trim().orEmpty()
        require(uuid.isNotBlank()) { "В ссылке VLESS отсутствует UUID" }
        val address = uri.host?.trim().orEmpty()
        require(address.isNotBlank()) { "В ссылке VLESS не указан хост" }
        return VlessParams(
            uuid = uuid,
            address = address,
            port = uri.port.takeIf { it > 0 } ?: 443,
            encryption = q["encryption"] ?: "none",
            flow = q["flow"]?.takeIf { it.isNotBlank() },
            network = q["type"] ?: "tcp",
            security = q["security"]?.takeIf { it.isNotBlank() && it != "none" },
            sni = q["sni"]?.takeIf { it.isNotBlank() },
            fp = q["fp"]?.takeIf { it.isNotBlank() },
            pbk = q["pbk"]?.takeIf { it.isNotBlank() },
            sid = q["sid"]?.takeIf { it.isNotBlank() },
            spx = q["spx"]?.takeIf { it.isNotBlank() },
            host = q["host"]?.takeIf { it.isNotBlank() },
            path = q["path"]?.takeIf { it.isNotBlank() },
            serviceName = q["serviceName"]?.takeIf { it.isNotBlank() },
            mode = q["mode"]?.takeIf { it.isNotBlank() },
            xhttpMode = q["xhttpMode"]?.takeIf { it.isNotBlank() },
            xhttpExtra = q["extra"]?.takeIf { it.isNotBlank() },
            insecure = q["allowInsecure"] == "1" || q["insecure"] == "1"
        )
    }

    private fun parseVmess(link: String): VmessParams {
        val payload = link.removePrefix("vmess://")
        val jsonStr = if (payload.startsWith("{")) payload
        else String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        val json = JsonParser.parseString(jsonStr).asJsonObject
        return VmessParams(
            uuid = json.get("id")?.asString ?: "",
            address = json.get("add")?.asString ?: "",
            port = json.get("port")?.asString?.toIntOrNull() ?: json.get("port")?.asInt ?: 443,
            alterId = json.get("aid")?.asString?.toIntOrNull() ?: json.get("aid")?.asInt ?: 0,
            network = json.get("net")?.asString ?: "tcp",
            security = json.get("tls")?.asString?.takeIf { it == "tls" },
            sni = json.get("sni")?.asString ?: json.get("host")?.asString,
            host = json.get("host")?.asString,
            path = json.get("path")?.asString,
            insecure = json.get("allowInsecure")?.asString == "1"
        )
    }

    private fun parseTrojan(link: String): TrojanParams {
        val uri = URI(link.replace(" ", "%20"))
        val q = parseQuery(uri)
        return TrojanParams(
            password = uri.userInfo.orEmpty(),
            address = uri.host,
            port = uri.port.takeIf { it > 0 } ?: 443,
            sni = q["sni"]?.takeIf { it.isNotBlank() } ?: uri.host,
            network = q["type"] ?: "tcp",
            host = q["host"],
            path = q["path"],
            insecure = q["allowInsecure"] == "1" || q["insecure"] == "1"
        )
    }

    private fun parseShadowsocks(link: String): ShadowsocksParams {
        val body = link.removePrefix("ss://")
        val hashIdx = body.indexOf('#')
        val main = if (hashIdx >= 0) body.substring(0, hashIdx) else body
        val query = if (main.contains('?')) main.substringAfter('?') else ""
        val mainNoQuery = main.substringBefore('?')

        val decoded = decodeSsCredentials(mainNoQuery)
        val hostPort = decoded.hostPort
        val hostEnd = hostPort.lastIndexOf(':')
        require(hostEnd > 0) { "Invalid SS host:port" }
        val host = parseSsHost(hostPort.substring(0, hostEnd))
        val port = hostPort.substring(hostEnd + 1).toIntOrNull() ?: 8388

        val pluginTransport = ShareLinkSupport.extractPlugin(link)?.let { plugin ->
            val opts = ShareLinkSupport.pluginOptions(plugin)
            when (opts["mode"]) {
                "websocket" -> ShadowsocksParams.PluginTransport(
                    network = "ws",
                    host = opts["host"],
                    path = opts["path"] ?: "/",
                    sni = opts["sni"] ?: opts["host"],
                    insecure = opts["skip-cert-verify"] == "true" || opts["allowInsecure"] == "1"
                )
                else -> null
            }
        }

        return ShadowsocksParams(
            method = decoded.method,
            password = decoded.password,
            address = host,
            port = port,
            pluginTransport = pluginTransport
        )
    }

    private data class SsCredentials(val method: String, val password: String, val hostPort: String)

    private fun decodeSsCredentials(main: String): SsCredentials {
        val at = main.lastIndexOf('@')
        if (at <= 0) throw IllegalArgumentException("Invalid SS URI")
        val userPart = main.substring(0, at)
        val hostPort = main.substring(at + 1)
        val creds = if (userPart.contains(':')) {
            userPart
        } else {
            String(
                Base64.decode(
                    userPart,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                ),
                Charsets.UTF_8
            )
        }
        val colon = creds.indexOf(':')
        if (colon <= 0) throw IllegalArgumentException("Invalid SS credentials")
        return SsCredentials(
            method = creds.substring(0, colon),
            password = creds.substring(colon + 1),
            hostPort = hostPort
        )
    }

    private fun parseSsHost(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return trimmed
    }

    private fun parseHysteria2(link: String): Hysteria2Params {
        val uri = URI(link.replace(" ", "%20"))
        val q = parseQuery(uri)
        val password = ShareLinkSupport.hysteria2Password(link)
        val insecure = q["insecure"] == "1" || q["allowInsecure"] == "1"
        val alpnRaw = q["alpn"]?.takeIf { it.isNotBlank() } ?: "h3"
        return Hysteria2Params(
            password = password,
            address = uri.host,
            port = uri.port.takeIf { it > 0 } ?: 443,
            sni = q["sni"]?.takeIf { it.isNotBlank() } ?: uri.host,
            insecure = insecure,
            alpn = alpnRaw.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            obfsPassword = q["obfs-password"]?.takeIf { it.isNotBlank() }
                ?: q["obfs_password"]?.takeIf { it.isNotBlank() },
            portHopping = q["mport"]?.takeIf { it.isNotBlank() },
            portHoppingInterval = q["mportHopInt"]?.toIntOrNull()?.takeIf { it >= 5 },
            pinnedPeerCertSha256 = q["pinSHA256"]?.takeIf { it.isNotBlank() }
        )
    }

    private fun buildVlessOutbound(params: VlessParams, bypass: ConnectionBypassProfile): JsonObject {
        val sni = bypass.effectiveSni(params.sni)
        val outbound = JsonObject()
        outbound.addProperty("tag", "proxy")
        outbound.addProperty("protocol", "vless")
        val user = JsonObject().apply {
            addProperty("id", params.uuid)
            addProperty("encryption", params.encryption)
            params.flow?.let { addProperty("flow", it) }
        }
        outbound.add("settings", JsonObject().apply {
            add("vnext", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", params.address)
                    addProperty("port", params.port)
                    add("users", JsonArray().apply { add(user) })
                })
            })
        })
        outbound.add("streamSettings", buildStreamSettings(
            params.network, params.security, sni, params.fp,
            params.pbk, params.sid, params.spx, params.host, params.path,
            params.serviceName, params.mode, params.xhttpMode, params.xhttpExtra, params.insecure,
            bypass
        ))
        applyMux(outbound, bypass)
        return outbound
    }

    private fun buildVmessOutbound(params: VmessParams, bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        val sni = bypass.effectiveSni(params.sni)
        addProperty("tag", "proxy")
        addProperty("protocol", "vmess")
        add("settings", JsonObject().apply {
            add("vnext", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", params.address)
                    addProperty("port", params.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", params.uuid)
                            addProperty("alterId", params.alterId)
                            addProperty("security", "auto")
                        })
                    })
                })
            })
        })
        add("streamSettings", buildStreamSettings(
            params.network, params.security, sni, null,
            null, null, null, params.host, params.path,
            null, null, null, null, params.insecure,
            bypass
        ))
        applyMux(this, bypass)
    }

    private fun buildTrojanOutbound(params: TrojanParams, bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        val sni = bypass.effectiveSni(params.sni)
        addProperty("tag", "proxy")
        addProperty("protocol", "trojan")
        add("settings", JsonObject().apply {
            add("servers", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", params.address)
                    addProperty("port", params.port)
                    addProperty("password", params.password)
                })
            })
        })
        add("streamSettings", buildStreamSettings(
            params.network, "tls", sni, null,
            null, null, null, params.host, params.path,
            null, null, null, null, params.insecure,
            bypass
        ))
        applyMux(this, bypass)
    }

    private fun buildShadowsocksOutbound(params: ShadowsocksParams, bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        val sniOverride = bypass.effectiveSni(null)
        addProperty("tag", "proxy")
        addProperty("protocol", "shadowsocks")
        add("settings", JsonObject().apply {
            add("servers", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", params.address)
                    addProperty("port", params.port)
                    addProperty("method", params.method)
                    addProperty("password", params.password)
                })
            })
        })
        params.pluginTransport?.let { transport ->
            add("streamSettings", JsonObject().apply {
                addProperty("network", transport.network)
                if (transport.network == "ws") {
                    add("wsSettings", JsonObject().apply {
                        addProperty("path", transport.path ?: "/")
                        add("headers", JsonObject().apply {
                            transport.host?.let { addProperty("Host", it) }
                        })
                    })
                }
                addProperty("security", "tls")
                add("tlsSettings", buildTlsSettings(
                    sniOverride ?: transport.sni ?: transport.host ?: params.address,
                    transport.insecure,
                    null,
                    bypass
                ))
            })
            applySockoptToStream(this, bypass)
        }
        applyMux(this, bypass)
    }

    /**
     * Xray v26+: Hy2 — protocol [hysteria], transport network [hysteria] (не устаревший hysteria2).
     */
    private fun buildHysteria2Outbound(params: Hysteria2Params, bypass: ConnectionBypassProfile): JsonObject = JsonObject().apply {
        val sni = bypass.effectiveSni(params.sni)
        addProperty("tag", "proxy")
        addProperty("protocol", "hysteria")
        add("settings", JsonObject().apply {
            addProperty("address", params.address)
            addProperty("port", params.port)
            addProperty("version", 2)
        })
        add("streamSettings", JsonObject().apply {
            addProperty("network", "hysteria")
            add("hysteriaSettings", JsonObject().apply {
                addProperty("version", 2)
                addProperty("auth", params.password)
                params.portHopping?.let { hopping ->
                    add("udphop", JsonObject().apply {
                        addProperty("port", hopping)
                        addProperty("interval", params.portHoppingInterval ?: 30)
                    })
                }
            })
            addProperty("security", "tls")
            add("tlsSettings", buildTlsSettings(sni ?: params.address, params.insecure, null, bypass).apply {
                params.pinnedPeerCertSha256?.let { addProperty("pinnedPeerCertSha256", it) }
                add("alpn", JsonArray().apply { params.alpn.forEach { add(it) } })
            })
            applySockoptToStream(this, bypass)
            params.obfsPassword?.let { obfs ->
                add("finalmask", JsonObject().apply {
                    add("udp", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "salamander")
                            add("settings", JsonObject().apply {
                                addProperty("password", obfs)
                            })
                        })
                    })
                })
            }
        })
        applyMux(this, bypass)
    }

    private fun buildStreamSettings(
        network: String, security: String?, sni: String?, fp: String?,
        pbk: String?, sid: String?, spx: String?,
        host: String?, path: String?, serviceName: String?, mode: String?,
        xhttpMode: String?, xhttpExtra: String?,
        insecure: Boolean,
        bypass: ConnectionBypassProfile
    ): JsonObject {
        val stream = JsonObject()
        stream.addProperty("network", network)
        when (network) {
            "ws" -> stream.add("wsSettings", JsonObject().apply {
                path?.let { addProperty("path", it) }
                host?.let { addProperty("host", it) }
            })
            "httpupgrade" -> stream.add("httpupgradeSettings", JsonObject().apply {
                host?.let { addProperty("host", it) }
                addProperty("path", path ?: "/")
            })
            "xhttp" -> stream.add("xhttpSettings", JsonObject().apply {
                host?.let { addProperty("host", it) }
                addProperty("path", path ?: "/")
                xhttpMode?.let { addProperty("mode", it) }
                xhttpExtra?.let { extra ->
                    runCatching { JsonParser.parseString(extra) }.getOrNull()?.let { add("extra", it) }
                }
            })
            "grpc" -> stream.add("grpcSettings", JsonObject().apply {
                serviceName?.let { addProperty("serviceName", it) }
                mode?.let { addProperty("multiMode", it == "multi") }
            })
            "tcp" -> if (!host.isNullOrBlank() || !path.isNullOrBlank()) {
                stream.add("tcpSettings", JsonObject().apply {
                    add("header", JsonObject().apply {
                        addProperty("type", "http")
                        add("request", JsonObject().apply {
                            host?.let { h ->
                                add("headers", JsonObject().apply {
                                    add("Host", JsonArray().apply {
                                        h.split(",").forEach { part ->
                                            val t = part.trim()
                                            if (t.isNotEmpty()) add(t)
                                        }
                                    })
                                })
                            }
                            path?.let { p ->
                                add("path", JsonArray().apply {
                                    p.split(",").forEach { part ->
                                        val t = part.trim()
                                        if (t.isNotEmpty()) add(t)
                                    }
                                })
                            }
                        })
                    })
                })
            }
        }
        when (security) {
            "tls" -> {
                stream.addProperty("security", "tls")
                stream.add("tlsSettings", buildTlsSettings(sni, insecure, fp, bypass))
            }
            "reality" -> {
                stream.addProperty("security", "reality")
                stream.add("realitySettings", JsonObject().apply {
                    sni?.let { addProperty("serverName", it) }
                    fp?.let { addProperty("fingerprint", it) }
                    pbk?.let { addProperty("publicKey", it) }
                    sid?.let { addProperty("shortId", it) }
                    spx?.let { addProperty("spiderX", it) }
                })
            }
        }
        applySockoptToStream(stream, bypass)
        return stream
    }

    private fun buildTlsSettings(
        sni: String?,
        insecure: Boolean,
        fp: String?,
        bypass: ConnectionBypassProfile
    ): JsonObject = JsonObject().apply {
        sni?.let { addProperty("serverName", it) }
        fp?.let { addProperty("fingerprint", it) }
        addProperty("allowInsecure", insecure)
        BypassPresets.effectiveDpi(bypass)?.let { preset ->
            add("fragment", JsonObject().apply {
                addProperty("packets", preset.packets)
                addProperty("length", preset.length)
                addProperty("interval", preset.interval)
            })
        }
    }

    private fun applySockoptToStream(stream: JsonObject, bypass: ConnectionBypassProfile) {
        if (!bypass.ttlEnabled && !bypass.tcpNoDelay) return
        stream.add("sockopt", JsonObject().apply {
            if (bypass.tcpNoDelay) addProperty("tcpNoDelay", true)
            if (bypass.ttlEnabled) {
                addProperty(
                    "tcpKeepAliveIdle",
                    bypass.effectiveTtl()?.coerceIn(30, 600) ?: 128
                )
            }
        })
    }

}
