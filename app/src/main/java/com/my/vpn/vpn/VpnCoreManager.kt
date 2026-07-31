package com.my.vpn.vpn

import android.app.Service
import com.my.vpn.AppConstants
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.ConnectLog
import kotlinx.coroutines.delay
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object VpnCoreManager {

    var pendingConfig: VpnConfig? = null

    private val coreStartupNotified = AtomicBoolean(false)
    private val coreStartFailed = AtomicBoolean(false)

    private val coreController: CoreController by lazy {
        CoreNativeManager.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long {
                coreStartupNotified.set(true)
                ConnectLog.stage("core_native", "startup_callback")
                return 0
            }

            override fun shutdown(): Long {
                coreStartupNotified.set(false)
                return 0
            }

            override fun onEmitStatus(l: Long, s: String?): Long = 0
        })
    }

    private var serviceRef: WeakReference<Service>? = null

    private val coreLock = Any()

    fun attachService(service: Service) {
        serviceRef = WeakReference(service)
        CoreNativeManager.initCoreEnv(service)
    }

    /** Прогрев native-контроллера при старте приложения (ускоряет первый connect). */
    fun warmup() {
        runCatching { coreController.isRunning }
    }

    fun isRunning(): Boolean = runCatching { coreController.isRunning }.getOrDefault(false)

    fun wasStartupNotified(): Boolean = coreStartupNotified.get()

    fun markCoreStartPending() {
        coreStartupNotified.set(false)
        coreStartFailed.set(false)
    }

    /**
     * Ждёт готовности Xray. [startLoop] асинхронный: isRunning() может отставать от GoLog/callback.
     */
    suspend fun awaitCoreRunning(
        timeoutMs: Long = AppConstants.VPN_CORE_START_TIMEOUT_MS,
        lateGraceMs: Long = AppConstants.VPN_CORE_LATE_START_GRACE_MS
    ): Boolean {
        if (isRunning()) return true

        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (VpnServiceStartTracker.hasFailed() || coreStartFailed.get()) return false
            if (isRunning()) return true
            if (coreStartupNotified.get()) return awaitRunningAfterStartup()
            delay(AppConstants.VPN_CORE_POLL_INTERVAL_MS)
        }

        if (isRunning()) return true
        if (coreStartupNotified.get()) return awaitRunningAfterStartup()

        val graceDeadline = System.currentTimeMillis() + lateGraceMs
        while (System.currentTimeMillis() < graceDeadline) {
            if (VpnServiceStartTracker.hasFailed() || coreStartFailed.get()) return false
            if (isRunning()) return true
            if (coreStartupNotified.get()) return awaitRunningAfterStartup()
            delay(AppConstants.VPN_CORE_POLL_INTERVAL_MS)
        }

        return isRunning()
    }

    private suspend fun awaitRunningAfterStartup(): Boolean {
        repeat(50) {
            if (VpnServiceStartTracker.hasFailed()) return false
            if (isRunning()) return true
            delay(80)
        }
        return isRunning()
    }

    fun startCore(vpnInterfaceFd: Int): Boolean {
        val config = pendingConfig ?: return false
        val json = V2RayConfigBuilder.buildVpnConfig(config, BypassProfileHolder.profile)

        synchronized(coreLock) {
            runCatching { coreController.stopLoop() }
            markCoreStartPending()
            Thread.sleep(80)
        }

        return runCatching {
            coreController.startLoop(json, vpnInterfaceFd)
            ConnectLog.stage("core_native", "startLoop_ok")
            true
        }.getOrElse { error ->
            coreStartFailed.set(true)
            VpnServiceStartTracker.markFailed("startLoop:${error.message}")
            ConnectLog.w("startLoop failed", error)
            false
        }
    }

    fun restartCore(vpnInterfaceFd: Int): Boolean = startCore(vpnInterfaceFd)

    fun stopCore() = synchronized(coreLock) {
        runCatching { coreController.stopLoop() }
        coreStartupNotified.set(false)
    }

    fun stopCoreBlocking() = synchronized(coreLock) {
        runCatching { coreController.stopLoop() }
        coreStartupNotified.set(false)
    }

    fun stopCoreFully() = synchronized(coreLock) {
        repeat(20) {
            runCatching { coreController.stopLoop() }
            coreStartupNotified.set(false)
            if (!isRunning()) return@synchronized
            Thread.sleep(50)
        }
    }
}
