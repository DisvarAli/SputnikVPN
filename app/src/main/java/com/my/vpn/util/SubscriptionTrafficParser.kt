package com.my.vpn.util

import com.my.vpn.data.model.SubscriptionTraffic

object SubscriptionTrafficParser {

    fun fromUserInfoHeader(header: String?): SubscriptionTraffic? {
        if (header.isNullOrBlank()) return null
        val parts = header.split(";").mapNotNull { segment ->
            val kv = segment.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0].trim().lowercase() to kv[1].trim() else null
        }.toMap()

        val upload = parts["upload"]?.toLongOrNull() ?: 0L
        val download = parts["download"]?.toLongOrNull() ?: 0L
        val total = parts["total"]?.toLongOrNull()
        val expire = parts["expire"]?.toLongOrNull()

        return SubscriptionTraffic(
            usedBytes = upload + download,
            totalBytes = total,
            expireAtEpochSec = expire
        )
    }
}
