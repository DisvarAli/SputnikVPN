package com.my.vpn.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.my.vpn.MainActivity
import com.my.vpn.MyVpnApp
import com.my.vpn.data.local.OnboardingStorage
import com.my.vpn.data.model.AppLanguage
import com.my.vpn.data.model.ConfigProtocol
import com.my.vpn.data.model.CountryGroup
import com.my.vpn.data.model.CustomSubscription
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.ConnectionDiagnostics
import com.my.vpn.data.model.SplitTunnelMode
import com.my.vpn.data.model.DnsBypassMode
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.SniBypassMode
import com.my.vpn.data.model.TtlBypassMode
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.util.BypassAutoPicker
import com.my.vpn.util.BypassProber
import com.my.vpn.util.BypassPresets
import com.my.vpn.util.VpnPrepare
import com.my.vpn.vpn.BypassProfileHolder
import com.my.vpn.ui.theme.AppThemeMode
import com.my.vpn.data.repository.ConfigPingScope
import com.my.vpn.data.repository.ConfigRepository
import com.my.vpn.AppConstants
import com.my.vpn.R
import com.my.vpn.update.AppReleaseChecker
import com.my.vpn.util.ConfigFilters
import com.my.vpn.util.ConnectLog
import com.my.vpn.util.ConnectionDiagnosticsBuilder
import com.my.vpn.util.CountryNames
import com.my.vpn.vpn.SplitTunnelHolder
import com.my.vpn.util.AppStrings
import com.my.vpn.util.LocaleHelper
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.update.AppUpdateDownloader
import com.my.vpn.util.ApkInstallUtil
import com.my.vpn.data.local.TrafficSessionStorage
import com.my.vpn.data.model.TrafficSession
import com.my.vpn.util.ConfigListProcessor
import com.my.vpn.util.InstalledAppsProvider
import com.my.vpn.util.VpnTrafficMonitor
import com.my.vpn.vpn.TunnelConnectivityChecker
import com.my.vpn.BuildConfig
import com.my.vpn.vpn.V2RayConfigBuilder
import com.my.vpn.vpn.VpnController
import com.my.vpn.domain.ConnectVpnUseCase
import com.my.vpn.vpn.VpnCoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyVpnApp
    private val repository: ConfigRepository = app.configRepository
    private val appSettings = app.appSettingsStorage
    private val onboardingStorage = OnboardingStorage(application)
    private val trafficSessionStorage = TrafficSessionStorage(application)
    private val connectVpnUseCase = ConnectVpnUseCase()
    private val strings: AppStrings by lazy { AppStrings(getApplication()) }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _requestActivityRecreate = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestActivityRecreate: SharedFlow<Unit> = _requestActivityRecreate.asSharedFlow()

    private val failedConfigIds = mutableSetOf<String>()
    private var healthMonitorJob: Job? = null
    private var recoveryJob: Job? = null
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var applyListFilterJob: Job? = null
    private var connectJob: Job? = null
    private var helpReturnScreen: AppScreen = AppScreen.Main

    init {
        viewModelScope.launch {
            val completed = onboardingStorage.isCompleted()
            if (completed) {
                val lastUpdated = repository.getLastUpdatedAt()
                val customSubs = repository.loadCustomSubscriptions()
                _uiState.update {
                    it.copy(
                        currentScreen = AppScreen.Main,
                        lastUpdatedAt = lastUpdated,
                        customSubscriptions = customSubs
                    )
                }
                bootstrap()
                checkAppUpdateSilent()
            } else {
                _uiState.update { it.copy(currentScreen = AppScreen.Onboarding, onboardingStep = 0) }
            }
        }
        startTrafficPolling()
        viewModelScope.launch {
            appSettings.bypassProfileFlow.collect { profile ->
                BypassProfileHolder.profile = profile
                _uiState.update { it.copy(bypassProfile = profile) }
            }
        }
        viewModelScope.launch {
            appSettings.configTestModeFlow.collect { mode ->
                _uiState.update { it.copy(configTestMode = mode) }
            }
        }
        viewModelScope.launch {
            // Обходы РКН / split tunnel сброшены к безопасным значениям по умолчанию
            SplitTunnelHolder.mode = SplitTunnelMode.FULL_TUNNEL
            SplitTunnelHolder.allowedPackages = emptySet()
            BypassProfileHolder.profile = ConnectionBypassProfile(splitRuDirect = false)
            appSettings.setSplitTunnelMode(SplitTunnelMode.FULL_TUNNEL)

            val packages = emptySet<String>()
            _uiState.update {
                it.copy(
                    backgroundConfigUpdate = appSettings.isBackgroundConfigUpdateEnabled(),
                    backgroundPingWifiOnly = appSettings.isBackgroundPingWifiOnly(),
                    strictListProxyVerified = appSettings.isStrictListProxyVerified(),
                    showAllFromSubscription = appSettings.isShowAllFromSubscription(),
                    showUnconfirmedTunnelConfigs = appSettings.isShowUnconfirmedTunnelConfigs(),
                    autoFailoverEnabled = appSettings.isAutoFailoverEnabled(),
                    appLanguage = appSettings.getAppLanguage(),
                    splitTunnelMode = SplitTunnelMode.FULL_TUNNEL,
                    splitTunnelPackages = "",
                    splitTunnelSelectedPackages = packages,
                    trafficSessions = trafficSessionStorage.getSessions(),
                    bypassProfile = ConnectionBypassProfile(splitRuDirect = false)
                )
            }
        }
        viewModelScope.launch {
            trafficSessionStorage.sessionsFlow.collect { sessions ->
                _uiState.update { it.copy(trafficSessions = sessions) }
            }
        }
        viewModelScope.launch {
            appSettings.strictListProxyVerifiedFlow.collect { strict ->
                _uiState.update { it.copy(strictListProxyVerified = strict) }
            }
        }
        viewModelScope.launch {
            appSettings.showAllFromSubscriptionFlow.collect { showAll ->
                _uiState.update { it.copy(showAllFromSubscription = showAll) }
                applyListFilter(_uiState.value.listFilter)
            }
        }
        viewModelScope.launch {
            appSettings.showUnconfirmedTunnelConfigsFlow.collect { showUnconfirmed ->
                _uiState.update { it.copy(showUnconfirmedTunnelConfigs = showUnconfirmed) }
                applyListFilter(_uiState.value.listFilter)
            }
        }
        viewModelScope.launch {
            appSettings.autoFailoverFlow.collect { enabled ->
                _uiState.update { it.copy(autoFailoverEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appSettings.appLanguageFlow.collect { lang ->
                _uiState.update { it.copy(appLanguage = lang) }
            }
        }
    }

    fun onboardingNext() {
        _uiState.update { it.copy(onboardingStep = (it.onboardingStep + 1).coerceAtMost(2)) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingStorage.setCompleted(true)
            val seenReminder = onboardingStorage.hasSeenInstructionReminder()
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.Main,
                    onboardingStep = 0,
                    showInstructionDialog = !seenReminder
                )
            }
            bootstrap()
        }
    }

    fun dismissInstructionDialog(openHelp: Boolean = false) {
        viewModelScope.launch {
            onboardingStorage.setInstructionReminderSeen(true)
            _uiState.update {
                it.copy(
                    showInstructionDialog = false,
                    currentScreen = if (openHelp) AppScreen.Help else it.currentScreen
                )
            }
        }
    }

    fun bootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.initialLoad(onProgress = ::updateProgress)) {
                is ConfigRepository.RefreshResult.Success -> {
                    applySuccess(result)
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            statusMessage = strings.get(R.string.msg_load_configs_hint),
                            showConfigFetchBypassDialog = true
                        )
                    }
                    startConfigRecovery(silent = true)
                }
                else -> Unit
            }
        }
    }

    fun fullRefreshFromMenu() {
        recoveryJob?.cancel()
        pingJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdating = true, errorMessage = null, loadingMessage = strings.get(R.string.msg_loading_all_subs))
            }
            when (val result = repository.fullRefreshFromRemote(onProgress = ::updateProgress)) {
                is ConfigRepository.RefreshResult.Success -> {
                    failedConfigIds.clear()
                    applySuccess(result)
                    _uiState.update { it.copy(statusMessage = strings.get(R.string.msg_cache_updated)) }
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = result.message,
                            showConfigFetchBypassDialog = true
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun repingAllCached() {
        pingJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isPinging = true, loadingMessage = strings.get(R.string.msg_pinging), errorMessage = null)
            }
            when (val result = repository.repingCachedConfigs(onProgress = ::updateProgress)) {
                is ConfigRepository.RefreshResult.Success -> {
                    applySuccess(result)
                    _uiState.update { it.copy(statusMessage = strings.get(R.string.msg_ping_updated)) }
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> Unit
            }
            _uiState.update { it.copy(isPinging = false, isUpdating = false, isLoading = false, pingPending = false) }
            applyListFilter(_uiState.value.listFilter)
        }
    }

    /** Pull-to-refresh на главном экране: только выбранная подписка, не весь кэш. */
    fun onMainPullRefresh() {
        pingJob?.cancel()
        viewModelScope.launch {
            when (val filter = _uiState.value.listFilter) {
                is ListFilter.Custom -> {
                    val sub = _uiState.value.customSubscriptions.find { it.id == filter.id }
                        ?: return@launch
                    _uiState.update {
                        it.copy(
                            isPinging = true,
                            isUpdating = true,
                            pingPending = true,
                            loadingMessage = strings.get(R.string.sub_loading_named, sub.title),
                            errorMessage = null
                        )
                    }
                    when (
                        val result = repository.refreshCustomSubscriptionFromRemote(
                            sub,
                            onProgress = ::updateProgress,
                            deferPing = false
                        )
                    ) {
                        is ConfigRepository.RefreshResult.Success -> {
                            failedConfigIds.clear()
                            applySuccess(result)
                            _uiState.update {
                                it.copy(statusMessage = strings.get(R.string.sub_cached_named, sub.title))
                            }
                            applyListFilter(ListFilter.Custom(sub.id))
                        }
                        is ConfigRepository.RefreshResult.Error -> {
                            _uiState.update { it.copy(errorMessage = result.message) }
                        }
                        else -> Unit
                    }
                    _uiState.update {
                        it.copy(isPinging = false, isUpdating = false, isLoading = false, pingPending = false)
                    }
                }
                is ListFilter.Builtin -> {
                    _uiState.update {
                        it.copy(isPinging = true, loadingMessage = strings.get(R.string.msg_pinging), errorMessage = null)
                    }
                    val scope = ConfigPingScope.Builtin(filter.type)
                    when (val result = repository.repingInScope(scope, onProgress = ::updateProgress)) {
                        is ConfigRepository.RefreshResult.Success -> {
                            applySuccess(result)
                            _uiState.update { it.copy(statusMessage = strings.get(R.string.msg_ping_updated)) }
                        }
                        is ConfigRepository.RefreshResult.Error -> {
                            _uiState.update { it.copy(errorMessage = result.message) }
                        }
                        else -> Unit
                    }
                    _uiState.update { it.copy(isPinging = false, isUpdating = false, pingPending = false) }
                    applyListFilter(filter)
                }
                ListFilter.All -> {
                    _uiState.update {
                        it.copy(
                            isPinging = true,
                            loadingMessage = strings.get(R.string.msg_pinging),
                            errorMessage = null
                        )
                    }
                    when (val result = repository.repingCachedConfigs(onProgress = ::updateProgress)) {
                        is ConfigRepository.RefreshResult.Success -> {
                            applySuccess(result)
                            _uiState.update { it.copy(statusMessage = strings.get(R.string.msg_ping_updated)) }
                        }
                        is ConfigRepository.RefreshResult.Error -> {
                            _uiState.update { it.copy(errorMessage = result.message) }
                        }
                        else -> Unit
                    }
                    _uiState.update {
                        it.copy(isPinging = false, isUpdating = false, isLoading = false, pingPending = false)
                    }
                    applyListFilter(filter)
                }
            }
        }
    }

    private fun pingScopeForListFilter(filter: ListFilter): ConfigPingScope? = when (filter) {
        is ListFilter.Custom -> ConfigPingScope.Custom(filter.id)
        is ListFilter.Builtin -> ConfigPingScope.Builtin(filter.type)
        ListFilter.All -> null
    }

    private fun updateProgress(message: String) {
        _uiState.update { it.copy(loadingMessage = message) }
    }

    fun onboardingBack() {
        _uiState.update { it.copy(onboardingStep = (it.onboardingStep - 1).coerceAtLeast(0)) }
    }

    fun handleSystemBack(): Boolean {
        val state = _uiState.value
        return when (state.currentScreen) {
            AppScreen.Help -> {
                closeHelp()
                true
            }
            AppScreen.Bypass -> {
                closeBypass()
                true
            }
            AppScreen.ConfigPicker -> {
                closeConfigPicker()
                true
            }
            AppScreen.MenuAppearance -> {
                closeMenuAppearance()
                true
            }
            AppScreen.MenuSubscriptions -> {
                when {
                    state.showAddSubscriptionDialog -> {
                        dismissAddSubscriptionDialog()
                        true
                    }
                    state.showManageSubscriptionsDialog -> {
                        dismissManageSubscriptionsDialog()
                        true
                    }
                    state.pendingDeleteSubscriptionId != null -> {
                        dismissDeleteSubscriptionConfirm()
                        true
                    }
                    else -> {
                        closeMenuSubscriptions()
                        true
                    }
                }
            }
            AppScreen.MenuAppUpdate -> {
                closeMenuAppUpdate()
                true
            }
            AppScreen.MenuAdvanced -> {
                when {
                    state.showDeleteConfirmDialog -> {
                        dismissDeleteConfirm()
                        true
                    }
                    state.showSplitTunnelPicker -> {
                        closeSplitTunnelPicker()
                        true
                    }
                    else -> {
                        closeMenuAdvanced()
                        true
                    }
                }
            }
            AppScreen.Menu -> {
                when {
                    state.showAddSubscriptionDialog -> {
                        dismissAddSubscriptionDialog()
                        true
                    }
                    state.showManageSubscriptionsDialog -> {
                        dismissManageSubscriptionsDialog()
                        true
                    }
                    state.pendingDeleteSubscriptionId != null -> {
                        dismissDeleteSubscriptionConfirm()
                        true
                    }
                    state.showDeleteConfirmDialog -> {
                        dismissDeleteConfirm()
                        true
                    }
                    else -> {
                        closeAppMenu()
                        true
                    }
                }
            }
            AppScreen.Onboarding -> {
                if (state.onboardingStep > 0) onboardingBack()
                true
            }
            AppScreen.Main -> {
                when {
                    state.showAddSubscriptionDialog -> {
                        dismissAddSubscriptionDialog()
                        true
                    }
                    state.showManageSubscriptionsDialog -> {
                        dismissManageSubscriptionsDialog()
                        true
                    }
                    state.pendingDeleteSubscriptionId != null -> {
                        dismissDeleteSubscriptionConfirm()
                        true
                    }
                    state.showInstructionDialog -> {
                        dismissInstructionDialog(openHelp = false)
                        true
                    }
                    state.showRestartDialog -> {
                        dismissRestartDialog()
                        true
                    }
                    state.showDeleteConfirmDialog -> {
                        dismissDeleteConfirm()
                        true
                    }
                    state.showSubscriptionsExpanded -> {
                        toggleSubscriptions()
                        true
                    }
                    else -> true
                }
            }
        }
    }

    fun selectSubscriptionFilter(type: SubscriptionListType) {
        applyListFilter(ListFilter.Builtin(type), collapseIfSelected = true)
    }

    fun selectCustomSubscription(id: String) {
        if (_uiState.value.customSubscriptions.none { it.id == id }) return
        applyListFilter(ListFilter.Custom(id), collapseIfSelected = true)
    }

    fun showAllConfigs() {
        applyListFilter(ListFilter.All, collapseIfSelected = true)
        _uiState.update {
            it.copy(
                showAllConfigsSelected = true,
                showSubscriptionsExpanded = false,
                showProtocolPickerExpanded = false,
                statusMessage = null
            )
        }
    }

    fun toggleProtocolPicker() {
        _uiState.update {
            it.copy(
                showProtocolPickerExpanded = !it.showProtocolPickerExpanded,
                showSubscriptionsExpanded = false
            )
        }
    }

    fun selectProtocolFilter(protocol: ConfigProtocol?) {
        val state = _uiState.value
        _uiState.update { it.copy(protocolFilter = protocol, showProtocolPickerExpanded = false) }
        applyListFilter(state.listFilter)
    }

    fun openBypass() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun closeBypass() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun setDnsBypassMode(mode: DnsBypassMode) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dnsMode = mode)) }
    }

    fun setSniBypassMode(mode: SniBypassMode) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(sniMode = mode)) }
    }

    fun setDnsDirectDoh(url: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dnsDirectDoh = url)) }
    }

    fun setDnsProxyDoh(url: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dnsProxyDoh = url)) }
    }

    fun setManualSni(value: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(manualSni = value)) }
    }

    fun setSplitRuDirect(enabled: Boolean) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(splitRuDirect = enabled)) }
    }

    fun setDpiBypassMode(mode: DpiBypassMode) {
        val preset = if (mode != DpiBypassMode.OFF && mode != DpiBypassMode.MANUAL) {
            BypassPresets.resolveDpiPreset(null, mode)?.id
        } else null
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dpiMode = mode, dpiPresetId = preset)) }
    }

    fun setDpiPreset(id: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dpiPresetId = id, dpiMode = DpiBypassMode.MANUAL)) }
    }

    fun setDpiPackets(v: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dpiPackets = v)) }
    }

    fun setDpiLength(v: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dpiLength = v)) }
    }

    fun setDpiInterval(v: String) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(dpiInterval = v)) }
    }

    fun setTtlBypassMode(mode: TtlBypassMode) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(ttlMode = mode)) }
    }

    fun setManualTtl(value: Int) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(manualTtl = value.coerceIn(1, 255))) }
    }

    fun setMuxPadding(enabled: Boolean) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(muxPadding = enabled)) }
    }

    fun setTcpNoDelay(enabled: Boolean) {
        _uiState.update { it.copy(bypassProfile = it.bypassProfile.copy(tcpNoDelay = enabled)) }
    }

    fun setStrictListProxyVerified(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setStrictListProxyVerified(enabled)
            _uiState.update { it.copy(strictListProxyVerified = enabled) }
            applyListFilter(_uiState.value.listFilter)
        }
    }

    fun setShowAllFromSubscription(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setShowAllFromSubscription(enabled)
            _uiState.update { it.copy(showAllFromSubscription = enabled) }
            applyListFilter(_uiState.value.listFilter)
        }
    }

    fun setShowUnconfirmedTunnelConfigs(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setShowUnconfirmedTunnelConfigs(enabled)
            _uiState.update {
                it.copy(
                    showUnconfirmedTunnelConfigs = enabled,
                    strictListProxyVerified = if (enabled) false else it.strictListProxyVerified
                )
            }
            applyListFilter(_uiState.value.listFilter)
        }
    }

    fun setAutoFailoverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setAutoFailoverEnabled(enabled)
            _uiState.update { it.copy(autoFailoverEnabled = enabled) }
        }
    }

    fun setSplitTunnelMode(mode: SplitTunnelMode) {
        viewModelScope.launch {
            appSettings.setSplitTunnelMode(mode)
            SplitTunnelHolder.mode = mode
            _uiState.update { it.copy(splitTunnelMode = mode) }
        }
    }

    fun setSplitTunnelPackagesInput(text: String) {
        _uiState.update { it.copy(splitTunnelPackages = text) }
    }

    fun saveSplitTunnelPackages() {
        viewModelScope.launch {
            val manual = _uiState.value.splitTunnelPackages
                .split(',', ';', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            val packages = manual.ifEmpty { _uiState.value.splitTunnelSelectedPackages }
            appSettings.setSplitTunnelPackages(packages)
            SplitTunnelHolder.allowedPackages = packages
            _uiState.update {
                it.copy(
                    splitTunnelSelectedPackages = packages,
                    splitTunnelPackages = packages.joinToString(", ")
                )
            }
        }
    }

    fun dismissDiagnosticsDialog() {
        _uiState.update { it.copy(showDiagnosticsDialog = false, connectionDiagnostics = null) }
    }

    fun autoPickFullBypass() {
        viewModelScope.launch {
            val config = probeTargetConfig()
            _uiState.update {
                it.copy(
                    bypassProbing = true,
                    bypassProbeIndex = 0,
                    bypassProbeTotal = 0,
                    bypassProbeDetail = strings.get(R.string.bypass_probe_start)
                )
            }
            val profile = BypassAutoPicker.pickFullAll(config, ::updateBypassProgress)
            appSettings.saveBypassProfile(profile)
            BypassProfileHolder.profile = profile
            _uiState.update {
                it.copy(
                    bypassProfile = profile,
                    bypassProbing = false,
                    bypassStatusMessage = strings.get(R.string.bypass_probe_done),
                    bypassProbeDetail = null
                )
            }
        }
    }

    fun dismissConfigFetchBypassDialog() {
        _uiState.update { it.copy(showConfigFetchBypassDialog = false) }
    }

    /** Перебор зеркал/белых списков РФ для загрузки конфигов без VPN. */
    fun startConfigFetchBypassAutoPick() {
        viewModelScope.launch {
            dismissConfigFetchBypassDialog()
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    bypassProbing = true,
                    loadingMessage = strings.get(R.string.bypass_fetch_loading),
                    errorMessage = null
                )
            }
            when (val result = repository.fullRefreshViaWhiteListBypass(
                onIndexedProgress = ::updateBypassIndexedProgress,
                onProgress = ::updateProgress
            )) {
                is ConfigRepository.RefreshResult.Success -> {
                    failedConfigIds.clear()
                    applySuccess(result)
                    _uiState.update {
                        it.copy(
                            statusMessage = strings.get(R.string.bypass_fetch_done),
                            bypassProbing = false
                        )
                    }
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            bypassProbing = false,
                            errorMessage = result.message,
                            showConfigFetchBypassDialog = true
                        )
                    }
                }
                else -> _uiState.update { it.copy(isUpdating = false, bypassProbing = false) }
            }
        }
    }

    private suspend fun refreshConfigsAfterBypass(profile: ConnectionBypassProfile) {
        updateBypassProgress(BypassAutoPicker.Progress(0, 1, strings.get(R.string.bypass_ping_servers)))
        when (val result = repository.repingCachedWithBypass(profile, ::updateBypassIndexedProgress)) {
            is ConfigRepository.RefreshResult.Success -> {
                _uiState.update {
                    it.copy(
                        configs = result.configs,
                        countryGroups = result.groups,
                        pingPending = result.pingPending
                    )
                }
                applyListFilter(_uiState.value.listFilter)
            }
            is ConfigRepository.RefreshResult.Error ->
                _uiState.update { it.copy(bypassStatusMessage = result.message) }
            else -> Unit
        }
    }

    private fun probeTargetConfig(): VpnConfig? {
        _uiState.value.selectedConfig?.let { return it }
        return _uiState.value.configs
            .filter { it.connectSupported }
            .minByOrNull { it.pingMs ?: Long.MAX_VALUE }
    }

    fun autoPickDnsBypass() {
        viewModelScope.launch {
            _uiState.update { it.copy(bypassProbing = true) }
            val (direct, proxy) = BypassAutoPicker.pickDnsWithProgress(::updateBypassProgress)
            val updated = _uiState.value.bypassProfile.copy(
                dnsMode = DnsBypassMode.AUTO,
                dnsDirectDoh = direct ?: BypassPresets.directDohServers.first().url,
                dnsProxyDoh = proxy ?: BypassPresets.proxyDohServers.first().url
            )
            _uiState.update {
                it.copy(
                    bypassProfile = updated,
                    bypassProbing = false,
                    bypassStatusMessage = "DNS подобран"
                )
            }
            probeTargetConfig()?.let { cfg ->
                if (BypassAutoPicker.testConfigWithProfile(cfg, updated)) {
                    refreshConfigsAfterBypass(updated)
                }
            }
        }
    }

    fun autoPickSniForSelectedConfig() {
        val config = probeTargetConfig() ?: run {
            _uiState.update { it.copy(errorMessage = strings.get(R.string.bypass_pick_server_sni)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(bypassProbing = true) }
            val sni = BypassAutoPicker.pickSniWithProgress(config, ::updateBypassProgress)
            val updated = _uiState.value.bypassProfile.copy(
                sniMode = if (sni != null) SniBypassMode.AUTO else SniBypassMode.OFF,
                selectedSni = sni
            )
            _uiState.update {
                it.copy(
                    bypassProfile = updated,
                    bypassProbing = false,
                    bypassStatusMessage = if (sni != null) "SNI: $sni" else "SNI не найден"
                )
            }
            if (sni != null && BypassAutoPicker.testConfigWithProfile(config, updated)) {
                refreshConfigsAfterBypass(updated)
            }
        }
    }

    fun saveBypassAndApply() {
        viewModelScope.launch {
            val profile = _uiState.value.bypassProfile
            appSettings.saveBypassProfile(profile)
            BypassProfileHolder.profile = profile
            refreshConfigsAfterBypass(profile)
            _uiState.update {
                it.copy(
                    bypassStatusMessage = strings.get(R.string.bypass_saved_applied),
                    currentScreen = AppScreen.Menu
                )
            }
            if (_uiState.value.isConnected) {
                _uiState.value.selectedConfig?.let { reconnectWithBypass(it) }
            }
        }
    }

    private fun updateBypassProgress(progress: BypassAutoPicker.Progress) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update {
                it.copy(
                    bypassProbeIndex = progress.index,
                    bypassProbeTotal = progress.total,
                    bypassProbeDetail = progress.label,
                    bypassStatusMessage = strings.get(
                        R.string.bypass_progress,
                        progress.index,
                        progress.total,
                        progress.label
                    )
                )
            }
        }
    }

    private fun updateBypassIndexedProgress(index: Int, total: Int, label: String) {
        updateBypassProgress(BypassAutoPicker.Progress(index, total, label))
    }

    private fun reconnectWithBypass(config: VpnConfig) {
        BypassProfileHolder.profile = _uiState.value.bypassProfile
        VpnController.reconnect(getApplication(), config)
    }

    fun setUiScaleLevel(level: Int) {
        viewModelScope.launch {
            appSettings.setUiScaleLevel(level)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            appSettings.setThemeMode(mode)
        }
    }

    fun addCustomSubscriptionFromImport(title: String, content: String) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()
            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(addSubscriptionError = strings.get(R.string.sub_enter_title)) }
                return@launch
            }
            if (trimmedContent.isBlank()) {
                _uiState.update { it.copy(addSubscriptionError = strings.get(R.string.sub_empty_file_qr)) }
                return@launch
            }
            runCatching {
                repository.addCustomSubscriptionFromContent(trimmedTitle, trimmedContent)
            }.onSuccess { sub ->
                val subs = repository.loadCustomSubscriptions()
                _uiState.update {
                    it.copy(
                        showAddSubscriptionDialog = false,
                        addSubscriptionError = null,
                        customSubscriptions = subs,
                        statusMessage = strings.get(R.string.sub_added_named, sub.title)
                    )
                }
                refreshCustomSubscriptionFromMenu(sub)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(addSubscriptionError = e.message ?: strings.get(R.string.sub_import_failed))
                }
            }
        }
    }

    fun openAppMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun closeAppMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.Main) }
    }

    fun openMenuAppearance() {
        _uiState.update { it.copy(currentScreen = AppScreen.MenuAppearance) }
    }

    fun closeMenuAppearance() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun openMenuSubscriptions() {
        _uiState.update { it.copy(currentScreen = AppScreen.MenuSubscriptions) }
    }

    fun closeMenuSubscriptions() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun openMenuAppUpdate() {
        _uiState.update { it.copy(currentScreen = AppScreen.MenuAppUpdate) }
    }

    fun closeMenuAppUpdate() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun openMenuAdvanced() {
        _uiState.update { it.copy(currentScreen = AppScreen.MenuAdvanced) }
    }

    fun closeMenuAdvanced() {
        _uiState.update { it.copy(currentScreen = AppScreen.Menu) }
    }

    fun setStatusMessage(message: String?) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun checkSubscriptionUpdatesManual() {
        viewModelScope.launch {
            _uiState.update { it.copy(subscriptionUpdateCheckMessage = strings.get(R.string.sub_checking)) }
            val has = repository.peekSubscriptionChangesOnRemote()
            _uiState.update {
                it.copy(
                    subscriptionUpdateCheckMessage = if (has) {
                        strings.get(R.string.sub_github_changed)
                    } else {
                        strings.get(R.string.sub_github_match)
                    }
                )
            }
        }
    }

    fun checkAppUpdateManual() {
        viewModelScope.launch { runAppUpdateCheck(showDialogIfNew = true, force = true) }
    }

    private fun checkAppUpdateSilent() {
        viewModelScope.launch { runAppUpdateCheck(showDialogIfNew = true, force = false) }
    }

    private suspend fun runAppUpdateCheck(showDialogIfNew: Boolean, force: Boolean) {
        if (!force && appSettings.isAppUpdateNeverPrompt()) {
            _uiState.update {
                it.copy(appUpdateStatusMessage = strings.get(R.string.msg_app_update_check_disabled))
            }
            return
        }
        if (!force && System.currentTimeMillis() < appSettings.getAppUpdatePostponeUntil()) {
            return
        }
        _uiState.update { it.copy(appUpdateStatusMessage = strings.get(R.string.msg_app_update_checking)) }
        val release = AppReleaseChecker.fetchLatestRelease()
        if (release == null) {
            _uiState.update {
                it.copy(
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_github_fail),
                    appReleaseInfo = null
                )
            }
            return
        }
        val installed = BuildConfig.VERSION_NAME
        val newer = AppReleaseChecker.isNewerThanInstalled(release.versionName, installed)
        val dismissed = appSettings.getDismissedAppVersion()
        if (!newer) {
            _uiState.update {
                it.copy(
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_current, installed),
                    appReleaseInfo = release,
                    showAppUpdateDialog = false
                )
            }
            return
        }
        if (!force && dismissed == release.versionName) {
            _uiState.update {
                it.copy(
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_dismissed, release.versionName),
                    appReleaseInfo = release
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                appReleaseInfo = release,
                appUpdateStatusMessage = strings.get(R.string.msg_app_update_available, release.versionName, installed),
                showAppUpdateDialog = showDialogIfNew
            )
        }
    }

    fun dismissAppUpdateDialog(markDismissed: Boolean) {
        viewModelScope.launch {
            val version = _uiState.value.appReleaseInfo?.versionName
            if (markDismissed && version != null) {
                appSettings.setDismissedAppVersion(version)
            }
            _uiState.update { it.copy(showAppUpdateDialog = false) }
        }
    }

    fun postponeAppUpdate() {
        viewModelScope.launch {
            appSettings.postponeAppUpdate(AppConstants.APP_UPDATE_POSTPONE_DAYS)
            _uiState.update {
                it.copy(
                    showAppUpdateDialog = false,
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_postponed)
                )
            }
        }
    }

    fun neverPromptAppUpdate() {
        viewModelScope.launch {
            appSettings.setAppUpdateNeverPrompt(true)
            _uiState.update {
                it.copy(
                    showAppUpdateDialog = false,
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_auto_off)
                )
            }
        }
    }

    fun downloadAndInstallAppUpdate() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    apkDownloadProgress = 0,
                    appUpdateStatusMessage = strings.get(R.string.msg_app_update_downloading)
                )
            }
            val result = AppUpdateDownloader.downloadLatestApk(getApplication()) { pct ->
                _uiState.update { it.copy(apkDownloadProgress = pct) }
            }
            result.fold(
                onSuccess = { file ->
                    val intent = ApkInstallUtil.installApk(getApplication(), file)
                    _uiState.update {
                        it.copy(
                            apkDownloadProgress = null,
                            apkInstallIntent = intent,
                            appUpdateStatusMessage = strings.get(R.string.msg_app_update_open_installer)
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            apkDownloadProgress = null,
                            errorMessage = e.message ?: strings.get(R.string.msg_app_update_download_error)
                        )
                    }
                }
            )
        }
    }

    fun clearApkInstallIntent() {
        _uiState.update { it.copy(apkInstallIntent = null) }
    }

    fun setConfigTestMode(mode: ConfigTestMode) {
        viewModelScope.launch {
            appSettings.setConfigTestMode(mode)
        }
    }

    fun setBackgroundConfigUpdate(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setBackgroundConfigUpdateEnabled(enabled)
            _uiState.update { it.copy(backgroundConfigUpdate = enabled) }
        }
    }

    fun setBackgroundPingWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            appSettings.setBackgroundPingWifiOnly(wifiOnly)
            _uiState.update { it.copy(backgroundPingWifiOnly = wifiOnly) }
        }
    }

    fun scheduleFullCachePing() {
        viewModelScope.launch {
            appSettings.setFullCachePingScheduled(true)
            com.my.vpn.util.WorkScheduler.enqueueConfigPingNow(getApplication())
            _uiState.update {
                it.copy(statusMessage = strings.get(R.string.cache_full_check_scheduled))
            }
        }
    }

    fun openAddSubscriptionDialog() {
        _uiState.update { it.copy(showAddSubscriptionDialog = true, addSubscriptionError = null) }
    }

    fun dismissAddSubscriptionDialog() {
        _uiState.update {
            it.copy(showAddSubscriptionDialog = false, addSubscriptionError = null)
        }
    }

    fun addCustomSubscription(title: String, url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                _uiState.update { it.copy(addSubscriptionError = strings.get(R.string.sub_enter_url)) }
                return@launch
            }
            if (title.trim().isBlank()) {
                _uiState.update { it.copy(addSubscriptionError = strings.get(R.string.sub_enter_title)) }
                return@launch
            }
            val sub = repository.addCustomSubscription(title, trimmedUrl)
            val subs = repository.loadCustomSubscriptions()
            _uiState.update {
                it.copy(
                    customSubscriptions = subs,
                    showAddSubscriptionDialog = false,
                    addSubscriptionError = null,
                    showSubscriptionsExpanded = true
                )
            }
            refreshCustomSubscriptionFromMenu(sub)
        }
    }

    fun refreshCustomSubscriptionFromMenu(sub: CustomSubscription) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    loadingMessage = strings.get(R.string.sub_loading_named, sub.title),
                    errorMessage = null
                )
            }
            when (val result = repository.refreshCustomSubscriptionFromRemote(sub, onProgress = ::updateProgress)) {
                is ConfigRepository.RefreshResult.Success -> {
                    failedConfigIds.clear()
                    applySuccess(result)
                    _uiState.update {
                        it.copy(
                            customSubscriptions = repository.loadCustomSubscriptions(),
                            statusMessage = strings.get(R.string.sub_cached_named, sub.title)
                        )
                    }
                    applyListFilter(ListFilter.Custom(sub.id))
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun openManageSubscriptionsDialog() {
        _uiState.update { it.copy(showManageSubscriptionsDialog = true) }
    }

    fun dismissManageSubscriptionsDialog() {
        _uiState.update { it.copy(showManageSubscriptionsDialog = false) }
    }

    fun requestDeleteCustomSubscription(id: String) {
        val sub = _uiState.value.customSubscriptions.find { it.id == id } ?: return
        _uiState.update {
            it.copy(
                pendingDeleteSubscriptionId = id,
                pendingDeleteSubscriptionTitle = sub.title
            )
        }
    }

    fun dismissDeleteSubscriptionConfirm() {
        _uiState.update {
            it.copy(
                pendingDeleteSubscriptionId = null,
                pendingDeleteSubscriptionTitle = null
            )
        }
    }

    fun confirmDeleteCustomSubscription() {
        val id = _uiState.value.pendingDeleteSubscriptionId ?: return
        viewModelScope.launch {
            repository.removeCustomSubscription(id)
            val subs = repository.loadCustomSubscriptions()
            val configs = repository.loadCachedConfigs()
            val filter = _uiState.value.listFilter.let { current ->
                if (current is ListFilter.Custom && current.id == id) ListFilter.All else current
            }
            _uiState.update {
                it.copy(
                    customSubscriptions = subs,
                    configs = configs,
                    pendingDeleteSubscriptionId = null,
                    pendingDeleteSubscriptionTitle = null,
                    showManageSubscriptionsDialog = if (subs.isEmpty()) false else it.showManageSubscriptionsDialog,
                    statusMessage = strings.get(R.string.sub_deleted)
                )
            }
            applyListFilter(filter, collapseIfSelected = false)
        }
    }

    fun toggleCountryExpanded(country: String) {
        _uiState.update { state ->
            val expanded = state.expandedCountries.toMutableSet()
            if (country in expanded) expanded.remove(country) else expanded.add(country)
            state.copy(expandedCountries = expanded)
        }
    }

    private fun applyListFilter(filter: ListFilter, collapseIfSelected: Boolean = false) {
        applyListFilterJob?.cancel()
        applyListFilterJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val filtered = withContext(Dispatchers.Default) {
                filterConfigs(snapshot.configs, filter, snapshot)
            }
            val groups = withContext(Dispatchers.Default) {
                repository.groupByCountry(filtered)
            }
            _uiState.update {
                it.copy(
                    listFilter = filter,
                    showAllConfigsSelected = filter is ListFilter.All,
                    countryGroups = groups,
                    selectedConfig = filtered.firstOrNull { c -> c.connectSupported }
                        ?: filtered.firstOrNull(),
                    showSubscriptionsExpanded = if (collapseIfSelected) false else it.showSubscriptionsExpanded,
                    expandedCountries = if (filter !is ListFilter.All) emptySet() else it.expandedCountries,
                    statusMessage = when {
                        filtered.isEmpty() && it.protocolFilter != null ->
                            strings.get(R.string.msg_no_protocol_servers, it.protocolFilter!!.displayName)
                        filtered.isEmpty() && filter !is ListFilter.All ->
                            strings.get(R.string.msg_no_servers_in_sub)
                        else -> it.statusMessage
                    }
                )
            }
        }
    }

    private fun filterConfigs(
        configs: List<VpnConfig>,
        filter: ListFilter,
        state: MainUiState = _uiState.value
    ): List<VpnConfig> {
        val allowPending = state.isPinging || state.pingPending
        val protocol = state.protocolFilter
        val byFilter = when (filter) {
            ListFilter.All -> configs
            is ListFilter.Builtin -> configs.filter { it.source == filter.type }
            is ListFilter.Custom -> configs.filter { it.customSourceId == filter.id }
        }
        val byProtocol = if (protocol != null) {
            byFilter.filter { it.protocol == protocol }
        } else {
            byFilter
        }
        val showAllInList = state.showAllFromSubscription ||
            filter is ListFilter.All ||
            filter is ListFilter.Custom
        val strictForList = if (filter is ListFilter.Custom) false else state.strictListProxyVerified
        val includeUnconfirmed = state.showUnconfirmedTunnelConfigs && !strictForList
        val displayed = ConfigFilters.filterForDisplay(
            byProtocol,
            allowPending,
            strictForList,
            showAllInList,
            includeUnconfirmed
        )
        return ConfigListProcessor.sortByPing(displayed)
    }

    fun openSplitTunnelPicker() {
        viewModelScope.launch(Dispatchers.Default) {
            val apps = InstalledAppsProvider.loadLaunchableApps(getApplication())
            withContext(Dispatchers.Main.immediate) {
                _uiState.update {
                    it.copy(
                        showSplitTunnelPicker = true,
                        installedApps = apps,
                        splitTunnelAppFilter = ""
                    )
                }
            }
        }
    }

    fun closeSplitTunnelPicker() {
        _uiState.update { it.copy(showSplitTunnelPicker = false) }
    }

    fun setSplitTunnelAppFilter(text: String) {
        _uiState.update { it.copy(splitTunnelAppFilter = text) }
    }

    fun toggleSplitTunnelApp(packageName: String) {
        _uiState.update { state ->
            val updated = state.splitTunnelSelectedPackages.toMutableSet()
            if (!updated.add(packageName)) updated.remove(packageName)
            state.copy(splitTunnelSelectedPackages = updated)
        }
    }

    fun saveSplitTunnelSelectedApps() {
        viewModelScope.launch {
            val packages = _uiState.value.splitTunnelSelectedPackages
            appSettings.setSplitTunnelPackages(packages)
            SplitTunnelHolder.allowedPackages = packages
            _uiState.update {
                it.copy(
                    splitTunnelPackages = packages.joinToString(", "),
                    showSplitTunnelPicker = false
                )
            }
        }
    }

    fun openConfigPicker() {
        _uiState.update { it.copy(currentScreen = AppScreen.ConfigPicker) }
    }

    fun closeConfigPicker() {
        _uiState.update { it.copy(currentScreen = AppScreen.Main) }
    }

    fun toggleSubscriptions() {
        _uiState.update { it.copy(showSubscriptionsExpanded = !it.showSubscriptionsExpanded) }
    }

    fun openHelp(fromMenu: Boolean = false) {
        helpReturnScreen = if (fromMenu) AppScreen.Menu else AppScreen.Main
        _uiState.update { it.copy(currentScreen = AppScreen.Help) }
    }

    fun closeHelp() {
        _uiState.update { it.copy(currentScreen = helpReturnScreen) }
    }

    fun requestDeleteAll() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            healthMonitorJob?.cancel()
            recoveryJob?.cancel()
            pingJob?.cancel()
            if (_uiState.value.isConnected) {
                VpnController.disconnect(getApplication())
            }
            repository.clearAllData()
            failedConfigIds.clear()
            _uiState.update {
                MainUiState(currentScreen = AppScreen.Main, showRestartDialog = true)
            }
            bootstrap()
        }
    }

    fun dismissRestartDialog() {
        _uiState.update { it.copy(showRestartDialog = false) }
    }

    private suspend fun applySuccess(result: ConfigRepository.RefreshResult.Success) {
        val lastUpdated = repository.getLastUpdatedAt()
        val filter = _uiState.value.listFilter
        val customSubs = repository.loadCustomSubscriptions()
        _uiState.update {
            it.copy(
                isLoading = false,
                isUpdating = false,
                isRecovering = false,
                loadingMessage = null,
                configs = result.configs,
                totalInSubscription = result.totalInSubscription,
                loadedSources = result.loadedSources,
                sourceStats = result.sourceStats,
                builtinTraffic = result.builtinTraffic,
                lastUpdatedAt = lastUpdated,
                customSubscriptions = customSubs,
                pingPending = result.pingPending
            )
        }
        applyListFilter(filter)
        if (result.pingPending) {
            startBackgroundPing()
        }
    }

    private fun startBackgroundPing() {
        if (pingJob?.isActive == true) return
        pingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPinging = true,
                    pingPending = true,
                    loadingMessage = strings.get(R.string.msg_pinging)
                )
            }
            val scope = pingScopeForListFilter(_uiState.value.listFilter)
            when (val result = repository.repingCachedConfigsFast(onProgress = ::updateProgress, pingScope = scope)) {
                is ConfigRepository.RefreshResult.Success -> {
                    val lastUpdated = repository.getLastUpdatedAt()
                    val filter = _uiState.value.listFilter
                    _uiState.update {
                        it.copy(
                            configs = result.configs,
                            totalInSubscription = result.totalInSubscription,
                            sourceStats = result.sourceStats,
                            builtinTraffic = result.builtinTraffic,
                            lastUpdatedAt = lastUpdated,
                            pingPending = false
                        )
                    }
                    applyListFilter(filter)
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> Unit
            }
            val hasMoreToPing = withContext(Dispatchers.IO) {
                repository.loadCachedConfigs().any { it.pingMs == null }
            }
            if (hasMoreToPing) {
                appSettings.setFullCachePingScheduled(true)
                com.my.vpn.util.WorkScheduler.enqueueConfigPingNow(getApplication())
            } else {
                appSettings.setFullCachePingScheduled(false)
            }
            _uiState.update {
                it.copy(
                    isPinging = false,
                    pingPending = false,
                    isUpdating = false,
                    isLoading = false,
                    loadingMessage = null
                )
            }
        }
    }

    fun onAppForeground() {
        refreshConnectionState()
    }

    fun selectConfig(config: VpnConfig) {
        val state = _uiState.value
        if (state.isConnecting) {
            cancelConnect()
            _uiState.update { it.copy(selectedConfig = config) }
            if (config.connectSupported) {
                tryConnect(config, userInitiated = true)
            }
            return
        }
        _uiState.update { it.copy(selectedConfig = config) }
        if (state.isConnected && state.connectedConfigId != config.id) {
            reconnectJob?.cancel()
            reconnectJob = viewModelScope.launch {
                delay(400)
                if (_uiState.value.selectedConfig?.id != config.id) return@launch
                reconnectToConfig(config)
            }
        }
    }

    private suspend fun reconnectToConfig(config: VpnConfig) {
        if (!config.connectSupported) {
            _uiState.update {
                it.copy(errorMessage = strings.get(R.string.protocol_no_connect, config.protocol.displayName))
            }
            return
        }
        healthMonitorJob?.cancel()
        failedConfigIds.clear()
        val context = getApplication<Application>()
        if (!VpnPrepare.isPrepared(context)) {
            tryConnect(config, userInitiated = true)
            return
        }
        beginConnecting(context, R.string.connecting_server, config)
        pingJob?.cancel()
        val ok = withContext(Dispatchers.IO) {
            VpnController.reconnect(context, config)
            if (!VpnController.awaitServiceStart()) return@withContext false
            withContext(Dispatchers.Main.immediate) {
                updateConnectingLabel(context, R.string.connecting_core_init)
            }
            if (!VpnController.awaitCoreStartAttempt()) return@withContext false
            waitForVpnCoreAndTunnel(config)
        }
        if (ok) {
            finishConnecting(config)
        } else {
            val lateOk = withContext(Dispatchers.IO) { awaitLateCoreReady() }
            if (lateOk) {
                finishConnecting(config)
                return
            }
            withContext(Dispatchers.IO) {
                VpnController.disconnect(context, userRequested = false)
            }
            endConnecting(
                connected = false,
                config = config,
                error = strings.get(R.string.reconnect_tunnel_fail)
            )
        }
    }

    fun connectSelected() {
        val config = _uiState.value.selectedConfig ?: return
        if (!config.connectSupported) {
            _uiState.update {
                it.copy(errorMessage = strings.get(R.string.protocol_list_only, config.protocol.displayName))
            }
            return
        }
        tryConnect(config, userInitiated = true)
    }

    fun onVpnPermissionGranted() {
        _uiState.update { it.copy(vpnPermissionIntent = null) }
        val config = _uiState.value.selectedConfig ?: return
        tryConnect(config, userInitiated = true)
    }

    fun onVpnPermissionDenied() {
        _uiState.update {
            it.copy(vpnPermissionIntent = null, errorMessage = strings.get(R.string.msg_vpn_permission))
        }
    }

    private fun tryConnect(config: VpnConfig, userInitiated: Boolean = false) {
        if (!config.connectSupported) return
        val context = getApplication<Application>()
        val intent = VpnPrepare.prepareIntent(context)
        if (intent != null) {
            _uiState.update { it.copy(vpnPermissionIntent = intent, selectedConfig = config) }
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            return
        }
        if (userInitiated) {
            failedConfigIds.clear()
        }
        connect(config)
    }

    private fun connect(config: VpnConfig) {
        launchConnectJob(config) {
            beginConnecting(getApplication(), R.string.connecting_server, config)
            connectAwaitingVpn(config)
        }
    }

    private fun launchConnectJob(config: VpnConfig, block: suspend () -> Unit) {
        connectJob?.cancel()
        healthMonitorJob?.cancel()
        connectJob = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                withContext(Dispatchers.Main.immediate) {
                    if (_uiState.value.isConnecting) {
                        endConnecting(
                            connected = false,
                            config = config,
                            error = null
                        )
                    }
                }
                throw e
            }
        }
    }

    private suspend fun connectAwaitingVpn(config: VpnConfig) {
        pingJob?.cancel()
        ConnectLog.stage(
            "connect",
            "${config.protocol.displayName} ${config.server}:${config.port}"
        )
        withContext(Dispatchers.Main.immediate) {
            updateConnectingLabel(
                getApplication(),
                R.string.connecting_server
            )
        }
        val savedBypass = withContext(Dispatchers.IO) {
            app.perServerBypassStorage.get(config.dedupeKey)
        }
        val profile = savedBypass ?: _uiState.value.bypassProfile
        BypassProfileHolder.profile = profile
        withContext(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(bypassProfile = profile, tunnelDegraded = false) }
        }
        val buildErr = withContext(Dispatchers.Default) {
            runCatching {
                V2RayConfigBuilder.buildVpnConfig(config, profile)
            }.exceptionOrNull()?.message
        }
        if (buildErr != null) {
            ConnectLog.stage("config_build", "fail: $buildErr")
            endConnecting(
                connected = false,
                config = config,
                error = strings.get(R.string.invalid_server_link, buildErr),
                diagnostics = ConnectionDiagnosticsBuilder.fromConnectFailure(
                    config = config,
                    tcpReachable = false,
                    tunnelHealthy = false,
                    error = buildErr
                )
            )
            return
        }
        val ok = withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            VpnController.connect(context, config)
            if (!VpnController.awaitServiceStart()) {
                ConnectLog.stage("service", "not_ready")
                return@withContext false
            }
            withContext(Dispatchers.Main.immediate) {
                updateConnectingLabel(context, R.string.connecting_core_init)
            }
            if (!VpnController.awaitCoreStartAttempt()) {
                ConnectLog.stage("service", "core_not_running")
                return@withContext false
            }
            waitForVpnCoreAndTunnel(config)
        }
        ConnectLog.stage("connect_result", if (ok) "ok" else "fail")
        if (ok) {
            finishConnecting(config)
        } else {
            val lateOk = withContext(Dispatchers.IO) { awaitLateCoreReady() }
            if (lateOk) {
                ConnectLog.stage("connect_result", "ok_late")
                finishConnecting(config)
                return
            }
            withContext(Dispatchers.IO) {
                VpnController.disconnect(getApplication(), userRequested = false)
            }
            val tcpOk = withContext(Dispatchers.IO) { repository.isConfigReachable(config) }
            val coreRunning = resolveCoreRunningForDiagnostics()
            val failReason = com.my.vpn.vpn.VpnServiceStartTracker.lastFailureReason()
            val errorMsg = connectFailureMessage(failReason)
            endConnecting(
                connected = false,
                config = config,
                error = errorMsg,
                diagnostics = ConnectionDiagnosticsBuilder.fromConnectFailure(
                    config = config,
                    tcpReachable = tcpOk,
                    tunnelHealthy = false,
                    error = errorMsg,
                    coreRunning = coreRunning
                )
            )
        }
    }

    private fun connectFailureMessage(failReason: String?): String {
        return when {
            failReason == "vpn_not_prepared" -> strings.get(R.string.msg_vpn_permission)
            failReason == "vpn_interface_failed" -> strings.get(R.string.vpn_interface_fail)
            failReason?.startsWith("appops_denied") == true -> strings.get(R.string.vpn_interface_fail)
            else -> strings.get(R.string.vpn_setup_tunnel_fail)
        }
    }

    /** Прервать подключение (проверка туннеля, запуск Xray) и вернуть UI в «отключено». */
    fun cancelConnect() {
        connectJob?.cancel()
        reconnectJob?.cancel()
        healthMonitorJob?.cancel()
        VpnController.endConnectAttempt()
        VpnController.disconnect(getApplication(), userRequested = true)
        _uiState.update {
            it.copy(
                isConnecting = false,
                isDisconnecting = false,
                isConnected = false,
                connectedConfigId = null,
                connectionButtonLabel = null,
                tunnelDegraded = false,
                statusMessage = getApplication<Application>().getString(R.string.status_disconnected)
            )
        }
    }

    fun disconnect() {
        if (_uiState.value.isConnecting) {
            cancelConnect()
            return
        }
        healthMonitorJob?.cancel()
        val state = _uiState.value
        val config = state.selectedConfig
        val endedAt = System.currentTimeMillis()
        if (state.isConnected && config != null && state.sessionStartedAtMs > 0L) {
            viewModelScope.launch {
                trafficSessionStorage.addSession(
                    TrafficSession(
                        configName = config.name,
                        country = config.country,
                        protocol = config.protocol.displayName,
                        startedAtMs = state.sessionStartedAtMs,
                        endedAtMs = endedAt,
                        rxBytes = state.rxBytes,
                        txBytes = state.txBytes
                    )
                )
            }
        }
        _uiState.update {
            it.copy(
                isDisconnecting = true,
                isConnecting = false,
                connectionButtonLabel = getApplication<Application>().getString(R.string.disconnecting_button)
            )
        }
        VpnController.disconnect(getApplication(), userRequested = true)
        _uiState.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                isDisconnecting = false,
                connectionButtonLabel = null,
                connectedConfigId = null,
                rxBytes = 0L,
                txBytes = 0L,
                sessionStartedAtMs = 0L,
                statusMessage = getApplication<Application>().getString(R.string.status_disconnected)
            )
        }
    }

    private fun beginConnecting(
        context: Application,
        @androidx.annotation.StringRes labelRes: Int,
        config: VpnConfig? = null
    ) {
        _uiState.update {
            it.copy(
                selectedConfig = config ?: it.selectedConfig,
                isConnecting = true,
                isDisconnecting = false,
                isConnected = false,
                connectedConfigId = null,
                connectionButtonLabel = context.getString(labelRes),
                errorMessage = null,
                statusMessage = context.getString(labelRes)
            )
        }
    }

    private fun updateConnectingLabel(context: Application, @androidx.annotation.StringRes labelRes: Int) {
        val label = context.getString(labelRes)
        _uiState.update {
            it.copy(connectionButtonLabel = label, statusMessage = label)
        }
    }

    private suspend fun waitForVpnCoreRunning(): Boolean {
        return VpnCoreManager.awaitCoreRunning()
    }

    private suspend fun awaitLateCoreReady(): Boolean {
        return VpnCoreManager.awaitCoreRunning(
            timeoutMs = AppConstants.VPN_CORE_LATE_START_GRACE_MS * 2,
            lateGraceMs = AppConstants.VPN_CORE_LATE_START_GRACE_MS
        )
    }

    private suspend fun resolveCoreRunningForDiagnostics(): Boolean {
        if (VpnCoreManager.isRunning()) return true
        return VpnCoreManager.awaitCoreRunning(
            timeoutMs = 6_000L,
            lateGraceMs = 4_000L
        )
    }

    private suspend fun waitForVpnCoreAndTunnel(config: VpnConfig): Boolean {
        if (!VpnCoreManager.isRunning()) {
            if (!VpnCoreManager.awaitCoreRunning(timeoutMs = 3_000L, lateGraceMs = 1_500L)) {
                ConnectLog.stage("core", "not_running")
                return false
            }
        }
        ConnectLog.stage("core", "running")
        withContext(Dispatchers.Main.immediate) {
            updateConnectingLabel(getApplication(), R.string.connecting_socks_ready)
        }
        val socksUp = connectVpnUseCase.verifyTunnelSocksUp(config)
        ConnectLog.stage("tunnel_verify", if (socksUp) "socks_ok" else "socks_fail")
        return socksUp
    }

    private fun verifyTunnelHttpInBackground(config: VpnConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val httpOk = connectVpnUseCase.verifyTunnelHttp(config)
            ConnectLog.stage("tunnel_verify_bg", if (httpOk) "http_ok" else "http_fail")
            withContext(Dispatchers.Main.immediate) {
                val state = _uiState.value
                if (!state.isConnected || state.connectedConfigId != config.id) return@withContext
                _uiState.update {
                    it.copy(
                        tunnelDegraded = !httpOk,
                        statusMessage = if (httpOk) {
                            it.statusMessage
                        } else {
                            strings.get(R.string.msg_tunnel_degraded)
                        }
                    )
                }
            }
        }
    }

    private fun finishConnecting(config: VpnConfig) {
        VpnController.endConnectAttempt()
        val context = getApplication<Application>()
        val country = CountryNames.toDisplay(config.country, _uiState.value.appLanguage)
        val status = context.getString(R.string.status_connected, country)
        _uiState.update {
            it.copy(
                isConnecting = false,
                isDisconnecting = false,
                connectionButtonLabel = null,
                isConnected = true,
                connectedConfigId = config.id,
                selectedConfig = config,
                rxBytes = 0L,
                txBytes = 0L,
                sessionStartedAtMs = System.currentTimeMillis(),
                errorMessage = null,
                statusMessage = status,
                tunnelDegraded = false
            )
        }
        verifyTunnelHttpInBackground(config)
        startHealthMonitor()
        viewModelScope.launch(Dispatchers.IO) {
            app.lastConnectStorage.saveConfigId(config.id, config.name)
            app.perServerBypassStorage.save(config.dedupeKey, _uiState.value.bypassProfile)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appSettings.setAppLanguage(language)
            LocaleHelper.applyLanguage(getApplication(), language)
            _uiState.update { it.copy(appLanguage = language) }
            _requestActivityRecreate.emit(Unit)
        }
    }

    private fun endConnecting(
        connected: Boolean,
        config: VpnConfig,
        error: String?,
        diagnostics: ConnectionDiagnostics? = null
    ) {
        VpnController.endConnectAttempt()
        _uiState.update {
            it.copy(
                isConnecting = false,
                isDisconnecting = false,
                connectionButtonLabel = null,
                isConnected = connected,
                connectedConfigId = if (connected) config.id else null,
                selectedConfig = config,
                errorMessage = error,
                statusMessage = if (connected) it.statusMessage else null,
                connectionDiagnostics = diagnostics,
                showDiagnosticsDialog = diagnostics != null
            )
        }
    }

    fun refreshConnectionState() {
        val connected = VpnController.isConnected
        _uiState.update {
            it.copy(
                isConnected = connected,
                connectedConfigId = VpnController.connectedConfigId
            )
        }
        if (connected) {
            viewModelScope.launch { updateTrafficStats() }
            if (healthMonitorJob?.isActive != true) startHealthMonitor()
        } else {
            healthMonitorJob?.cancel()
        }
    }

    private fun startHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = viewModelScope.launch {
            delay(AppConstants.HEALTH_GRACE_AFTER_CONNECT_MS)
            var checkIntervalMs = AppConstants.HEALTH_CHECK_INTERVAL_MS
            var tunnelFailures = 0
            val failureThreshold = if (_uiState.value.tunnelDegraded) 4 else 2
            while (isActive && _uiState.value.isConnected) {
                delay(checkIntervalMs)
                if (!_uiState.value.isConnected) break
                if (!VpnCoreManager.isRunning()) {
                    handleConnectionFailure("VPN остановлен, переключение сервера…")
                    break
                }
                val config = _uiState.value.selectedConfig
                if (config != null) {
                    val port = AppConstants.XRAY_SOCKS_VPN_PORT
                    val healthy = withContext(Dispatchers.IO) {
                        TunnelConnectivityChecker.verifyHttpViaSocks(port)
                    }
                    if (!healthy) {
                        tunnelFailures++
                        _uiState.update { it.copy(tunnelDegraded = true) }
                        checkIntervalMs = (checkIntervalMs * 3 / 2)
                            .coerceAtMost(AppConstants.HEALTH_CHECK_INTERVAL_MAX_MS)
                        if (tunnelFailures < failureThreshold) continue
                        handleConnectionFailure(strings.get(R.string.msg_tunnel_failover))
                        break
                    } else {
                        tunnelFailures = 0
                        _uiState.update { it.copy(tunnelDegraded = false) }
                        checkIntervalMs = AppConstants.HEALTH_CHECK_INTERVAL_MS
                    }
                }
            }
        }
    }

    private fun handleConnectionFailure(message: String) {
        if (!_uiState.value.autoFailoverEnabled) {
            viewModelScope.launch {
                if (_uiState.value.isConnected) {
                    VpnController.disconnect(getApplication(), userRequested = false)
                }
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        connectedConfigId = null,
                        statusMessage = message,
                        errorMessage = strings.get(R.string.msg_connection_lost),
                        tunnelDegraded = true
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    statusMessage = message,
                    isConnecting = false,
                    isDisconnecting = false,
                    connectionButtonLabel = null
                )
            }
            _uiState.value.connectedConfigId?.let { failedConfigIds.add(it) }
            _uiState.value.selectedConfig?.id?.let { failedConfigIds.add(it) }
            if (_uiState.value.isConnected) {
                VpnController.disconnect(getApplication(), userRequested = false)
                _uiState.update { it.copy(isConnected = false, connectedConfigId = null) }
            }
            attemptFailover()
        }
    }

    private suspend fun attemptFailover() {
        val configs = filterConfigs(_uiState.value.configs, _uiState.value.listFilter)
        if (configs.isEmpty()) {
            startConfigRecovery(silent = false)
            return
        }

        val candidates = configs
            .filter { it.id !in failedConfigIds }
            .sortedBy { it.pingMs ?: Long.MAX_VALUE }

        for (config in candidates) {
            _uiState.update {
                it.copy(
                    statusMessage = strings.get(
                        R.string.msg_checking_server,
                        CountryNames.toDisplay(config.country, it.appLanguage)
                    ),
                    selectedConfig = config
                )
            }
            if (!repository.isConfigReachable(config)) {
                failedConfigIds.add(config.id)
                continue
            }
            if (!VpnPrepare.isPrepared(getApplication())) {
                _uiState.update {
                    it.copy(
                        statusMessage = strings.get(R.string.msg_vpn_permission_reconnect),
                        selectedConfig = config
                    )
                }
                return
            }
            beginConnecting(getApplication(), R.string.connecting_server)
            val savedBypass = app.perServerBypassStorage.get(config.dedupeKey)
            if (savedBypass != null) {
                BypassProfileHolder.profile = savedBypass
                _uiState.update { it.copy(bypassProfile = savedBypass) }
            }
            val ok = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                VpnController.connect(context, config)
                VpnController.awaitServiceReady()
                withContext(Dispatchers.Main.immediate) {
                    updateConnectingLabel(context, R.string.connecting_core_init)
                }
                VpnController.awaitCoreStartAttempt()
                waitForVpnCoreAndTunnel(config)
            }
            if (!ok) {
                failedConfigIds.add(config.id)
                endConnecting(connected = false, config = config, error = null)
                continue
            }
            finishConnecting(config)
            return
        }

        _uiState.update {
            it.copy(
                errorMessage = strings.get(R.string.msg_no_working_configs),
                statusMessage = strings.get(R.string.msg_refresh_ping_hint)
            )
        }
    }

    private fun startConfigRecovery(silent: Boolean) {
        if (recoveryJob?.isActive == true) return
        recoveryJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecovering = true,
                    statusMessage = if (silent) {
                        strings.get(R.string.cache_checking)
                    } else {
                        strings.get(R.string.cache_reping)
                    }
                )
            }
            when (val result = repository.repingCachedConfigsFast(onProgress = ::updateProgress)) {
                is ConfigRepository.RefreshResult.Success -> {
                    if (result.configs.isNotEmpty()) {
                        failedConfigIds.clear()
                        applySuccess(result)
                        _uiState.update {
                            it.copy(isRecovering = false, statusMessage = strings.get(R.string.cache_checked))
                        }
                        return@launch
                    }
                }
                is ConfigRepository.RefreshResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> Unit
            }
            _uiState.update { it.copy(isRecovering = false) }
        }
    }

    private fun startTrafficPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isConnected) {
                    updateTrafficStats()
                }
                delay(1000)
            }
        }
    }

    private suspend fun updateTrafficStats() {
        val (rx, tx) = withContext(Dispatchers.Default) {
            VpnTrafficMonitor.getSessionTraffic(getApplication())
        }
        withContext(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(rxBytes = rx, txBytes = tx) }
        }
        VpnController.updateNotification(getApplication(), rx, tx)
    }
}
