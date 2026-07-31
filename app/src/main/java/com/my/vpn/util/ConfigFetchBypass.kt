package com.my.vpn.util

import com.my.vpn.data.model.MirrorType
import com.my.vpn.data.model.SubscriptionUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Перебор зеркал и файлов белых списков РФ для загрузки конфигов без VPN
 * (в т.ч. при блокировке raw.githubusercontent.com).
 *
 * Стратегия: сначала параллельный probe по зеркалам на одном лёгком файле,
 * затем при необходимости — остальные probe-файлы.
 */
object ConfigFetchBypass {

    data class Progress(val index: Int, val total: Int, val label: String)

    data class FetchPath(
        val mirror: MirrorType,
        val probeFile: String
    )

    private const val MIRROR_BATCH = 4

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun findWorkingFetchPath(
        onProgress: (Progress) -> Unit
    ): FetchPath? = withContext(Dispatchers.IO) {
        val mirrors = SubscriptionUrls.defaultMirrorOrder
        val files = WhiteListConfigSources.probeFiles
        val total = mirrors.size * files.size
        val counter = AtomicInteger(0)
        val progressMutex = Mutex()

        suspend fun report(label: String) {
            val idx = counter.incrementAndGet()
            progressMutex.withLock {
                onProgress(Progress(idx.coerceAtMost(total), total, label))
            }
        }

        // 1) Быстрый проход: каждое зеркало × первый (самый маленький) файл, батчами.
        val primaryFile = files.first()
        findInMirrorBatches(mirrors, primaryFile, ::report)?.let { return@withContext it }

        // 2) Остальные файлы по зеркалам (зеркало могло отдавать 404 на один файл).
        for (file in files.drop(1)) {
            findInMirrorBatches(mirrors, file, ::report)?.let { return@withContext it }
        }
        null
    }

    private suspend fun findInMirrorBatches(
        mirrors: List<MirrorType>,
        file: String,
        report: suspend (String) -> Unit
    ): FetchPath? = coroutineScope {
        for (batch in mirrors.chunked(MIRROR_BATCH)) {
            val results = batch.map { mirror ->
                async(Dispatchers.IO) {
                    val label = "${mirror.label} · ${WhiteListConfigSources.probeLabels[file] ?: file}"
                    report(label)
                    if (probeUrl(SubscriptionUrls.buildUrl(mirror, file))) {
                        FetchPath(mirror, file)
                    } else {
                        null
                    }
                }
            }.awaitAll()
            results.firstOrNull { it != null }?.let { return@coroutineScope it }
        }
        null
    }

    private fun probeUrl(url: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SputnikVPN/2.1")
            .header("Cache-Control", "no-cache")
            .header("Accept", "text/plain,*/*")
            .get()
            .build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching false
                val body = response.body?.string().orEmpty()
                looksLikeSubscription(body)
            }
        }.getOrDefault(false)
    }

    /** Сырой текст подписки или HTML-обёртка (Яндекс-переводчик). */
    internal fun looksLikeSubscription(body: String): Boolean {
        if (body.length < 8) return false
        if (body.contains("://")) {
            val lines = body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.any { isShareLink(it) }) return true
            // HTML-прокси: ссылки внутри страницы
            if (SHARE_LINK_REGEX.containsMatchIn(body)) return true
        }
        return false
    }

    private fun isShareLink(t: String): Boolean =
        t.startsWith("vless://", true) ||
            t.startsWith("vmess://", true) ||
            t.startsWith("trojan://", true) ||
            t.startsWith("ss://", true) ||
            t.startsWith("hysteria2://", true) ||
            t.startsWith("hy2://", true) ||
            t.startsWith("tuic://", true) ||
            t.startsWith("wireguard://", true) ||
            t.startsWith("wg://", true)

    private val SHARE_LINK_REGEX = Regex(
        "(?i)(vless|vmess|trojan|ss|hysteria2|hy2|tuic|wireguard|wg)://"
    )
}
