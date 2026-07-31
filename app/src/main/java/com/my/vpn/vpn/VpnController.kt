package com.my.vpn.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.AppConstants
import com.my.vpn.util.VpnTrafficMonitor
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import com.my.vpn.util.ConnectLog
import kotlinx.coroutines.delay

object VpnController {

    private val connectInProgress = AtomicBoolean(false)

    private var activeConfigId: String? = null
    private var activeConfig: VpnConfig? = null
    var serviceRef: WeakReference<MyVpnService>? = null
        private set

    val isConnected: Boolean
        get() = VpnCoreManager.isRunning()

    val connectedConfigId: String?
        get() = activeConfigId

    fun bindService(service: MyVpnService) {
        serviceRef = WeakReference(service)
    }

    fun unbindService(service: MyVpnService) {
        if (serviceRef?.get() === service) {
            serviceRef = null
        }
    }

    fun isConnectInProgress(): Boolean = connectInProgress.get()

    fun endConnectAttempt() {
        connectInProgress.set(false)
    }

    fun connect(context: Context, config: VpnConfig) {
        connectInProgress.set(true)
        VpnCoreManager.stopCoreFully()
        VpnServiceStartTracker.reset()
        activeConfigId = config.id
        activeConfig = config
        VpnCoreManager.pendingConfig = config
        VpnTrafficMonitor.resetBaseline(context)
        val intent = Intent(context, MyVpnService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    /** Ждёт bind сервиса; при [VpnServiceStartTracker.hasFailed] — сразу false. */
    suspend fun awaitServiceReady(
        timeoutMs: Long = AppConstants.VPN_SERVICE_START_TIMEOUT_MS
    ): Boolean = awaitServiceStart(timeoutMs)

    suspend fun awaitServiceStart(
        timeoutMs: Long = AppConstants.VPN_SERVICE_START_TIMEOUT_MS
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (VpnServiceStartTracker.hasFailed()) {
                ConnectLog.stage("service", "failed:${VpnServiceStartTracker.lastFailureReason()}")
                return false
            }
            if (VpnCoreManager.isRunning()) return true
            delay(30)
        }
        if (VpnServiceStartTracker.hasFailed()) return false
        return serviceRef?.get() != null && !VpnServiceStartTracker.hasFailed()
    }

    suspend fun awaitCoreStartAttempt(
        timeoutMs: Long = AppConstants.VPN_CORE_START_TIMEOUT_MS
    ): Boolean {
        if (VpnServiceStartTracker.hasFailed()) return false
        return VpnCoreManager.awaitCoreRunning(timeoutMs = timeoutMs, lateGraceMs = 2_000L)
    }

    fun reconnect(context: Context, config: VpnConfig) {
        val service = serviceRef?.get()
        if (service != null && VpnCoreManager.isRunning()) {
            activeConfigId = config.id
            activeConfig = config
            VpnCoreManager.pendingConfig = config
            VpnTrafficMonitor.resetBaseline(context)
            VpnServiceStartTracker.markStarting()
            service.switchConfig(config)
        } else {
            connect(context, config)
        }
    }

    fun disconnect(context: Context, userRequested: Boolean = true) {
        connectInProgress.set(false)
        activeConfigId = null
        activeConfig = null
        VpnCoreManager.pendingConfig = null
        VpnTrafficMonitor.clear()
        serviceRef?.get()?.stopVpn(userRequested = userRequested)
        context.stopService(Intent(context, MyVpnService::class.java))
    }

    fun updateNotification(context: Context, rxBytes: Long, txBytes: Long) {
        serviceRef?.get()?.updateConnectedNotification(
            config = activeConfig,
            rxBytes = rxBytes,
            txBytes = txBytes
        )
    }
}
