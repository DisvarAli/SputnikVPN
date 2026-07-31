package com.my.vpn.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.my.vpn.data.model.InstalledAppInfo
import java.util.concurrent.atomic.AtomicReference

object InstalledAppsProvider {

    private val cache = AtomicReference<List<InstalledAppInfo>?>(null)

    /** Список без иконок — быстро, без блокировки UI. */
    fun loadLaunchableApps(context: Context, forceRefresh: Boolean = false): List<InstalledAppInfo> {
        if (!forceRefresh) {
            cache.get()?.let { return it }
        }
        val pm = context.packageManager
        val self = context.packageName
        val list = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != self }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                InstalledAppInfo(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = null
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
        cache.set(list)
        return list
    }

    fun invalidateCache() {
        cache.set(null)
    }

    fun labelForPackage(context: Context, packageName: String): String =
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
