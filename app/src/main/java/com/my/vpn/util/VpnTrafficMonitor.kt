package com.my.vpn.util

import android.content.Context
import android.net.TrafficStats

/**
 * Счётчик трафика VPN: сначала TUN-интерфейс, иначе UID приложения.
 */
object VpnTrafficMonitor {

    private var baselineRx = 0L
    private var baselineTx = 0L
    private var baselineUidRx = 0L
    private var baselineUidTx = 0L
    private var useTun = false

    fun resetBaseline(context: Context) {
        val tun = readTunTotals()
        if (tun != null) {
            useTun = true
            baselineRx = tun.first
            baselineTx = tun.second
        } else {
            useTun = false
            val uid = context.applicationInfo.uid
            baselineUidRx = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
            baselineUidTx = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0)
        }
    }

    fun clear() {
        baselineRx = 0L
        baselineTx = 0L
        baselineUidRx = 0L
        baselineUidTx = 0L
        useTun = false
    }

    fun getSessionTraffic(context: Context): Pair<Long, Long> {
        if (useTun) {
            val current = readTunTotals()
            if (current != null) {
                val rx = (current.first - baselineRx).coerceAtLeast(0)
                val tx = (current.second - baselineTx).coerceAtLeast(0)
                return rx to tx
            }
        }
        val uid = context.applicationInfo.uid
        val rx = (TrafficStats.getUidRxBytes(uid) - baselineUidRx).coerceAtLeast(0)
        val tx = (TrafficStats.getUidTxBytes(uid) - baselineUidTx).coerceAtLeast(0)
        return rx to tx
    }

    private fun readTunTotals(): Pair<Long, Long>? {
        val text = runCatching { java.io.File("/proc/net/dev").readText() }.getOrNull() ?: return null
        var totalRx = 0L
        var totalTx = 0L
        var found = false
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (!line.contains(':')) return@forEach
            val name = line.substringBefore(':').trim()
            if (!name.startsWith("xray") &&
                !name.startsWith("tun") &&
                name != "tun0"
            ) {
                return@forEach
            }
            val nums = line.substringAfter(':').trim().split(Regex("\\s+"))
            if (nums.size < 9) return@forEach
            totalRx += nums[0].toLongOrNull() ?: 0L
            totalTx += nums[8].toLongOrNull() ?: 0L
            found = true
        }
        return if (found) totalRx to totalTx else null
    }
}
