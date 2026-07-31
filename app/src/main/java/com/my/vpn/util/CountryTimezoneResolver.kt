package com.my.vpn.util

import com.my.vpn.data.model.VpnConfig
import java.util.TimeZone

/**
 * Подбор IANA timezone по стране из конфига (remark / country в подписке).
 */
object CountryTimezoneResolver {

    private val FLAG_PREFIX = Regex("^([\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF])\\s*")

    private val COUNTRY_TO_TZ = mapOf(
        "netherlands" to "Europe/Amsterdam",
        "holland" to "Europe/Amsterdam",
        "germany" to "Europe/Berlin",
        "france" to "Europe/Paris",
        "united kingdom" to "Europe/London",
        "uk" to "Europe/London",
        "england" to "Europe/London",
        "usa" to "America/New_York",
        "united states" to "America/New_York",
        "us" to "America/New_York",
        "canada" to "America/Toronto",
        "finland" to "Europe/Helsinki",
        "sweden" to "Europe/Stockholm",
        "norway" to "Europe/Oslo",
        "denmark" to "Europe/Copenhagen",
        "poland" to "Europe/Warsaw",
        "czech republic" to "Europe/Prague",
        "czechia" to "Europe/Prague",
        "austria" to "Europe/Vienna",
        "switzerland" to "Europe/Zurich",
        "italy" to "Europe/Rome",
        "spain" to "Europe/Madrid",
        "portugal" to "Europe/Lisbon",
        "belgium" to "Europe/Brussels",
        "ireland" to "Europe/Dublin",
        "romania" to "Europe/Bucharest",
        "bulgaria" to "Europe/Sofia",
        "hungary" to "Europe/Budapest",
        "greece" to "Europe/Athens",
        "turkey" to "Europe/Istanbul",
        "israel" to "Asia/Jerusalem",
        "uae" to "Asia/Dubai",
        "united arab emirates" to "Asia/Dubai",
        "singapore" to "Asia/Singapore",
        "japan" to "Asia/Tokyo",
        "south korea" to "Asia/Seoul",
        "korea" to "Asia/Seoul",
        "hong kong" to "Asia/Hong_Kong",
        "taiwan" to "Asia/Taipei",
        "india" to "Asia/Kolkata",
        "australia" to "Australia/Sydney",
        "new zealand" to "Pacific/Auckland",
        "brazil" to "America/Sao_Paulo",
        "argentina" to "America/Argentina/Buenos_Aires",
        "mexico" to "America/Mexico_City",
        "chile" to "America/Santiago",
        "colombia" to "America/Bogota",
        "south africa" to "Africa/Johannesburg",
        "egypt" to "Africa/Cairo",
        "ukraine" to "Europe/Kyiv",
        "belarus" to "Europe/Minsk",
        "kazakhstan" to "Asia/Almaty",
        "latvia" to "Europe/Riga",
        "lithuania" to "Europe/Vilnius",
        "estonia" to "Europe/Tallinn",
        "moldova" to "Europe/Chisinau",
        "serbia" to "Europe/Belgrade",
        "croatia" to "Europe/Zagreb",
        "slovakia" to "Europe/Bratislava",
        "slovenia" to "Europe/Ljubljana",
        "iceland" to "Atlantic/Reykjavik",
        "russia" to "Europe/Moscow",
        "russian federation" to "Europe/Moscow",
        "россия" to "Europe/Moscow",
        "рф" to "Europe/Moscow",
        "china" to "Asia/Shanghai",
        "китай" to "Asia/Shanghai",
        "thailand" to "Asia/Bangkok",
        "vietnam" to "Asia/Ho_Chi_Minh",
        "indonesia" to "Asia/Jakarta",
        "malaysia" to "Asia/Kuala_Lumpur",
        "philippines" to "Asia/Manila",
        "pakistan" to "Asia/Karachi",
        "iran" to "Asia/Tehran",
        "iraq" to "Asia/Baghdad",
        "qatar" to "Asia/Qatar",
        "saudi arabia" to "Asia/Riyadh",
        "armenia" to "Asia/Yerevan",
        "georgia" to "Asia/Tbilisi",
        "azerbaijan" to "Asia/Baku",
        "cyprus" to "Asia/Nicosia",
        "luxembourg" to "Europe/Luxembourg",
        "monaco" to "Europe/Monaco",
        "malta" to "Europe/Malta"
    )

    fun resolve(config: VpnConfig): TimeZone {
        val id = resolveZoneId(config) ?: "UTC"
        return TimeZone.getTimeZone(id)
    }

    fun resolveZoneId(config: VpnConfig): String? {
        val token = normalizeCountryToken(config.country)
        if (token.isBlank()) return guessFromServer(config.server)
        COUNTRY_TO_TZ[token]?.let { return it }
        COUNTRY_TO_TZ.entries.firstOrNull { (key, _) ->
            token.contains(key) || key.contains(token)
        }?.value?.let { return it }
        return guessFromServer(config.server)
    }

    private fun normalizeCountryToken(raw: String): String {
        val withoutFlag = FLAG_PREFIX.replace(raw, "").trim()
        val namePart = withoutFlag
            .split(",", "|", "-", "·", "—")
            .firstOrNull()
            ?.trim()
            .orEmpty()
            .ifBlank { withoutFlag }
        return namePart.lowercase()
    }

    /** Грубая эвристика по TLD / известным диапазонам — запасной вариант. */
    private fun guessFromServer(server: String): String? {
        val host = server.lowercase()
        return when {
            host.endsWith(".de") -> "Europe/Berlin"
            host.endsWith(".fr") -> "Europe/Paris"
            host.endsWith(".uk") || host.endsWith(".co.uk") -> "Europe/London"
            host.endsWith(".nl") -> "Europe/Amsterdam"
            host.endsWith(".jp") -> "Asia/Tokyo"
            host.endsWith(".sg") -> "Asia/Singapore"
            host.endsWith(".ru") -> "Europe/Moscow"
            host.endsWith(".us") -> "America/New_York"
            else -> null
        }
    }
}
