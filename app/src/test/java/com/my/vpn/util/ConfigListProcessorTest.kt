package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigListProcessorTest {

    @Test
    fun sortByPing_ordersAscending() {
        val configs = listOf(
            cfg("b", ping = 200),
            cfg("a", ping = 50),
            cfg("c", ping = null)
        )
        val sorted = ConfigListProcessor.sortByPing(configs)
        assertEquals("a", sorted[0].name)
        assertEquals("b", sorted[1].name)
        assertEquals("c", sorted[2].name)
    }

    private fun cfg(name: String, country: String = "RU", ping: Long? = 100L) = VpnConfig(
        name = name,
        country = country,
        protocol = ConfigProtocol.VLESS,
        shareLink = "vless://u@1.1.1.1:443",
        server = "1.1.1.1",
        port = 443,
        pingMs = ping
    )
}
