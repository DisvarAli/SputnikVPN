package com.my.vpn.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.my.vpn.MyVpnApp
import com.my.vpn.vpn.VpnController

/**
 * Фоновая догоняющая проверка: TCP чанками, VPN-тест — только топ по TCP (см. [ConfigRepository.backgroundPingChunk]).
 */
class ConfigPingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MyVpnApp ?: return Result.failure()
        if (VpnController.isConnected) return Result.success()
        if (VpnController.isConnectInProgress()) return Result.success()
        if (!app.appSettingsStorage.isFullCachePingScheduled()) return Result.success()
        if (app.appSettingsStorage.isBackgroundPingWifiOnly() && !isOnWifi(app)) {
            return Result.success()
        }

        val repo = app.configRepository
        if (repo.loadCachedConfigs().isEmpty()) return Result.success()

        val allDone = repo.backgroundPingChunk()
        if (allDone) {
            app.appSettingsStorage.setFullCachePingScheduled(false)
        }
        return Result.success()
    }

    private fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        const val WORK_NAME = "config_ping_worker"
    }
}
