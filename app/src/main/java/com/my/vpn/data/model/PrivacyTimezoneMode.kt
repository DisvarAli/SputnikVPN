package com.my.vpn.data.model

enum class PrivacyTimezoneMode(val storageKey: String) {
    /** Не менять часовой пояс приложения. */
    OFF("off"),
    /** По стране выбранного / подключённого сервера. */
    AUTO("auto");

    val isAuto: Boolean get() = this == AUTO

    companion object {
        fun fromKey(key: String?): PrivacyTimezoneMode = when (key) {
            "off" -> OFF
            null, "auto", "auto_from_server",
            "utc", "us_east", "eu_central", "asia_sg" -> AUTO
            else -> AUTO
        }
    }
}
