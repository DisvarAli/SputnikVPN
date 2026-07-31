package com.my.vpn.util

import java.nio.charset.StandardCharsets
import java.util.Base64

object Base64Util {

    fun decodeFlexible(input: String): String {
        val normalized = input.trim()
            .replace('-', '+')
            .replace('_', '/')
        val padding = (4 - normalized.length % 4) % 4
        val padded = normalized + "=".repeat(padding)
        return String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
    }

    fun decodeSubscription(raw: String): String {
        return runCatching { decodeFlexible(raw.replace("\n", "").trim()) }
            .getOrDefault(raw)
    }
}
