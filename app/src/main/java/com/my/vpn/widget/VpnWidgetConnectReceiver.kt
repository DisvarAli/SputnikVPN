package com.my.vpn.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.my.vpn.MainActivity
import com.my.vpn.MyVpnApp
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.VpnPrepare
import com.my.vpn.vpn.VpnController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Подключение последнего сервера с виджета (при уже выданном VPN-разрешении).
 */
class VpnWidgetConnectReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CONNECT) return
        val pending = goAsync()
        scope.launch {
            try {
                handleConnect(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleConnect(context: Context) {
        if (VpnController.isConnected) {
            VpnController.disconnect(context, userRequested = true)
            VpnWidgetProvider.notifyAllWidgets(context)
            return
        }
        val app = context.applicationContext as? MyVpnApp ?: return
        val configId = app.lastConnectStorage.getConfigId() ?: run {
            openMain(context)
            return
        }
        val config = app.configRepository.loadCachedConfigs()
            .firstOrNull { it.id == configId }
            ?: run {
                openMain(context)
                return
            }
        if (VpnPrepare.prepareIntent(context) != null) {
            openMain(context)
            return
        }
        VpnController.connect(context, config)
        VpnWidgetProvider.notifyAllWidgets(context)
    }

    private fun openMain(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    companion object {
        const val ACTION_CONNECT = "com.my.vpn.widget.ACTION_CONNECT"
    }
}
