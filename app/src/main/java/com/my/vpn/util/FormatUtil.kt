package com.my.vpn.util

import android.content.Context
import android.net.TrafficStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrafficMonitor {

    private var baselineRx = 0L
    private var baselineTx = 0L

    fun resetBaseline(context: Context) {
        val uid = context.applicationInfo.uid
        baselineRx = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
        baselineTx = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0)
    }

    fun clear() {
        baselineRx = 0L
        baselineTx = 0L
    }

    fun getSessionTraffic(context: Context): Pair<Long, Long> {
        val uid = context.applicationInfo.uid
        val rx = (TrafficStats.getUidRxBytes(uid) - baselineRx).coerceAtLeast(0)
        val tx = (TrafficStats.getUidTxBytes(uid) - baselineTx).coerceAtLeast(0)
        return rx to tx
    }
}

object FormatUtil {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru", "RU"))

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format(Locale.US, "%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    fun formatUpdatedAt(timestampMs: Long): String {
        if (timestampMs <= 0L) return "—"
        return dateFormat.format(Date(timestampMs))
    }

    fun formatTrafficLabel(usedBytes: Long, totalBytes: Long?): String {
        val used = formatBytes(usedBytes)
        return if (totalBytes == null || totalBytes <= 0L) {
            "$used · безлимит"
        } else {
            val left = (totalBytes - usedBytes).coerceAtLeast(0L)
            "$used / ${formatBytes(totalBytes)} · осталось ${formatBytes(left)}"
        }
    }
}
