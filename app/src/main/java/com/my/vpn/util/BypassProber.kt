package com.my.vpn.util

import com.my.vpn.data.model.DnsDohEndpoint
import com.my.vpn.data.model.DnsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket

object BypassProber {

    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun findBestDirectDoh(onProgress: ((String) -> Unit)? = null): DnsDohEndpoint? =
        probeDohList(BypassPresets.directDohServers, onProgress)

    suspend fun findBestProxyDoh(onProgress: ((String) -> Unit)? = null): DnsDohEndpoint? =
        probeDohList(BypassPresets.proxyDohServers, onProgress)

    private suspend fun probeDohList(
        list: List<DnsDohEndpoint>,
        onProgress: ((String) -> Unit)?
    ): DnsDohEndpoint? = withContext(Dispatchers.IO) {
        onProgress?.invoke("Проверка DNS…")
        val results = list.map { endpoint ->
            async {
                val ms = measureDohInternal(endpoint.url)
                endpoint to ms
            }
        }.awaitAll()
        val best = results.filter { it.second != null }.minByOrNull { it.second!! }
        best?.first
    }

    fun measureDoh(url: String): Long? = measureDohInternal(url)

    fun measureUdp(host: String, port: Int, timeoutMs: Int = 2000): Long? {
        return runCatching {
            val start = System.nanoTime()
            val packet = java.net.DatagramPacket(
                ByteArray(1),
                1,
                java.net.InetAddress.getByName(host),
                port
            )
            java.net.DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.send(packet)
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrNull()
    }

    fun measureTcp(host: String, port: Int, timeoutMs: Int = 2500): Long? {
        return runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrNull()
    }

    private fun measureDohInternal(url: String): Long? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-message")
            .get()
            .build()
        return runCatching {
            val start = System.nanoTime()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 400 && response.code != 415) return@runCatching null
                (System.nanoTime() - start) / 1_000_000
            }
        }.getOrNull()
    }

    suspend fun findBestSni(
        host: String,
        port: Int,
        candidates: List<String> = BypassPresets.sniCandidates,
        onProgress: ((String) -> Unit)? = null,
        onIndexedProgress: ((Int, Int, String) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        if (host.isBlank() || port !in 1..65535) return@withContext null
        val list = candidates.take(80)
        var best: Pair<String, Long>? = null
        list.forEachIndexed { index, sni ->
            onIndexedProgress?.invoke(index + 1, list.size, sni)
            onProgress?.invoke("SNI ${index + 1}/${list.size}: $sni")
            val ms = measureTlsSni(host, port, sni) ?: return@forEachIndexed
            if (best == null || ms < best!!.second) best = sni to ms
        }
        best?.first
    }

    fun measureTlsSni(host: String, port: Int, sni: String, timeoutMs: Int = 3500): Long? {
        return runCatching {
            val start = System.nanoTime()
            val sslContext = SSLContext.getDefault()
            val ssl = sslContext.socketFactory.createSocket() as SSLSocket
            ssl.use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val params = SSLParameters()
                params.serverNames = listOf(SNIHostName(sni))
                socket.sslParameters = params
                socket.soTimeout = timeoutMs
                socket.startHandshake()
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrNull()
    }
}
