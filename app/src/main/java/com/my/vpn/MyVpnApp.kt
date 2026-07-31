package com.my.vpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.my.vpn.data.local.AppSettingsStorage
import com.my.vpn.data.local.CustomSubscriptionStorage
import com.my.vpn.data.local.PerServerBypassStorage
import com.my.vpn.data.repository.ConfigRepository
import com.my.vpn.di.AppContainer
import com.my.vpn.util.LocaleHelper
import com.my.vpn.util.WorkScheduler
import com.my.vpn.vpn.CoreNativeManager
import com.my.vpn.vpn.VpnCoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyVpnApp : Application() {

    private lateinit var container: AppContainer

    val configRepository: ConfigRepository
        get() = container.configRepository

    val appSettingsStorage: AppSettingsStorage
        get() = container.appSettingsStorage

    val perServerBypassStorage: PerServerBypassStorage
        get() = container.perServerBypassStorage

    val customSubscriptionStorage: CustomSubscriptionStorage
        get() = container.customSubscriptionStorage

    val lastConnectStorage: com.my.vpn.data.local.LastConnectStorage
        get() = container.lastConnectStorage

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun attachBaseContext(base: Context) {
        val language = AppSettingsStorage.readLanguageBlocking(base)
        super.attachBaseContext(LocaleHelper.wrap(base, language))
    }

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applyLanguage(this, AppSettingsStorage.readLanguageBlocking(this))
        instance = this
        container = AppContainer(this)
        createNotificationChannel()
        CoreNativeManager.initCoreEnv(this)
        appScope.launch(Dispatchers.Default) {
            VpnCoreManager.warmup()
        }
        appScope.launch {
            appSettingsStorage.syncLanguageEarlyCache()
            appSettingsStorage.loadVpnOptionHolders()
        }
        WorkScheduler.scheduleBackgroundTasks(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.vpn_notification_subtitle)
                setShowBadge(true)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: MyVpnApp
            private set
    }
}
