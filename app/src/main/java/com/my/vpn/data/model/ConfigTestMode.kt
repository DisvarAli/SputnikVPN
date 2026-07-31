package com.my.vpn.data.model

/** Режим проверки доступности серверов в списке. */
enum class ConfigTestMode(val label: String) {
    /** Только TCP до порта (быстро). */
    TCP_ONLY("Быстрый (TCP)"),
    /** TCP, затем тест через Xray (рекомендуется). */
    TCP_THEN_PROXY("Полный (TCP + VPN-тест)"),
    /** Сразу VPN-тест без TCP-скрининга. */
    PROXY_ONLY("Только VPN-тест")
}

enum class ConfigTestError {
    NONE,
    TCP_TIMEOUT,
    TCP_REFUSED,
    PROXY_TIMEOUT,
    PROXY_HANDSHAKE,
    PROXY_REJECTED,
    UNSUPPORTED,
    UNKNOWN
}
