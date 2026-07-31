package com.my.vpn.util

import com.my.vpn.data.model.DnsDohEndpoint
import com.my.vpn.data.model.DnsStream
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.DpiFragmentPreset

object BypassPresets {

    val directDohServers: List<DnsDohEndpoint> = listOf(
        DnsDohEndpoint("yandex", "Yandex DOT", "https://common.dot.dns.yandex.net/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("yandex_alt", "Yandex 77.88.8", "https://77.88.8.8/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("alidns", "AliDNS", "https://223.5.5.5/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("alidns_alt", "AliDNS 223.6.6.6", "https://223.6.6.6/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("cf-direct", "Cloudflare", "https://1.1.1.1/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("cf_sec", "Cloudflare 1.0.0.1", "https://1.0.0.1/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("google-direct", "Google", "https://8.8.8.8/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("google_alt", "Google 8.8.4.4", "https://8.8.4.4/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("quad9", "Quad9", "https://9.9.9.9/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("adguard", "AdGuard", "https://dns.adguard-dns.com/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("mullvad", "Mullvad", "https://dns.mullvad.net/dns-query", DnsStream.DIRECT),
        DnsDohEndpoint("opendns", "OpenDNS", "https://doh.opendns.com/dns-query", DnsStream.DIRECT)
    )

    val proxyDohServers: List<DnsDohEndpoint> = listOf(
        DnsDohEndpoint("cf-proxy", "Cloudflare", "https://1.1.1.1/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("cf_sec_p", "Cloudflare 1.0.0.1", "https://1.0.0.1/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("google-proxy", "Google", "https://8.8.8.8/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("google_alt_p", "Google 8.8.4.4", "https://8.8.4.4/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("quad9_p", "Quad9", "https://9.9.9.9/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("alidns-proxy", "AliDNS", "https://223.5.5.5/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("adguard_p", "AdGuard", "https://dns.adguard-dns.com/dns-query", DnsStream.PROXY),
        DnsDohEndpoint("mullvad_p", "Mullvad", "https://dns.mullvad.net/dns-query", DnsStream.PROXY)
    )

    /** SNI из белых списков РКН / Reality (WHITE-SNI-RU и типичные CDN). */
    val sniCandidates: List<String> = listOf(
        "www.microsoft.com",
        "update.microsoft.com",
        "www.windowsupdate.com",
        "download.windowsupdate.com",
        "www.cloudflare.com",
        "cdn.cloudflare.com",
        "www.google.com",
        "www.gstatic.com",
        "www.apple.com",
        "www.apple.com.cn",
        "www.bing.com",
        "www.amazon.com",
        "www.wikipedia.org",
        "ru.wikipedia.org",
        "www.yahoo.com",
        "www.adobe.com",
        "www.samsung.com",
        "www.zoom.us",
        "www.dropbox.com",
        "www.linkedin.com",
        "www.instagram.com",
        "www.facebook.com",
        "www.twitter.com",
        "www.reddit.com",
        "www.twitch.tv",
        "www.nvidia.com",
        "www.intel.com",
        "www.oracle.com",
        "www.akamai.net",
        "www.fastly.com",
        "www.cloudfront.net",
        "www.googletagmanager.com",
        "www.youtube.com",
        "i.ytimg.com",
        "www.github.com",
        "api.github.com",
        "www.stackoverflow.com",
        "www.cloudflare-dns.com",
        "dns.google",
        "one.one.one.one",
        "sns-video-qc.xhscdn.com",
        "stats.vk-portal.net",
        "sun6-21.userapi.com",
        "sun9-38.userapi.com",
        "eh.vk.com",
        "vkvideo.ru",
        "vk.com",
        "m.vk.com",
        "yandex.ru",
        "yandex.net",
        "yastatic.net",
        "sba.yandex.net",
        "sync.browser.yandex.net",
        "mail.ru",
        "ok.ru",
        "avito.ru",
        "www.wildberries.ru",
        "wb.ru",
        "www.ozon.ru",
        "st.ozone.ru",
        "cdn1.ozone.ru",
        "sberbank.ru",
        "online.sberbank.ru",
        "tbank.ru",
        "gosuslugi.ru",
        "pptest.gosuslugi.ru",
        "nalog.ru",
        "www.rutube.ru",
        "rutube.ru",
        "ivi.ru",
        "api.ivi.ru",
        "kinopoisk.ru",
        "www.kinopoisk.ru",
        "cdnvideo.ru",
        "ads.x5.ru",
        "x5.ru",
        "magnit.ru",
        "www.magnit.ru",
        "2gis.ru",
        "www.2gis.ru",
        "mts.ru",
        "mos.ru",
        "www.mos.ru",
        "tinkoff.ru",
        "www.tinkoff.ru",
        "alfabank.ru",
        "www.alfabank.ru",
        "beeline.ru",
        "www.beeline.ru",
        "megafon.ru",
        "www.megafon.ru",
        "tele2.ru",
        "www.tele2.ru",
        "rt.ru",
        "www.rt.ru",
        "rostelecom.ru",
        "www.rostelecom.ru",
        "spb.ru",
        "www.spb.ru",
        "rambler.ru",
        "lenta.ru",
        "ria.ru",
        "www.ria.ru",
        "tass.ru",
        "www.tass.ru",
        "rbc.ru",
        "www.rbc.ru",
        "hh.ru",
        "www.hh.ru",
        "dzen.ru",
        "www.dzen.ru",
        "yandex.com",
        "ya.ru",
        "www.ya.ru",
        "market.yandex.ru",
        "alice.yandex.ru",
        "cloud.mail.ru",
        "disk.yandex.ru",
        "docs.google.com",
        "drive.google.com",
        "play.google.com",
        "android.clients.google.com",
        "connectivitycheck.gstatic.com",
        "captive.apple.com",
        "www.msftconnecttest.com",
        "detectportal.firefox.com"
    )

    val dpiPresets: List<DpiFragmentPreset> = listOf(
        DpiFragmentPreset("light", "Лёгкий", "tlshello", "50-100", "5-10"),
        DpiFragmentPreset("medium", "Средний", "tlshello", "100-200", "10-20"),
        DpiFragmentPreset("aggressive", "Сильный", "1-3", "200-400", "20-40"),
        DpiFragmentPreset("hello", "TLS Hello", "tlshello", "80-150", "8-15"),
        DpiFragmentPreset("split", "Split TLS", "1-2", "120-250", "15-25"),
        DpiFragmentPreset("random", "Случайный", "1-5", "80-300", "10-30")
    )

    fun resolveDpiPreset(id: String?, mode: DpiBypassMode): DpiFragmentPreset? {
        id?.let { wanted ->
            dpiPresets.firstOrNull { it.id == wanted }?.let { return it }
        }
        return when (mode) {
            DpiBypassMode.LIGHT -> dpiPresets.first { it.id == "light" }
            DpiBypassMode.MEDIUM -> dpiPresets.first { it.id == "medium" }
            DpiBypassMode.AGGRESSIVE -> dpiPresets.first { it.id == "aggressive" }
            DpiBypassMode.AUTO -> dpiPresets.first { it.id == "medium" }
            else -> null
        }
    }

    fun effectiveDpi(profile: com.my.vpn.data.model.ConnectionBypassProfile): DpiFragmentPreset? {
        if (!profile.dpiEnabled) return null
        return when (profile.dpiMode) {
            DpiBypassMode.MANUAL -> DpiFragmentPreset(
                "manual", "Вручную", profile.dpiPackets, profile.dpiLength, profile.dpiInterval
            )
            else -> resolveDpiPreset(profile.dpiPresetId, profile.dpiMode)
        }
    }

    val ruDomainSuffixes = listOf(
        "domain:ru",
        "domain:su",
        "regexp:.*\\.xn--p1ai$"
    )

    val ruDirectExtraDomains = listOf(
        "domain:gosuslugi.ru",
        "domain:yandex.ru",
        "domain:vk.com",
        "domain:mail.ru",
        "domain:ozon.ru",
        "domain:wildberries.ru",
        "domain:sberbank.ru"
    )

    const val COUNTRY_ANYCAST_MARKER = "anycast"
}
