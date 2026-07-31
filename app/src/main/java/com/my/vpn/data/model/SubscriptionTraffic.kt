package com.my.vpn.data.model

data class SubscriptionTraffic(
    val usedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val expireAtEpochSec: Long? = null
) {
    val isUnlimited: Boolean
        get() = totalBytes == null || totalBytes <= 0L

    val remainingBytes: Long?
        get() = if (isUnlimited || totalBytes == null) null else (totalBytes - usedBytes).coerceAtLeast(0L)

    val usageFraction: Float?
        get() {
            if (isUnlimited || totalBytes == null || totalBytes <= 0L) return null
            return (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        }
}
