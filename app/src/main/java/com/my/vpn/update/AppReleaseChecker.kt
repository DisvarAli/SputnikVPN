package com.my.vpn.update

import com.google.gson.JsonParser
import com.my.vpn.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val htmlUrl: String,
    val changelog: String
)

object AppReleaseChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/${AppConstants.APP_GITHUB_OWNER}/${AppConstants.APP_GITHUB_REPO}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SputnikVPN/${AppConstants.APP_VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                val json = JsonParser.parseString(body).asJsonObject
                val tag = json.get("tag_name")?.asString?.trimStart('v', 'V').orEmpty()
                if (tag.isBlank()) return@withContext null
                AppReleaseInfo(
                    versionName = tag,
                    tagName = json.get("tag_name")?.asString.orEmpty(),
                    htmlUrl = json.get("html_url")?.asString
                        ?: "https://github.com/${AppConstants.APP_GITHUB_OWNER}/${AppConstants.APP_GITHUB_REPO}/releases",
                    changelog = json.get("body")?.asString?.trim().orEmpty()
                        .ifBlank { "См. страницу релиза на GitHub." }
                )
            }
        }.getOrNull()
    }

    fun isNewerThanInstalled(latest: String, installed: String): Boolean {
        return compareVersions(latest, installed) > 0
    }

    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".", "-").mapNotNull { it.toIntOrNull() }
        val pb = b.split(".", "-").mapNotNull { it.toIntOrNull() }
        val max = maxOf(pa.size, pb.size)
        for (i in 0 until max) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }
}
