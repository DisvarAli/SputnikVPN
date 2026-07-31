package com.my.vpn.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.my.vpn.MyVpnApp
import com.my.vpn.data.repository.ConfigRepository
import com.my.vpn.vpn.VpnController

class ConfigUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MyVpnApp ?: return Result.failure()
        if (VpnController.isConnected) return Result.success()
        if (!app.appSettingsStorage.isBackgroundConfigUpdateEnabled()) return Result.success()

        val repo = app.configRepository
        return when {
            repo.peekSubscriptionChangesOnRemote() -> {
                when (repo.fullRefreshFromRemote(deferPing = true)) {
                    is ConfigRepository.RefreshResult.Success -> {
                        com.my.vpn.util.WorkScheduler.enqueueConfigPingNow(app)
                        Result.success()
                    }
                    is ConfigRepository.RefreshResult.Error -> Result.retry()
                    else -> Result.success()
                }
            }
            else -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "config_update_worker"
    }
}
