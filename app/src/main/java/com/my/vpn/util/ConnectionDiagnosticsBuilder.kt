package com.my.vpn.util

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConnectionDiagnostics
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.vpn.VpnCoreManager

object ConnectionDiagnosticsBuilder {

    fun fromConnectFailure(
        config: VpnConfig,
        tcpReachable: Boolean?,
        tunnelHealthy: Boolean?,
        error: String? = null,
        coreRunning: Boolean = VpnCoreManager.isRunning()
    ): ConnectionDiagnostics {
        val label = "${config.country} · ${config.protocol.displayName}"
        val summary = buildString {
            append(error ?: "Подключение не удалось")
            append(" · core=${if (coreRunning) "ok" else "нет"}")
            tcpReachable?.let { append(" · TCP=${if (it) "ok" else "fail"}") }
            tunnelHealthy?.let { append(" · туннель=${if (it) "ok" else "fail"}") }
        }
        val suggestion = when {
            !config.connectSupported ->
                "Этот конфиг не поддерживается. Выберите VLESS/VMess/Trojan/SS/Hy2."
            tcpReachable == false -> "Порт недоступен. Обновите пинг или выберите сервер с меньшей задержкой."
            tunnelHealthy == false && coreRunning ->
                "Туннель не прошёл проверку. Выберите другой сервер или включите полный режим проверки."
            !coreRunning && tunnelHealthy != true ->
                "Ядро Xray ещё не готово. Подождите 10–20 с и повторите, либо перезапустите приложение."
            tunnelHealthy == false ->
                "Туннель не прошёл проверку. Выберите другой сервер и обновите список."
            else -> "Обновите пинг, выберите другой сервер или режим «Полный (TCP + VPN-тест)»."
        }
        return ConnectionDiagnostics(
            serverLabel = label,
            protocol = config.protocol.displayName,
            tcpReachable = tcpReachable,
            proxyVerified = config.proxyVerified,
            coreRunning = coreRunning,
            tunnelHealthy = tunnelHealthy,
            summary = summary,
            suggestion = suggestion
        )
    }
}
