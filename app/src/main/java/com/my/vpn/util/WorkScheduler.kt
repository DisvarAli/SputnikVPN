package com.my.vpn.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.OutOfQuotaPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.my.vpn.AppConstants
import com.my.vpn.worker.ConfigPingWorker
import com.my.vpn.worker.ConfigUpdateWorker
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun scheduleBackgroundTasks(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val updateWork = PeriodicWorkRequestBuilder<ConfigUpdateWorker>(
            AppConstants.CONFIG_UPDATE_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ConfigUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateWork
        )

        val pingConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val pingWork = PeriodicWorkRequestBuilder<ConfigPingWorker>(6, TimeUnit.HOURS)
            .setConstraints(pingConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ConfigPingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            pingWork
        )
    }

    fun enqueueConfigPingNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ConfigPingWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${ConfigPingWorker.WORK_NAME}_once",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
