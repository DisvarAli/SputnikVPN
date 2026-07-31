package com.my.vpn.ui.theme

enum class AppThemeMode(val storageKey: String, val label: String) {
    DARK_STANDARD("dark_standard", "Тёмная"),
    DARK_RED("dark_red", "Тёмная (красная)"),
    LIGHT("light", "Дневная"),
    SYSTEM("system", "Как в системе");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == key }
                ?: when (key) {
                    null, "dark_red" -> DARK_RED
                    else -> DARK_STANDARD
                }
    }
}
