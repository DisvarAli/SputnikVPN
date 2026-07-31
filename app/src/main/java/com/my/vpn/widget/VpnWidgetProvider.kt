package com.my.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.my.vpn.MyVpnApp
import com.my.vpn.R
import com.my.vpn.vpn.VpnController
import kotlinx.coroutines.runBlocking

class VpnWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            if (VpnController.isConnected) {
                VpnController.disconnect(context, userRequested = true)
                notifyAllWidgets(context)
            } else {
                val connectIntent = Intent(context, VpnWidgetConnectReceiver::class.java).apply {
                    action = VpnWidgetConnectReceiver.ACTION_CONNECT
                }
                context.sendBroadcast(connectIntent)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.my.vpn.widget.ACTION_TOGGLE"

        fun notifyAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VpnWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val connected = VpnController.isConnected
            val app = context.applicationContext as? MyVpnApp
            val lastName = app?.let {
                runBlocking { it.lastConnectStorage.getConfigDisplayName() }
            }
            val views = RemoteViews(context.packageName, R.layout.widget_vpn_toggle)
            val statusText = when {
                connected -> context.getString(R.string.widget_status_connected)
                !lastName.isNullOrBlank() ->
                    context.getString(R.string.widget_status_last_server, lastName)
                else -> context.getString(R.string.widget_status_disconnected)
            }
            views.setTextViewText(R.id.widget_status, statusText)
            views.setTextViewText(
                R.id.widget_button,
                context.getString(
                    if (connected) R.string.widget_disconnect else R.string.widget_connect
                )
            )
            val toggleIntent = Intent(context, VpnWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_button, pending)
            return views
        }
    }
}
