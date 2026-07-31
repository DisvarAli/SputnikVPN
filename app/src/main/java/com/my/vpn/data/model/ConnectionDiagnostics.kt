package com.my.vpn.data.model

data class ConnectionDiagnostics(
    val timestampMs: Long = System.currentTimeMillis(),
    val serverLabel: String = "",
    val protocol: String = "",
    val tcpReachable: Boolean? = null,
    val proxyVerified: Boolean = false,
    val coreRunning: Boolean = false,
    val tunnelHealthy: Boolean? = null,
    val summary: String = "",
    val suggestion: String = ""
)
