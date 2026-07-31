package com.my.vpn.data.model

enum class AppLanguage(val storageKey: String, val localeTag: String?) {
    SYSTEM("system", null),
    RUSSIAN("ru", "ru"),
    ENGLISH("en", "en");

    companion object {
        fun fromKey(key: String?): AppLanguage =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}
