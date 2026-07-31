package com.my.vpn.util

import com.my.vpn.AppConstants

/**
 * Проверка системного DNS при активном VPN (аудит утечек).
 */
object DnsLeakGuard {

    val VPN_VIRTUAL_DNS: Set<String> = buildSet {
        addAll(AppConstants.VPN_DNS_SERVERS)
        add(AppConstants.VPN_IPV4_ROUTER)
        add(AppConstants.VPN_TUN_DNS)
        add("127.0.0.1")
        add("::1")
        add("0.0.0.0")
    }

    fun isAllowedSystemDns(address: String): Boolean {
        val normalized = address.trim().removePrefix("/")
        if (normalized.isBlank()) return true
        if (normalized in VPN_VIRTUAL_DNS) return true
        if (normalized.startsWith("127.")) return true
        if (normalized.startsWith("10.10.0.")) return true
        if (normalized.startsWith("fe80:") || normalized == "::1") return true
        if (normalized.startsWith("169.254.")) return true
        if (isLocalOrCarrierDns(normalized)) return true
        return false
    }

    /** DNS роутера/LAN/CGNAT в LinkProperties при VPN — не считаем «посторонним». */
    fun isLocalOrCarrierDns(address: String): Boolean {
        val parts = address.split(".")
        if (parts.size != 4) return false
        val octets = parts.mapNotNull { it.toIntOrNull() } 
        if (octets.size != 4) return false
        val (a, b, _, _) = octets
        return when {
            a == 10 && b != 10 -> true
            a == 192 && b == 168 -> true
            a == 172 && b in 16..31 -> true
            a == 100 && b in 64..127 -> true
            else -> false
        }
    }

    fun isLeakDnsWhenVpnOn(address: String): Boolean = !isAllowedSystemDns(address)
}
