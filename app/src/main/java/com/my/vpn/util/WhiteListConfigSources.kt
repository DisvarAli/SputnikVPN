package com.my.vpn.util

/**
 * Файлы белых списков РФ — зеркала igareck/vpn-configs-for-russia (см. SubscriptionSource).
 * Используются для перебора зеркал без VPN.
 */
object WhiteListConfigSources {

    /**
     * Лёгкие файлы первыми — быстрее найти живое зеркало.
     * Полные CIDR/SNI оставляем как запасной вариант.
     */
    val probeFiles: List<String> = listOf(
        "WHITE-SNI-RU-all.txt",
        "BLACK_VLESS_RUS_mobile.txt",
        "Vless-Reality-White-Lists-Rus-Mobile.txt",
        "WHITE-CIDR-RU-checked.txt",
        "Vless-Reality-White-Lists-Rus-Mobile-2.txt",
        "WHITE-CIDR-RU-all.txt"
    )

    val probeLabels: Map<String, String> = mapOf(
        "WHITE-SNI-RU-all.txt" to "Белый SNI",
        "BLACK_VLESS_RUS_mobile.txt" to "Чёрный mobile",
        "Vless-Reality-White-Lists-Rus-Mobile.txt" to "Reality белый #1",
        "WHITE-CIDR-RU-checked.txt" to "Белый CIDR (проверен)",
        "Vless-Reality-White-Lists-Rus-Mobile-2.txt" to "Reality белый #2",
        "WHITE-CIDR-RU-all.txt" to "Белый CIDR (полный)"
    )
}
