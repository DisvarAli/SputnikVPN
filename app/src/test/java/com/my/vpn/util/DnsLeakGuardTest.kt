package com.my.vpn.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsLeakGuardTest {

    @Test
    fun allowsVpnDns() {
        assertTrue(DnsLeakGuard.isAllowedSystemDns("1.1.1.1"))
        assertTrue(DnsLeakGuard.isAllowedSystemDns("8.8.8.8"))
        assertTrue(DnsLeakGuard.isAllowedSystemDns("10.10.0.1"))
        assertTrue(DnsLeakGuard.isAllowedSystemDns("127.0.0.1"))
    }

    @Test
    fun flagsPublicDnsAsLeakWhenNotVpnDns() {
        assertTrue(DnsLeakGuard.isLeakDnsWhenVpnOn("94.140.14.14"))
        assertFalse(DnsLeakGuard.isLeakDnsWhenVpnOn("1.1.1.1"))
        assertFalse(DnsLeakGuard.isLeakDnsWhenVpnOn("192.168.1.1"))
    }
}
