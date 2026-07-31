package com.my.vpn.data.repository

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConfigTestError
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.DpiFragmentPreset
import com.my.vpn.data.model.VpnConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyConfigTesterTest {

    private val bypass = ConnectionBypassProfile()
    private val sampleConfig = VpnConfig(
        name = "Test",
        country = "DE",
        protocol = ConfigProtocol.VLESS,
        shareLink = "vless://uuid@1.2.3.4:443?security=reality#test",
        server = "1.2.3.4",
        port = 443
    )

    @Test
    fun measureProxyDelay_buildJsonFails_returnsHandshakeError() {
        val tester = ProxyConfigTester(
            buildLatencyJson = { _, _ -> null }
        )
        val result = tester.measureProxyDelay(sampleConfig, bypass)
        assertFalse(result.success)
        assertEquals(ConfigTestError.PROXY_HANDSHAKE, result.error)
    }

    @Test
    fun measureProxyDelay_allUrlsFail_returnsTimeout() {
        val tester = ProxyConfigTester(
            buildLatencyJson = { _, _ -> """{"log":{}}""" },
            measureDelay = { _, _ -> -1L },
            testUrls = listOf("https://a.test/", "https://b.test/")
        )
        val result = tester.measureProxyDelay(sampleConfig, bypass)
        assertFalse(result.success)
        assertEquals(ConfigTestError.PROXY_TIMEOUT, result.error)
    }

    @Test
    fun measureProxyDelay_firstUrlSuccess_returnsDelay() {
        val calls = mutableListOf<String>()
        val tester = ProxyConfigTester(
            buildLatencyJson = { _, _ -> """{"inbounds":[]}""" },
            measureDelay = { _, url ->
                calls.add(url)
                if (url.contains("first")) 120L else -1L
            },
            testUrls = listOf("https://first.test/", "https://second.test/")
        )
        val result = tester.measureProxyDelay(sampleConfig, bypass)
        assertTrue(result.success)
        assertEquals(120L, result.delayMs)
        assertEquals(ConfigTestError.NONE, result.error)
        assertEquals(1, calls.size)
    }

    @Test
    fun measureProxyDelay_triesSecondUrlWhenFirstFails() {
        val tester = ProxyConfigTester(
            buildLatencyJson = { _, _ -> "{}" },
            measureDelay = { _, url ->
                when {
                    url.contains("fail") -> -1L
                    else -> 200L
                }
            },
            testUrls = listOf("https://fail.test/", "https://ok.test/")
        )
        val result = tester.measureProxyDelay(sampleConfig, bypass)
        assertTrue(result.success)
        assertEquals(200L, result.delayMs)
    }

    @Test
    fun measureJson_coercesDelayToMaxPing() {
        val tester = ProxyConfigTester(
            measureDelay = { _, _ -> 9999L },
            testUrls = listOf("https://x.test/"),
            maxPingMs = 750L
        )
        val result = tester.measureJson("{}")
        assertTrue(result.success)
        assertEquals(750L, result.delayMs)
    }

    @Test
    fun measureWithDpiPreset_buildFails_returnsHandshakeError() {
        val tester = ProxyConfigTester(
            buildLatencyJsonWithDpi = { _, _, _ -> null }
        )
        val preset = DpiFragmentPreset("medium", "Средний", "tlshello", "100-200", "10-20")
        val result = tester.measureWithDpiPreset(sampleConfig, bypass, preset)
        assertFalse(result.success)
        assertEquals(ConfigTestError.PROXY_HANDSHAKE, result.error)
    }

    @Test
    fun measureWithDpiPreset_usesDpiBuilder() {
        var dpiCalled = false
        val tester = ProxyConfigTester(
            buildLatencyJson = { _, _ -> error("should not use default builder") },
            buildLatencyJsonWithDpi = { _, _, preset ->
                dpiCalled = preset.id == "light"
                "{}"
            },
            measureDelay = { _, _ -> 50L },
            testUrls = listOf("https://x.test/")
        )
        val preset = DpiFragmentPreset("light", "Лёгкий", "tlshello", "50-100", "5-10")
        val result = tester.measureWithDpiPreset(sampleConfig, bypass, preset)
        assertTrue(dpiCalled)
        assertTrue(result.success)
        assertEquals(50L, result.delayMs)
    }

}
