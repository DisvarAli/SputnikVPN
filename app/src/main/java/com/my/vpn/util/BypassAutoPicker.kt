package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.DnsBypassMode
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.SniBypassMode
import com.my.vpn.data.model.TtlBypassMode
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.data.repository.ProxyConfigTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Полный перебор обходов: DNS, SNI, DPI, TCP-оптимизация + VPN-тест через Xray.
 */
object BypassAutoPicker {

    data class Progress(val index: Int, val total: Int, val label: String)

    private const val MAX_SNI_PROBE = 24
    private const val MAX_DOH_PROBE = 6
    private const val MAX_DPI_PROBE = 4
    private const val PROBE_YIELD_MS = 8L

    private val tcpOptCandidates = listOf(
        TtlBypassMode.TTL_64 to 64,
        TtlBypassMode.TTL_128 to 128,
        TtlBypassMode.TTL_255 to 255
    )

    suspend fun pickFullAll(
        config: VpnConfig?,
        onProgress: (Progress) -> Unit
    ): ConnectionBypassProfile = withContext(Dispatchers.Default) {
        val dnsSteps = MAX_DOH_PROBE * 2
        val sniSteps = if (config != null) MAX_SNI_PROBE else 0
        val dpiSteps = if (config != null && config.connectSupported) BypassPresets.dpiPresets.size else 0
        val tcpOptSteps = if (config != null && config.connectSupported) tcpOptCandidates.size else tcpOptCandidates.size
        val transportSteps = if (config != null) 2 else 0
        val total = dnsSteps + sniSteps + dpiSteps + tcpOptSteps + transportSteps

        var step = 0
        fun tick(label: String) {
            step++
            onProgress(Progress(step.coerceAtMost(total), total, label))
        }

        val (direct, proxy) = pickDnsWithProgress { p ->
            tick(p.label)
        }
        val sni = config?.let { pickSniWithProgress(it) { p -> tick(p.label) } }
        val dpiId = config?.let { pickDpiWithProgress(it, sni) { p -> tick(p.label) } }
            ?: BypassPresets.dpiPresets.first { it.id == "medium" }.id

        var bestTcpOpt = TtlBypassMode.TTL_128
        var bestTcpOptMs = Long.MAX_VALUE
        if (config != null && config.connectSupported) {
            val baseProfile = buildProfileAfterPick(direct, proxy, sni, dpiId)
            tcpOptCandidates.forEach { (mode, keepAlive) ->
                tick("TCP-опт. keepAlive=$keepAlive")
                val trial = baseProfile.copy(
                    ttlMode = mode,
                    selectedTtl = keepAlive
                )
                delay(PROBE_YIELD_MS)
                val result = ProxyConfigTester.measureProxyDelay(config, trial)
                val ms = if (result.success) result.delayMs else Long.MAX_VALUE
                if (ms < bestTcpOptMs) {
                    bestTcpOptMs = ms
                    bestTcpOpt = mode
                }
            }
            tick("TCP → ${config.server}:${config.port}")
            BypassProber.measureTcp(config.server, config.port)
            tick("UDP → ${config.server}:${config.port}")
            BypassProber.measureUdp(config.server, config.port)
        } else {
            tcpOptCandidates.forEach { (_, value) -> tick("TCP-опт. $value") }
        }

        var profile = buildProfileAfterPick(direct, proxy, sni, dpiId).copy(
            ttlMode = bestTcpOpt,
            selectedTtl = tcpOptCandidates.first { it.first == bestTcpOpt }.second
        )
        if (config != null && config.connectSupported && !testConfigWithProfile(config, profile)) {
            profile = profile.copy(
                sniMode = SniBypassMode.OFF,
                dnsMode = DnsBypassMode.AUTO
            )
            if (!testConfigWithProfile(config, profile)) {
                profile = profile.copy(dpiMode = DpiBypassMode.OFF, dpiPresetId = null)
            }
        }
        profile
    }

    suspend fun pickDnsWithProgress(
        onProgress: (Progress) -> Unit
    ): Pair<String?, String?> = withContext(Dispatchers.Default) {
        val directList = BypassPresets.directDohServers.take(MAX_DOH_PROBE)
        val proxyList = BypassPresets.proxyDohServers.take(MAX_DOH_PROBE)
        val total = directList.size + proxyList.size
        var index = 0
        var bestDirect: Pair<String, Long>? = null
        for (endpoint in directList) {
            index++
            onProgress(Progress(index, total, "DNS direct: ${endpoint.label}"))
            delay(PROBE_YIELD_MS)
            val ms = BypassProber.measureDoh(endpoint.url) ?: continue
            if (bestDirect == null || ms < bestDirect!!.second) {
                bestDirect = endpoint.url to ms
            }
        }
        var bestProxy: Pair<String, Long>? = null
        for (endpoint in proxyList) {
            index++
            onProgress(Progress(index, total, "DNS proxy: ${endpoint.label}"))
            delay(PROBE_YIELD_MS)
            val ms = BypassProber.measureDoh(endpoint.url) ?: continue
            if (bestProxy == null || ms < bestProxy!!.second) {
                bestProxy = endpoint.url to ms
            }
        }
        bestDirect?.first to bestProxy?.first
    }

    suspend fun pickSniWithProgress(
        config: VpnConfig,
        onProgress: (Progress) -> Unit
    ): String? = withContext(Dispatchers.Default) {
        val candidates = BypassPresets.sniCandidates.take(MAX_SNI_PROBE)
        if (config.server.isBlank() || config.port !in 1..65535) return@withContext null
        var best: Pair<String, Long>? = null
        candidates.forEachIndexed { i, sni ->
            onProgress(Progress(i + 1, candidates.size, "SNI: $sni → ${config.server}"))
            delay(PROBE_YIELD_MS)
            val ms = BypassProber.measureTlsSni(config.server, config.port, sni) ?: return@forEachIndexed
            if (best == null || ms < best!!.second) best = sni to ms
        }
        best?.first
    }

    suspend fun pickDpiWithProgress(
        config: VpnConfig,
        baseSni: String?,
        onProgress: (Progress) -> Unit
    ): String? = withContext(Dispatchers.Default) {
        val presets = BypassPresets.dpiPresets.take(MAX_DPI_PROBE)
        if (config.protocol !in setOf(ConfigProtocol.VLESS, ConfigProtocol.VMESS, ConfigProtocol.TROJAN)) {
            return@withContext presets.firstOrNull { it.id == "medium" }?.id
        }
        if (!config.connectSupported) return@withContext "medium"
        val baseBypass = ConnectionBypassProfile(
            sniMode = if (baseSni != null) SniBypassMode.AUTO else SniBypassMode.OFF,
            selectedSni = baseSni,
            dpiMode = DpiBypassMode.OFF
        )
        var bestId: String? = null
        var bestMs = Long.MAX_VALUE
        presets.forEachIndexed { i, preset ->
            onProgress(Progress(i + 1, presets.size, "DPI VPN-тест: ${preset.label}"))
            delay(PROBE_YIELD_MS)
            val result = ProxyConfigTester.measureWithDpiPreset(config, baseBypass, preset)
            if (!result.success) return@forEachIndexed
            if (result.delayMs < bestMs) {
                bestMs = result.delayMs
                bestId = preset.id
                if (result.delayMs <= 400) return@withContext preset.id
            }
        }
        bestId ?: "medium"
    }

    fun testConfigWithProfile(config: VpnConfig, profile: ConnectionBypassProfile): Boolean {
        if (config.server.isBlank() || config.port !in 1..65535) return false
        if (profile.dnsEnabled) {
            val directOk = profile.dnsDirectDoh?.let { BypassProber.measureDoh(it) != null } ?: true
            val proxyOk = profile.dnsProxyDoh?.let { BypassProber.measureDoh(it) != null } ?: true
            if (!directOk && !proxyOk) return false
        }
        if (!config.connectSupported) return false
        return ProxyConfigTester.measureProxyDelay(config, profile).success
    }

    fun buildProfileAfterPick(
        directDoh: String?,
        proxyDoh: String?,
        sni: String?,
        dpiPresetId: String?
    ): ConnectionBypassProfile = ConnectionBypassProfile(
        dnsMode = DnsBypassMode.AUTO,
        dnsDirectDoh = directDoh ?: BypassPresets.directDohServers.first().url,
        dnsProxyDoh = proxyDoh ?: BypassPresets.proxyDohServers.first().url,
        sniMode = if (sni != null) SniBypassMode.AUTO else SniBypassMode.OFF,
        selectedSni = sni,
        splitRuDirect = true,
        dpiMode = if (dpiPresetId != null) DpiBypassMode.MEDIUM else DpiBypassMode.OFF,
        dpiPresetId = dpiPresetId,
        ttlMode = TtlBypassMode.TTL_128
    )
}
