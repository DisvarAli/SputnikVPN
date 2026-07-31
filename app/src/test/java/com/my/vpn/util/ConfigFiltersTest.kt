package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.VpnConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigFiltersTest {

    @Test
    fun hasDisplayableLatency_showsTcpWhenProxyFailed() {
        val tcpOnly = VpnConfig(
            name = "Tcp",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = 120L,
            tcpPingMs = 120L,
            isAvailable = true,
            proxyVerified = false
        )
        assertTrue(ConfigFilters.hasDisplayableLatency(tcpOnly))
        val list = ConfigFilters.filterForDisplay(listOf(tcpOnly), allowPendingPing = false)
        assertEquals(1, list.size)
    }

    @Test
    fun showAllWithoutPingFilter_includesFailedPing() {
        val failed = VpnConfig(
            name = "Dead",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = -1L,
            isAvailable = false,
            proxyVerified = false
        )
        val hidden = ConfigFilters.filterForDisplay(listOf(failed), allowPendingPing = false)
        assertTrue(hidden.isEmpty())
        val shown = ConfigFilters.filterForDisplay(
            listOf(failed),
            allowPendingPing = false,
            showAllWithoutPingFilter = true
        )
        assertEquals(1, shown.size)
    }

    @Test
    fun showAllWithoutPingFilter_includesNotConnectable() {
        // HYSTERIA2 без пароля считается "not connectable" в текущей версии,
        // но должна попадать в список при включённом «Все серверы подписки».
        val notConnectable = VpnConfig(
            name = "Hy2",
            country = "DE",
            protocol = ConfigProtocol.HYSTERIA2,
            shareLink = "hysteria2://example.com:443",
            server = "example.com",
            port = 443,
            pingMs = -1L,
            isAvailable = false,
            proxyVerified = false
        )
        val hidden = ConfigFilters.filterForDisplay(listOf(notConnectable), allowPendingPing = false)
        assertTrue(hidden.isEmpty())
        val shown = ConfigFilters.filterForDisplay(
            listOf(notConnectable),
            allowPendingPing = false,
            showAllWithoutPingFilter = true
        )
        assertEquals(1, shown.size)
    }

    @Test
    fun strictListProxyVerified_hidesUnverified() {
        val verified = VpnConfig(
            name = "Ok",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = 100L,
            isAvailable = true,
            proxyVerified = true
        )
        val unverified = verified.copy(id = "2", proxyVerified = false, pingMs = -1L, isAvailable = false)
        val list = ConfigFilters.filterForDisplay(
            listOf(verified, unverified),
            allowPendingPing = false,
            strictProxyVerified = true
        )
        assertEquals(1, list.size)
        assertTrue(list.first().proxyVerified)
    }

    @Test
    fun strictListProxyVerified_hidesTcpOnlyVless() {
        val tcpOnly = VpnConfig(
            name = "Tcp",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = 80L,
            tcpPingMs = 80L,
            isAvailable = true,
            proxyVerified = false
        )
        val list = ConfigFilters.filterForDisplay(
            listOf(tcpOnly),
            allowPendingPing = false,
            strictProxyVerified = true
        )
        assertTrue(list.isEmpty())
    }

    @Test
    fun includeUnconfirmedTunnel_false_hidesTcpOnlyVless() {
        val tcpOnly = VpnConfig(
            name = "Tcp",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = 80L,
            tcpPingMs = 80L,
            isAvailable = true,
            proxyVerified = false
        )
        val list = ConfigFilters.filterForDisplay(
            listOf(tcpOnly),
            allowPendingPing = false,
            includeUnconfirmedTunnel = false
        )
        assertTrue(list.isEmpty())
    }

    @Test
    fun includeUnconfirmedTunnel_false_keepsPendingPing() {
        val pending = VpnConfig(
            name = "Wait",
            country = "DE",
            protocol = ConfigProtocol.VLESS,
            shareLink = "vless://u@1.1.1.1:443#x",
            server = "1.1.1.1",
            port = 443,
            pingMs = null,
            proxyVerified = false
        )
        val list = ConfigFilters.filterForDisplay(
            listOf(pending),
            allowPendingPing = false,
            includeUnconfirmedTunnel = false
        )
        assertEquals(1, list.size)
    }

}
