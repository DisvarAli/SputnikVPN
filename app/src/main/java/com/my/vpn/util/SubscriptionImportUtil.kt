package com.my.vpn.util

import android.content.Context
import android.net.Uri

object SubscriptionImportUtil {

    fun readTextFromUri(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    fun extractSubscriptionText(raw: String, context: android.content.Context? = null): String {
        val trimmed = HappLinkResolver.normalizeSubscriptionInput(raw.trim(), context)
        if (HappLinkResolver.isEncryptedHappLink(trimmed)) return trimmed
        if (trimmed.contains("://")) return trimmed
        return runCatching {
            val decoded = android.util.Base64.decode(
                trimmed.replace("\n", ""),
                android.util.Base64.DEFAULT
            )
            String(decoded, Charsets.UTF_8)
        }.getOrDefault(trimmed)
    }
}
