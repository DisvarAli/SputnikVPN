package com.my.vpn.data.repository

import com.my.vpn.AppConstants
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConfigTestError
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.vpn.BypassProfileHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class ConfigTester {

    private val proxyTesterFull = ProxyConfigTester()
    private val proxyTesterFast = ProxyConfigTester(
        testUrls = AppConstants.CONNECTIVITY_TEST_URLS
    )

    data class TestRunOptions(
        val fast: Boolean = false,
        val maxProxyAfterTcp: Int? = null
    )

    suspend fun testConfigs(
        configs: List<VpnConfig>,
        mode: ConfigTestMode = ConfigTestMode.TCP_THEN_PROXY,
        bypass: ConnectionBypassProfile? = null,
        onProgress: ((String) -> Unit)? = null,
        options: TestRunOptions = TestRunOptions()
    ): List<VpnConfig> = withContext(Dispatchers.IO) {
        val maxTotal = if (options.fast) {
            AppConstants.MAX_CONFIGS_TO_TEST_FAST
        } else {
            AppConstants.MAX_CONFIGS_TO_TEST
        }
        val sample = configs.take(maxTotal)
        if (sample.isEmpty()) return@withContext emptyList()

        onProgress?.invoke("Проверка: ${sample.size} серверов (${mode.label})…")
        when (mode) {
            ConfigTestMode.TCP_ONLY -> tcpTestParallel(sample, options, onProgress)
            ConfigTestMode.PROXY_ONLY -> proxyTestParallel(sample, bypass, options, onProgress)
            ConfigTestMode.TCP_THEN_PROXY -> {
                val afterTcp = tcpTestParallel(sample, options, onProgress)
                val proxyCap = options.maxProxyAfterTcp
                    ?: if (options.fast) {
                        AppConstants.PROXY_TEST_AFTER_TCP_FAST_MAX
                    } else {
                        AppConstants.PROXY_TEST_AFTER_TCP_MAX
                    }
                val tcpOk = afterTcp
                    .filter { it.isAvailable && it.connectSupported }
                    .sortedBy { it.pingMs ?: Long.MAX_VALUE }
                    .take(proxyCap)
                if (tcpOk.isEmpty()) return@withContext afterTcp
                onProgress?.invoke("VPN-тест: ${tcpOk.size} конфигов…")
                val proxyTested = proxyTestParallel(tcpOk, bypass, options) { msg ->
                    onProgress?.invoke(msg)
                }
                mergeTcpAndProxy(afterTcp, proxyTested)
            }
        }
    }

    suspend fun isReachable(
        config: VpnConfig,
        mode: ConfigTestMode = ConfigTestMode.TCP_ONLY,
        bypass: ConnectionBypassProfile? = null
    ): Boolean = withContext(Dispatchers.IO) {
        when (testSingle(config, mode, bypass ?: BypassProfileHolder.profile, TestRunOptions(fast = true))) {
            is TestOutcome.Available -> true
            else -> false
        }
    }

    suspend fun testConfigsWithBypass(
        configs: List<VpnConfig>,
        profile: ConnectionBypassProfile,
        mode: ConfigTestMode = ConfigTestMode.TCP_THEN_PROXY,
        onProgress: ((Int, Int, String) -> Unit)? = null,
        options: TestRunOptions = TestRunOptions()
    ): List<VpnConfig> = withContext(Dispatchers.IO) {
        testConfigs(
            configs = configs,
            mode = mode,
            bypass = profile,
            onProgress = { msg ->
                onProgress?.invoke(0, 0, msg)
            },
            options = options
        )
    }

    private fun mergeTcpAndProxy(
        tcpResults: List<VpnConfig>,
        proxyResults: List<VpnConfig>
    ): List<VpnConfig> {
        val proxyByKey = proxyResults.associateBy { it.dedupeKey }
        return tcpResults.map { cfg ->
            proxyByKey[cfg.dedupeKey] ?: cfg
        }
    }

    private suspend fun tcpTestParallel(
        configs: List<VpnConfig>,
        options: TestRunOptions,
        onProgress: ((String) -> Unit)? = null
    ): List<VpnConfig> = coroutineScope {
        val parallelism = if (options.fast) {
            AppConstants.TCP_PING_PARALLELISM_FAST
        } else {
            AppConstants.TCP_PING_PARALLELISM
        }
        val semaphore = Semaphore(parallelism)
        val total = configs.size
        var done = 0
        val progressLock = Any()

        configs.map { config ->
            async(Dispatchers.IO) {
                val result = semaphore.withPermit { testSingleTcp(config, options) }
                synchronized(progressLock) {
                    done++
                    val step = if (options.fast) 20 else 25
                    if (done == total || done % step == 0) {
                        onProgress?.invoke("TCP: $done/$total…")
                    }
                }
                result
            }
        }.awaitAll()
    }

    private suspend fun proxyTestParallel(
        configs: List<VpnConfig>,
        bypass: ConnectionBypassProfile?,
        options: TestRunOptions,
        onProgress: ((String) -> Unit)? = null
    ): List<VpnConfig> = coroutineScope {
        val profile = bypass ?: BypassProfileHolder.profile
        val parallelism = if (options.fast) {
            AppConstants.PROXY_TEST_PARALLELISM_FAST
        } else {
            AppConstants.PROXY_TEST_PARALLELISM
        }
        val semaphore = Semaphore(parallelism)
        val proxyTester = if (options.fast) proxyTesterFast else proxyTesterFull
        val total = configs.size
        var done = 0
        val progressLock = Any()

        configs.map { config ->
            async(Dispatchers.IO) {
                val result = semaphore.withPermit {
                    testSingleProxy(config, profile, proxyTester, options)
                }
                synchronized(progressLock) {
                    done++
                    if (done == total || done % 5 == 0) {
                        onProgress?.invoke("VPN-тест: $done/$total…")
                    }
                }
                result
            }
        }.awaitAll()
    }

    private fun testSingle(
        config: VpnConfig,
        mode: ConfigTestMode,
        bypass: ConnectionBypassProfile,
        options: TestRunOptions
    ): TestOutcome {
        val proxyTester = if (options.fast) proxyTesterFast else proxyTesterFull
        return when (mode) {
            ConfigTestMode.TCP_ONLY -> {
                val cfg = testSingleTcp(config, options)
                if (cfg.isAvailable) TestOutcome.Available(cfg) else TestOutcome.Unavailable(cfg)
            }
            ConfigTestMode.PROXY_ONLY -> {
                val cfg = testSingleProxy(config, bypass, proxyTester, options)
                if (cfg.isAvailable) TestOutcome.Available(cfg) else TestOutcome.Unavailable(cfg)
            }
            ConfigTestMode.TCP_THEN_PROXY -> {
                val tcp = testSingleTcp(config, options)
                if (!tcp.isAvailable || !tcp.connectSupported) {
                    return if (tcp.isAvailable) TestOutcome.Available(tcp) else TestOutcome.Unavailable(tcp)
                }
                val proxy = testSingleProxy(tcp, bypass, proxyTester, options)
                if (proxy.isAvailable) TestOutcome.Available(proxy) else TestOutcome.Unavailable(proxy)
            }
        }
    }

    private fun testSingleTcp(config: VpnConfig, options: TestRunOptions): VpnConfig {
        val now = System.currentTimeMillis()
        if (!config.connectSupported) {
            return config.copy(
                pingMs = -1L,
                isAvailable = false,
                proxyVerified = false,
                lastTestError = ConfigTestError.UNSUPPORTED,
                lastTestedAtEpochMs = now
            )
        }
        // Hysteria2 — UDP/QUIC: TCP-скрининг бесполезен (как в v2rayNG).
        if (!needsTcpPrecheck(config)) {
            return config.copy(
                tcpPingMs = null,
                pingMs = null,
                isAvailable = true,
                proxyVerified = false,
                lastTestError = ConfigTestError.NONE,
                lastTestedAtEpochMs = now
            )
        }
        val ping = measureTcpPing(config.server, config.port, options)
        return if (ping in 0..AppConstants.MAX_PING_MS) {
            config.copy(
                tcpPingMs = ping,
                pingMs = ping,
                isAvailable = true,
                proxyVerified = false,
                lastTestError = ConfigTestError.NONE,
                lastTestedAtEpochMs = now
            )
        } else {
            config.copy(
                tcpPingMs = ping.takeIf { it >= 0 },
                pingMs = -1L,
                isAvailable = false,
                proxyVerified = false,
                lastTestError = if (ping < 0) ConfigTestError.TCP_TIMEOUT else ConfigTestError.TCP_REFUSED,
                lastTestedAtEpochMs = now
            )
        }
    }

    private fun testSingleProxy(
        config: VpnConfig,
        profile: ConnectionBypassProfile,
        proxyTester: ProxyConfigTester,
        options: TestRunOptions
    ): VpnConfig {
        val now = System.currentTimeMillis()
        if (!config.connectSupported) {
            return config.copy(
                pingMs = -1L,
                isAvailable = false,
                proxyVerified = false,
                lastTestError = ConfigTestError.UNSUPPORTED,
                lastTestedAtEpochMs = now
            )
        }
        // v2rayNG: быстрый TCP pre-check перед measureOutboundDelay (пропускаем UDP/сложные).
        if (needsTcpPrecheck(config)) {
            val tcp = measureTcpPing(
                config.server,
                config.port,
                options.copy(fast = true),
                overrideTimeoutMs = AppConstants.PROXY_TCP_PRECHECK_MS
            )
            if (tcp < 0) {
                val tcpMs = config.tcpPingMs?.takeIf { it in 0..AppConstants.MAX_PING_MS }
                return if (tcpMs != null) {
                    config.copy(
                        pingMs = tcpMs,
                        isAvailable = true,
                        proxyVerified = false,
                        lastTestError = ConfigTestError.TCP_TIMEOUT,
                        lastTestedAtEpochMs = now
                    )
                } else {
                    config.copy(
                        pingMs = -1L,
                        isAvailable = false,
                        proxyVerified = false,
                        lastTestError = ConfigTestError.TCP_TIMEOUT,
                        lastTestedAtEpochMs = now
                    )
                }
            }
        }
        val result = proxyTester.measureProxyDelay(config, profile)
        return if (result.success) {
            config.copy(
                pingMs = result.delayMs.coerceIn(0, AppConstants.MAX_PING_MS),
                isAvailable = true,
                proxyVerified = true,
                lastTestError = ConfigTestError.NONE,
                lastTestedAtEpochMs = now
            )
        } else {
            val tcpMs = config.tcpPingMs?.takeIf { it in 0..AppConstants.MAX_PING_MS }
                ?: config.pingMs?.takeIf { it in 0..AppConstants.MAX_PING_MS }
            if (tcpMs != null) {
                config.copy(
                    pingMs = tcpMs,
                    isAvailable = true,
                    proxyVerified = false,
                    lastTestError = result.error,
                    lastTestedAtEpochMs = now
                )
            } else {
                config.copy(
                    pingMs = -1L,
                    isAvailable = false,
                    proxyVerified = false,
                    lastTestError = result.error,
                    lastTestedAtEpochMs = now
                )
            }
        }
    }

    private fun needsTcpPrecheck(config: VpnConfig): Boolean =
        config.protocol != ConfigProtocol.HYSTERIA2

    private fun measureTcpPing(
        host: String,
        port: Int,
        options: TestRunOptions,
        overrideTimeoutMs: Int? = null
    ): Long {
        val timeout = overrideTimeoutMs ?: if (options.fast) {
            AppConstants.TCP_PING_TIMEOUT_FAST_MS
        } else {
            AppConstants.TCP_PING_TIMEOUT_MS
        }
        // Как v2rayNG SpeedtestManager.socketConnectTime
        var socket: Socket? = null
        val start = System.currentTimeMillis()
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeout)
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        } finally {
            runCatching { socket?.close() }
        }
    }

    private sealed class TestOutcome {
        data class Available(val config: VpnConfig) : TestOutcome()
        data class Unavailable(val config: VpnConfig) : TestOutcome()
    }
}
