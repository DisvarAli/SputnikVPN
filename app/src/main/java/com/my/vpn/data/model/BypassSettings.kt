package com.my.vpn.data.model

enum class DnsBypassMode(val label: String) {
    OFF("Выкл"),
    AUTO("Авто"),
    MANUAL("Вручную")
}

enum class SniBypassMode(val label: String) {
    OFF("Выкл"),
    AUTO("Авто"),
    MANUAL("Вручную")
}

enum class DpiBypassMode(val label: String) {
    OFF("Выкл"),
    LIGHT("Лёгкий"),
    MEDIUM("Средний"),
    AGGRESSIVE("Сильный"),
    AUTO("Авто"),
    MANUAL("Вручную")
}

enum class TtlBypassMode(val label: String) {
    OFF("Выкл"),
    AUTO("Авто"),
    TTL_64("KeepAlive 64"),
    TTL_128("KeepAlive 128"),
    TTL_255("KeepAlive 255"),
    MANUAL("Вручную")
}

data class DnsDohEndpoint(
    val id: String,
    val label: String,
    val url: String,
    val stream: DnsStream
)

enum class DnsStream { DIRECT, PROXY }

data class DpiFragmentPreset(
    val id: String,
    val label: String,
    val packets: String,
    val length: String,
    val interval: String
)

data class ConnectionBypassProfile(
    val dnsMode: DnsBypassMode = DnsBypassMode.OFF,
    val dnsDirectDoh: String? = null,
    val dnsProxyDoh: String? = null,
    val sniMode: SniBypassMode = SniBypassMode.OFF,
    val manualSni: String = "",
    val selectedSni: String? = null,
    val splitRuDirect: Boolean = false,
    val dpiMode: DpiBypassMode = DpiBypassMode.OFF,
    val dpiPresetId: String? = null,
    val dpiPackets: String = "tlshello",
    val dpiLength: String = "100-200",
    val dpiInterval: String = "10-20",
    val ttlMode: TtlBypassMode = TtlBypassMode.OFF,
    val manualTtl: Int = 64,
    val selectedTtl: Int? = null,
    val muxPadding: Boolean = false,
    val tcpNoDelay: Boolean = true
) {
    val dnsEnabled: Boolean get() = dnsMode != DnsBypassMode.OFF
    val sniEnabled: Boolean get() = sniMode != SniBypassMode.OFF
    val dpiEnabled: Boolean get() = dpiMode != DpiBypassMode.OFF
    val ttlEnabled: Boolean get() = ttlMode != TtlBypassMode.OFF

    fun effectiveSni(original: String?): String? {
        if (!sniEnabled) return original
        return when (sniMode) {
            SniBypassMode.MANUAL -> manualSni.trim().takeIf { it.isNotEmpty() } ?: original
            SniBypassMode.AUTO -> selectedSni?.takeIf { it.isNotEmpty() } ?: original
            SniBypassMode.OFF -> original
        }
    }

    fun effectiveTtl(): Int? {
        if (!ttlEnabled) return null
        return when (ttlMode) {
            TtlBypassMode.TTL_64 -> 64
            TtlBypassMode.TTL_128 -> 128
            TtlBypassMode.TTL_255 -> 255
            TtlBypassMode.MANUAL -> manualTtl.coerceIn(1, 255)
            TtlBypassMode.AUTO -> selectedTtl?.coerceIn(1, 255)
            TtlBypassMode.OFF -> null
        }
    }
}
