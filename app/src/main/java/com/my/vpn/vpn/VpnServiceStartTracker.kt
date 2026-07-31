package com.my.vpn.vpn

import java.util.concurrent.atomic.AtomicReference

/**
 * Состояние последнего запуска [MyVpnService] — чтобы UI не ждал таймаут, если TUN/ядро уже упали.
 */
object VpnServiceStartTracker {

    enum class Phase {
        IDLE,
        STARTING,
        INTERFACE_OK,
        FAILED
    }

    private val phase = AtomicReference(Phase.IDLE)
    private val failureReason = AtomicReference<String?>(null)

    fun reset() {
        phase.set(Phase.IDLE)
        failureReason.set(null)
    }

    fun markStarting() {
        phase.set(Phase.STARTING)
        failureReason.set(null)
    }

    fun markInterfaceOk() {
        phase.set(Phase.INTERFACE_OK)
    }

    fun markFailed(reason: String) {
        failureReason.set(reason)
        phase.set(Phase.FAILED)
    }

    fun hasFailed(): Boolean = phase.get() == Phase.FAILED

    fun lastFailureReason(): String? = failureReason.get()

    fun isPastInterface(): Boolean {
        val p = phase.get()
        return p == Phase.INTERFACE_OK || p == Phase.STARTING
    }
}
