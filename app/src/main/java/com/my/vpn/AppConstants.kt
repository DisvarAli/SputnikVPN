package com.my.vpn

import com.my.vpn.data.model.ConfigProtocol

object AppConstants {
    const val APP_VERSION_NAME = "2.1.0"
    const val CONFIG_INSPIRATION_REPO = "igareck/vpn-configs-for-russia"
    const val CONFIG_INSPIRATION_URL = "https://github.com/igareck/vpn-configs-for-russia"
    const val APP_GITHUB_OWNER = "DisvarAli"
    const val APP_GITHUB_REPO = "SputnikVPN"
    const val APP_UPDATE_POSTPONE_DAYS = 7

    const val MAX_PING_MS = 2_000L
    /** Как v2rayNG SpeedtestManager: короткий TCP handshake. */
    const val TCP_PING_TIMEOUT_MS = 800
    const val TCP_PING_PARALLELISM = 256
    const val TCP_PING_TIMEOUT_FAST_MS = 600
    const val TCP_PING_PARALLELISM_FAST = 192
    /** Real ping через Xray — узкий пул (ядро дорогое), как батчи v2rayNG. */
    const val PROXY_TEST_PARALLELISM = 6
    const val PROXY_TEST_PARALLELISM_FAST = 4
    const val PROXY_TEST_TIMEOUT_MS = 2_000L
    const val MAX_CONFIGS_TO_TEST = 400
    const val MAX_CONFIGS_TO_TEST_FAST = 80
    /** После TCP — только лучшие N на measureOutboundDelay (v2rayNG-style pre-check). */
    const val PROXY_TEST_AFTER_TCP_MAX = 10
    const val PROXY_TEST_AFTER_TCP_FAST_MAX = 6
    /** TCP pre-check перед real ping (v2rayNG RealPingWorkerService: 1000 ms). */
    const val PROXY_TCP_PRECHECK_MS = 1_000

    val CONNECTIVITY_TEST_URLS = listOf(
        "https://www.gstatic.com/generate_204"
    )
    const val CONNECTIVITY_TEST_URL = "https://www.gstatic.com/generate_204"

    const val XRAY_SOCKS_TEST_PORT = 10809
    const val XRAY_SOCKS_VPN_PORT = 10808

    const val CONFIG_UPDATE_INTERVAL_HOURS = 12L
    const val CONFIG_PING_WORK_CHUNK = 100
    const val CONFIG_PING_PROXY_CHUNK = 16
    const val BACKGROUND_PING_WIFI_ONLY_DEFAULT = true
    const val MAX_PER_SOURCE_SAMPLE = 100
    const val MAX_PER_SOURCE_SAMPLE_FAST = 28
    const val MAX_CACHE_CONFIGS = 12_000

    val SUPPORTED_PROTOCOLS = setOf(
        ConfigProtocol.VLESS,
        ConfigProtocol.VMESS,
        ConfigProtocol.TROJAN,
        ConfigProtocol.SHADOWSOCKS,
        ConfigProtocol.HYSTERIA2
    )

    val CONNECT_PROTOCOLS = SUPPORTED_PROTOCOLS.filter { it.connectViaXray }.toSet()

    const val VPN_MTU = 1400
    const val VPN_IPV4_CLIENT = "10.10.0.2"
    const val VPN_IPV4_ROUTER = "10.10.0.1"
    const val VPN_TUN_DNS = VPN_IPV4_ROUTER
    const val DNS_SERVER = "1.1.1.1"
    const val DNS_SERVER_SECONDARY = "8.8.8.8"
    val VPN_DNS_SERVERS = listOf(DNS_SERVER, DNS_SERVER_SECONDARY)

    val DNS_LEAK_PORTS = listOf("53", "853", "5353", "5355")

    val TUNNEL_PROBE_DOMAINS = listOf(
        "connectivitycheck.gstatic.com",
        "www.gstatic.com",
        "clients3.google.com",
        "play.googleapis.com",
        "captive.apple.com",
        "www.msftconnecttest.com",
        "detectportal.firefox.com"
    )

    const val NOTIFICATION_CHANNEL_ID = "sputnik_vpn_channel"
    const val NOTIFICATION_ID = 1

    const val ACTION_VPN_STOP = "com.my.vpn.action.STOP"

    const val VPN_CORE_START_TIMEOUT_MS = 12_000L
    const val VPN_CORE_POLL_INTERVAL_MS = 50L
    const val VPN_CORE_LATE_START_GRACE_MS = 2_500L

    const val TUNNEL_HTTP_TIMEOUT_MS = 1_800
    const val TUNNEL_HTTP_MAX_ATTEMPTS = 2
    const val TUNNEL_HTTP_RETRY_MS = 120L

    const val TUNNEL_CONNECT_HTTP_TIMEOUT_MS = 1_500
    const val TUNNEL_CONNECT_HTTP_MAX_ATTEMPTS = 2
    const val TUNNEL_CONNECT_HTTP_RETRY_MS = 100L
    const val TUNNEL_CONNECT_SOCKS_MAX_ATTEMPTS = 16
    const val TUNNEL_CONNECT_SOCKS_RETRY_MS = 40L

    const val VPN_SERVICE_START_TIMEOUT_MS = 2_800L

    const val HEALTH_CHECK_INTERVAL_MS = 15_000L
    const val HEALTH_CHECK_INTERVAL_MAX_MS = 45_000L
    const val HEALTH_GRACE_AFTER_CONNECT_MS = 3_000L
    const val STABLE_BEFORE_UPDATE_CHECK_MS = 12_000L
    const val CONFIG_RECOVERY_RETRY_MS = 45_000L
}
