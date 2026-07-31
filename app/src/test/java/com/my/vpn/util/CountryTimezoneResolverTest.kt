package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryTimezoneResolverTest {

    @Test
    fun resolvesGermanyFromCountry() {
        val config = config(country = "🇩🇪 Germany")
        assertEquals("Europe/Berlin", CountryTimezoneResolver.resolveZoneId(config))
    }

    @Test
    fun resolvesRussiaRussianName() {
        val config = config(country = "Россия")
        assertEquals("Europe/Moscow", CountryTimezoneResolver.resolveZoneId(config))
    }

    @Test
    fun fallsBackToServerTld() {
        val config = config(country = "Unknown", server = "node.example.de")
        assertEquals("Europe/Berlin", CountryTimezoneResolver.resolveZoneId(config))
    }

    private fun config(country: String, server: String = "1.2.3.4") = VpnConfig(
        name = "t",
        country = country,
        protocol = ConfigProtocol.VLESS,
        shareLink = "vless://u@$server:443",
        server = server,
        port = 443
    )
}
