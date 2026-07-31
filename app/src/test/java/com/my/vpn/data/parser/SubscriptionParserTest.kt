package com.my.vpn.data.parser

import com.my.vpn.data.model.ConfigProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionParserTest {

    @Test
    fun parseVlessLink_extractsServerAndPort() {
        val link = "vless://uuid@1.2.3.4:443?encryption=none&security=reality&sni=example.com#Test"
        val configs = SubscriptionParser.parseSubscription(link)
        assertEquals(1, configs.size)
        val c = configs.first()
        assertEquals(ConfigProtocol.VLESS, c.protocol)
        assertEquals("1.2.3.4", c.server)
        assertEquals(443, c.port)
        assertTrue(c.connectSupported)
    }

    @Test
    fun parseHysteria2_withPassword() {
        val link = "hysteria2://secret@5.6.7.8:443?sni=example.com#Hy2"
        val configs = SubscriptionParser.parseSubscription(link)
        assertEquals(1, configs.size)
        assertEquals(ConfigProtocol.HYSTERIA2, configs.first().protocol)
        assertTrue(configs.first().connectSupported)
    }

    @Test
    fun parseHysteriaV1_ignored() {
        val link = "hysteria://secret@5.6.7.8:443?sni=example.com#Legacy"
        val configs = SubscriptionParser.parseSubscription(link)
        assertTrue(configs.isEmpty())
    }

    @Test
    fun parseBase64Subscription_multipleLines() {
        val body = "vless://a@1.1.1.1:443?encryption=none#One\nvless://b@2.2.2.2:8443?encryption=none#Two"
        val b64 = java.util.Base64.getEncoder().encodeToString(body.toByteArray())
        val configs = SubscriptionParser.parseSubscription(b64)
        assertEquals(2, configs.size)
    }
}
