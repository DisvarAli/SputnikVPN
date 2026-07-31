package com.my.vpn.data.repository

import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConfigTestError
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.DpiFragmentPreset
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.vpn.BypassProfileHolder
import com.my.vpn.vpn.CoreNativeManager
import com.my.vpn.vpn.V2RayConfigBuilder

/**
 * Реальная проверка конфига через Xray [Libv2ray.measureOutboundDelay].
 */
class ProxyConfigTester(
    private val measureDelay: (json: String, url: String) -> Long = { json, url ->
        CoreNativeManager.measureOutboundDelay(json, url)
    },
    private val buildLatencyJson: (VpnConfig, ConnectionBypassProfile) -> String? = { config, bypass ->
        runCatching { V2RayConfigBuilder.buildLatencyTestConfig(config, bypass) }.getOrNull()
    },
    private val buildLatencyJsonWithDpi: (
        VpnConfig,
        ConnectionBypassProfile,
        DpiFragmentPreset
    ) -> String? = { config, bypass, preset ->
        runCatching {
            V2RayConfigBuilder.buildLatencyTestConfigWithDpiPreset(config, bypass, preset)
        }.getOrNull()
    },
    private val testUrls: List<String> = AppConstants.CONNECTIVITY_TEST_URLS,
    private val maxPingMs: Long = AppConstants.MAX_PING_MS
) {

    fun measureProxyDelay(
        config: VpnConfig,
        bypass: ConnectionBypassProfile = BypassProfileHolder.profile
    ): ProxyTestResult {
        if (!config.connectSupported) {
            return ProxyTestResult.failure(ConfigTestError.UNSUPPORTED)
        }
        val json = buildLatencyJson(config, bypass)
            ?: return ProxyTestResult.failure(ConfigTestError.PROXY_HANDSHAKE)
        return measureJson(json)
    }

    fun measureWithDpiPreset(
        config: VpnConfig,
        bypass: ConnectionBypassProfile,
        preset: DpiFragmentPreset
    ): ProxyTestResult {
        val json = buildLatencyJsonWithDpi(config, bypass, preset)
            ?: return ProxyTestResult.failure(ConfigTestError.PROXY_HANDSHAKE)
        return measureJson(json)
    }

    fun measureJson(json: String): ProxyTestResult {
        // Один URL как v2rayNG SettingsManager.getDelayTestUrl() — без каскада таймаутов.
        val url = testUrls.firstOrNull() ?: AppConstants.CONNECTIVITY_TEST_URL
        val raw = runCatching { measureDelay(json, url) }.getOrDefault(-1L)
        if (raw < 0) return ProxyTestResult.failure(ConfigTestError.PROXY_TIMEOUT)
        return ProxyTestResult.success(raw.coerceIn(0, maxPingMs))
    }

    data class ProxyTestResult(
        val delayMs: Long,
        val error: ConfigTestError,
        val success: Boolean
    ) {
        companion object {
            fun success(ms: Long) = ProxyTestResult(ms, ConfigTestError.NONE, true)
            fun failure(err: ConfigTestError) = ProxyTestResult(-1L, err, false)
        }
    }

    companion object {
        /** Экземпляр по умолчанию (нативный Xray). */
        @JvmField
        val Default: ProxyConfigTester = ProxyConfigTester()

        fun measureProxyDelay(
            config: VpnConfig,
            bypass: ConnectionBypassProfile = BypassProfileHolder.profile
        ): ProxyTestResult = Default.measureProxyDelay(config, bypass)

        fun measureWithDpiPreset(
            config: VpnConfig,
            bypass: ConnectionBypassProfile,
            preset: DpiFragmentPreset
        ): ProxyTestResult = Default.measureWithDpiPreset(config, bypass, preset)
    }
}
