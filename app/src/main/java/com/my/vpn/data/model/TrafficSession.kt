package com.my.vpn.data.model

import java.util.UUID

data class TrafficSession(
    val id: String = UUID.randomUUID().toString(),
    val configName: String,
    val country: String,
    val protocol: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val rxBytes: Long,
    val txBytes: Long
) {
    val durationMs: Long get() = (endedAtMs - startedAtMs).coerceAtLeast(0L)
}
