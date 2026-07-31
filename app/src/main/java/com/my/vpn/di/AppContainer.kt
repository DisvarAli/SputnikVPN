package com.my.vpn.di

import android.content.Context
import com.my.vpn.data.local.AppSettingsStorage
import com.my.vpn.data.local.ConfigStorage
import com.my.vpn.data.local.CustomSubscriptionStorage
import com.my.vpn.data.local.PerServerBypassStorage
import com.my.vpn.data.local.LastConnectStorage
import com.my.vpn.data.local.TrafficSessionStorage
import com.my.vpn.data.repository.ConfigRepository
import com.my.vpn.data.repository.ConfigTester

/**
 * Ручной DI-контейнер (Hilt отложен до совместимости с AGP 9.x).
 * Централизует создание зависимостей приложения.
 */
class AppContainer(context: Context) {

    val appSettingsStorage: AppSettingsStorage = AppSettingsStorage(context)
    val configStorage: ConfigStorage = ConfigStorage(context)
    val customSubscriptionStorage: CustomSubscriptionStorage = CustomSubscriptionStorage(context)
    val perServerBypassStorage: PerServerBypassStorage = PerServerBypassStorage(context)
    val trafficSessionStorage: TrafficSessionStorage = TrafficSessionStorage(context)
    val lastConnectStorage: LastConnectStorage = LastConnectStorage(context)

    val configRepository: ConfigRepository = ConfigRepository(
        configStorage,
        customSubscriptionStorage,
        appSettingsStorage,
        ConfigTester(),
        context.applicationContext
    )
}
