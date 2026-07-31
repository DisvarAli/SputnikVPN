package com.my.vpn.vpn

import android.app.AppOpsManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.core.app.NotificationCompat
import com.my.vpn.AppConstants
import com.my.vpn.MainActivity
import com.my.vpn.util.ConnectLog
import com.my.vpn.R
import com.my.vpn.data.model.SplitTunnelMode
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.CountryNames
import com.my.vpn.util.FormatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyVpnService : VpnService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    /** @see AppOpsManager.OPSTR_ESTABLISH_VPN_SERVICE (API 34+) */
    private val opEstablishVpn = "android:establish_vpn_service"

    private var vpnInterface: ParcelFileDescriptor? = null
    private var connectedConfig: VpnConfig? = null

    override fun onCreate() {
        super.onCreate()
        VpnCoreManager.attachService(this)
        VpnController.bindService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AppConstants.ACTION_VPN_STOP) {
            stopVpn(userRequested = true)
            return START_NOT_STICKY
        }

        connectedConfig = VpnCoreManager.pendingConfig
        VpnServiceStartTracker.markStarting()
        startForeground(AppConstants.NOTIFICATION_ID, buildNotification(connected = false))
        serviceScope.launch {
            if (!startOrRestartCore()) {
                ConnectLog.stage("service", "core_start_failed")
                VpnServiceStartTracker.markFailed(
                    VpnServiceStartTracker.lastFailureReason() ?: "core_start_failed"
                )
                stopVpn()
                return@launch
            }
            VpnServiceStartTracker.markInterfaceOk()
            startForeground(AppConstants.NOTIFICATION_ID, buildNotification(connected = true))
        }
        return START_STICKY
    }

    fun switchConfig(config: VpnConfig) {
        VpnCoreManager.pendingConfig = config
        connectedConfig = config
        startForeground(AppConstants.NOTIFICATION_ID, buildNotification(connected = false))
        if (SplitTunnelHolder.mode == SplitTunnelMode.SELECTED_APPS) {
            closeVpnInterface()
            if (!establishInterface()) {
                stopVpn()
                return
            }
        }

        if (!startOrRestartCore()) {
            stopVpn()
            return
        }

        startForeground(AppConstants.NOTIFICATION_ID, buildNotification(connected = true))
    }

    private fun startOrRestartCore(): Boolean {
        val config = VpnCoreManager.pendingConfig ?: connectedConfig ?: return false
        connectedConfig = config
        if (SplitTunnelHolder.mode == SplitTunnelMode.SELECTED_APPS) {
            closeVpnInterface()
        }
        if (vpnInterface == null && !establishInterface()) {
            ConnectLog.stage("service", "vpn_interface_failed")
            VpnServiceStartTracker.markFailed("vpn_interface_failed")
            return false
        }
        VpnServiceStartTracker.markInterfaceOk()
        val fd = vpnInterface?.fd ?: return false
        if (startCoreWithRetry(fd)) return true
        ConnectLog.stage("service", "startLoop_exception")
        VpnServiceStartTracker.markFailed("startLoop_exception")
        return false
    }

    private fun startCoreWithRetry(fd: Int): Boolean {
        if (VpnCoreManager.restartCore(fd)) return true
        Thread.sleep(350)
        return VpnCoreManager.restartCore(fd)
    }

    fun updateConnectedNotification(config: VpnConfig?, rxBytes: Long, txBytes: Long) {
        if (!VpnCoreManager.isRunning()) return
        connectedConfig = config ?: connectedConfig
        val notification = buildNotification(
            connected = true,
            config = connectedConfig,
            rxBytes = rxBytes,
            txBytes = txBytes
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(AppConstants.NOTIFICATION_ID, notification)
    }

    private fun establishInterface(): Boolean {
        closeVpnInterface()
        repeat(3) { attempt ->
            if (establishInterfaceOnce()) return true
            if (attempt < 2) Thread.sleep(120)
        }
        VpnServiceStartTracker.markFailed("vpn_interface_failed")
        ConnectLog.stage("service", "vpn_interface_failed")
        return false
    }

    private fun establishInterfaceOnce(): Boolean {
        val appOps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getSystemService(AppOpsManager::class.java)
        } else {
            null
        }
        var vpnOpStarted = false
        return try {
            if (VpnService.prepare(this@MyVpnService) != null) {
                ConnectLog.stage("vpn_interface", "not_prepared")
                VpnServiceStartTracker.markFailed("vpn_not_prepared")
                return false
            }
            if (appOps != null) {
                val mode = appOps.startOp(opEstablishVpn, Process.myUid(), packageName)
                if (mode != AppOpsManager.MODE_ALLOWED) {
                    ConnectLog.stage("vpn_interface", "appops_denied:$mode")
                    VpnServiceStartTracker.markFailed("appops_denied")
                    return false
                }
                vpnOpStarted = true
            }
            val perApp = SplitTunnelHolder.mode == SplitTunnelMode.SELECTED_APPS &&
                SplitTunnelHolder.allowedPackages.isNotEmpty()
            val sessionLabel = getString(R.string.app_name)
            val builder = Builder()
                .setSession(sessionLabel)
                .setMtu(AppConstants.VPN_MTU)
                .addAddress(AppConstants.VPN_IPV4_CLIENT, 30)
                .addRoute("0.0.0.0", 0)
                .addDisallowedApplication(packageName)
            // IPv6-маршрут ::/0 часто даёт «VPN есть, интернета нет» — только IPv4 через TUN.
            AppConstants.VPN_DNS_SERVERS.forEach { builder.addDnsServer(it) }

            if (perApp) {
                var added = 0
                SplitTunnelHolder.allowedPackages.forEach { pkg ->
                    runCatching {
                        builder.addAllowedApplication(pkg)
                        added++
                    }.onFailure { e ->
                        ConnectLog.stage("vpn_split", "allow_fail:$pkg:${e.message}")
                    }
                }
                if (added == 0) {
                    ConnectLog.stage("vpn_split", "no_allowed_apps")
                    return false
                }
                ConnectLog.stage("vpn_split", "allowed_apps:$added")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
                builder.setBlocking(false)
            }

            vpnInterface = builder.establish()
            vpnInterface != null
        } catch (e: Exception) {
            ConnectLog.w("establishInterface", e)
            false
        } finally {
            if (vpnOpStarted && appOps != null) {
                appOps.finishOp(opEstablishVpn, Process.myUid(), packageName)
            }
        }
    }

    fun stopVpn(@Suppress("UNUSED_PARAMETER") userRequested: Boolean = false) {
        VpnCoreManager.stopCore()
        stopVpnInternal()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun stopVpnInternal() {
        closeVpnInterface()
        connectedConfig = null
    }

    private fun closeVpnInterface() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    override fun onDestroy() {
        serviceJob.cancel()
        stopVpnInternal()
        VpnController.unbindService(this)
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn(userRequested = true)
        super.onRevoke()
    }

    private fun buildNotification(
        connected: Boolean,
        config: VpnConfig? = connectedConfig,
        rxBytes: Long = 0L,
        txBytes: Long = 0L
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MyVpnService::class.java).setAction(AppConstants.ACTION_VPN_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (connected) {
            getString(R.string.vpn_notification_title)
        } else {
            getString(R.string.app_name)
        }

        val contentText = when {
            connected && config != null -> {
                val country = CountryNames.toRussian(config.country)
                "$country · ${config.protocol.displayName}"
            }
            connected -> getString(R.string.vpn_connected)
            else -> getString(R.string.vpn_connecting)
        }

        val subText = if (connected && (rxBytes > 0 || txBytes > 0)) {
            "↓ ${FormatUtil.formatBytes(rxBytes)}  ↑ ${FormatUtil.formatBytes(txBytes)}"
        } else if (connected) {
            getString(R.string.vpn_notification_subtitle)
        } else {
            null
        }

        return NotificationCompat.Builder(applicationContext, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stat_vpn, getString(R.string.disconnect), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(connected)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
