package com.my.vpn.util

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.io.File

object AssetUtil {

    fun userAssetPath(context: Context): String {
        val dir = context.getExternalFilesDir("assets")
            ?: File(context.filesDir, "assets").also { it.mkdirs() }
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    /**
     * Xray требует 32-байтный BaseKey (стандартное кодирование для libv2ray).
     */
    fun getDeviceIdForXUDPBaseKey(context: Context): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: "sputnik_fallback"
        val bytes = raw.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(
            bytes.copyOf(32),
            Base64.NO_PADDING or Base64.URL_SAFE
        )
    }
}
