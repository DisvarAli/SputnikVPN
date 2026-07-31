package com.my.vpn.ui.viewmodel

import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.ConnectionDiagnostics
import com.my.vpn.data.model.CountryGroup
import com.my.vpn.data.model.CustomSubscription
import com.my.vpn.data.model.InstalledAppInfo
import com.my.vpn.data.model.SplitTunnelMode
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.data.model.TrafficSession
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.data.model.AppLanguage
import com.my.vpn.data.model.ConfigTestMode
enum class AppScreen {
    Onboarding,
    Main,
    ConfigPicker,
    Help,
    Menu,
    MenuAppearance,
    MenuSubscriptions,
    MenuAppUpdate,
    MenuAdvanced,
    Bypass
}

sealed class ListFilter {
    data object All : ListFilter()
    data class Builtin(val type: SubscriptionListType) : ListFilter()
    data class Custom(val id: String) : ListFilter()
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.Onboarding,
    val onboardingStep: Int = 0,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isPinging: Boolean = false,
    val isRecovering: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isDisconnecting: Boolean = false,
    val connectionButtonLabel: String? = null,
    val configs: List<VpnConfig> = emptyList(),
    val countryGroups: List<CountryGroup> = emptyList(),
    val selectedConfig: VpnConfig? = null,
    val connectedConfigId: String? = null,
    val totalInSubscription: Int = 0,
    val loadedSources: Int = 0,
    val sourceStats: Map<SubscriptionListType, Int> = emptyMap(),
    val builtinTraffic: Map<SubscriptionListType, SubscriptionTraffic> = emptyMap(),
    val customSubscriptions: List<CustomSubscription> = emptyList(),
    val lastUpdatedAt: Long = 0L,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val showSubscriptionsExpanded: Boolean = false,
    val listFilter: ListFilter = ListFilter.All,
    val loadingMessage: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showAppUpdateDialog: Boolean = false,
    val appReleaseInfo: com.my.vpn.update.AppReleaseInfo? = null,
    val appUpdateStatusMessage: String? = null,
    val subscriptionUpdateCheckMessage: String? = null,
    val showRestartDialog: Boolean = false,
    val showInstructionDialog: Boolean = false,
    val showAddSubscriptionDialog: Boolean = false,
    val showManageSubscriptionsDialog: Boolean = false,
    val pendingDeleteSubscriptionId: String? = null,
    val pendingDeleteSubscriptionTitle: String? = null,
    val addSubscriptionError: String? = null,
    val showAllConfigsSelected: Boolean = true,
    val protocolFilter: ConfigProtocol? = null,
    val showProtocolPickerExpanded: Boolean = false,
    val expandedCountries: Set<String> = emptySet(),
    val pingPending: Boolean = false,
    val vpnPermissionIntent: android.content.Intent? = null,
    val bypassProfile: ConnectionBypassProfile = ConnectionBypassProfile(),
    val bypassProbing: Boolean = false,
    val bypassStatusMessage: String? = null,
    val bypassProbeIndex: Int = 0,
    val bypassProbeTotal: Int = 0,
    val bypassProbeDetail: String? = null,
    val showConfigFetchBypassDialog: Boolean = false,
    val configTestMode: ConfigTestMode = ConfigTestMode.TCP_ONLY,
    val backgroundConfigUpdate: Boolean = true,
    val backgroundPingWifiOnly: Boolean = true,
    val apkDownloadProgress: Int? = null,
    val apkInstallIntent: android.content.Intent? = null,
    val tunnelDegraded: Boolean = false,
    val strictListProxyVerified: Boolean = false,
    val showAllFromSubscription: Boolean = true,
    val showUnconfirmedTunnelConfigs: Boolean = true,
    val autoFailoverEnabled: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val splitTunnelMode: SplitTunnelMode = SplitTunnelMode.FULL_TUNNEL,
    val splitTunnelPackages: String = "",
    val splitTunnelSelectedPackages: Set<String> = emptySet(),
    val showSplitTunnelPicker: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val splitTunnelAppFilter: String = "",
    val connectionDiagnostics: ConnectionDiagnostics? = null,
    val showDiagnosticsDialog: Boolean = false,
    val trafficSessions: List<TrafficSession> = emptyList(),
    val sessionStartedAtMs: Long = 0L
) {
    val displaySubscriptionTraffic: SubscriptionTraffic?
        get() {
            if (isConnected) {
                selectedConfig?.source?.let { builtinTraffic[it] }?.let { return it }
                selectedConfig?.customSourceId?.let { id ->
                    customSubscriptions.find { it.id == id }?.traffic?.let { return it }
                }
            }
            return when (val filter = listFilter) {
                is ListFilter.Builtin -> builtinTraffic[filter.type]
                is ListFilter.Custom -> customSubscriptions.find { it.id == filter.id }?.traffic
                ListFilter.All -> null
            }
        }

    val selectedListLabel: String
        get() {
            val base = when (val filter = listFilter) {
                ListFilter.All -> "Все конфиги"
                is ListFilter.Builtin -> filter.type.title
                is ListFilter.Custom -> customSubscriptions.find { it.id == filter.id }?.title ?: "Своя подписка"
            }
            val proto = protocolFilter?.displayName
            return if (proto != null) "$base · $proto" else base
        }

    val protocolFilterLabel: String
        get() = protocolFilter?.displayName ?: "Протокол"

    val filteredInstalledApps: List<InstalledAppInfo>
        get() {
            val q = splitTunnelAppFilter.trim().lowercase()
            if (q.isEmpty()) return installedApps
            return installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
}
