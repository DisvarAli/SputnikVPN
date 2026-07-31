package com.my.vpn.util

import android.content.Context
import android.content.Intent
import android.net.VpnService

object VpnPrepare {

    /**
     * Intent для системного диалога VPN, или null если разрешение уже выдано этому пакету.
     * Работает для всех applicationId (в т.ч. dev-сборка `com.my.vpn.dev`).
     */
    fun prepareIntent(context: Context): Intent? {
        val app = context.applicationContext
        return runCatching { VpnService.prepare(app) }.getOrNull()
    }

    fun isPrepared(context: Context): Boolean = prepareIntent(context) == null
}
