package com.my.vpn.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubscriptionParserHy2Test {

    @Test
    fun parseLine_hy2Alias_mapsToHysteria2() {
        val link = "hy2://secret@5.6.7.8:443?sni=example.com#Hy2"
        val cfg = SubscriptionParser.parseLine(link)
        assertNotNull(cfg)
        assertEquals(com.my.vpn.data.model.ConfigProtocol.HYSTERIA2, cfg!!.protocol)
    }
}
