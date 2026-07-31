package com.my.vpn.update

import android.content.Context
import com.my.vpn.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object AppUpdateDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    suspend fun downloadLatestApk(
        context: Context,
        onProgress: ((Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val release = AppReleaseChecker.fetchLatestRelease()
                ?: error("Не удалось получить информацию о релизе")
            val url = resolveApkUrl(release.tagName)
            val outDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(outDir, "app-debug.apk")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SputnikVPN/${AppConstants.APP_VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body ?: error("Пустой ответ")
                val total = body.contentLength().coerceAtLeast(1L)
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            onProgress?.invoke(pct)
                        }
                    }
                }
            }
            outFile
        }
    }

    private fun resolveApkUrl(tagName: String): String {
        val tag = tagName.trim().ifBlank { "v${AppConstants.APP_VERSION_NAME}" }
        val normalized = if (tag.startsWith("v", ignoreCase = true)) tag else "v$tag"
        val base =
            "https://github.com/${AppConstants.APP_GITHUB_OWNER}/${AppConstants.APP_GITHUB_REPO}/releases/download/$normalized"
        val releaseUrl = "$base/app-release.apk"
        if (releaseApkExists(releaseUrl)) return releaseUrl
        return "$base/app-debug.apk"
    }

    private fun releaseApkExists(url: String): Boolean = runCatching {
        val head = Request.Builder().url(url).head().build()
        client.newCall(head).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}
