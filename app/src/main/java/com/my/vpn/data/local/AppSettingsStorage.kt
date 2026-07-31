package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.SplitTunnelMode
import com.my.vpn.data.model.DnsBypassMode
import com.my.vpn.data.model.DpiBypassMode
import com.my.vpn.data.model.SniBypassMode
import com.my.vpn.data.model.TtlBypassMode
import com.my.vpn.data.model.AppLanguage
import com.my.vpn.data.model.PrivacyTimezoneMode
import com.my.vpn.ui.theme.AppThemeMode
import com.my.vpn.ui.theme.UI_SCALE_LEVEL_DEFAULT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettingsStorage(private val context: Context) {

    suspend fun getAcknowledgedSubscriptionHash(): String? =
        context.appSettingsStore.data.map { it[KEY_ACK_SUB_HASH] }.first()

    suspend fun setAcknowledgedSubscriptionHash(hash: String) {
        context.appSettingsStore.edit { it[KEY_ACK_SUB_HASH] = hash }
    }

    suspend fun isAppUpdateNeverPrompt(): Boolean =
        context.appSettingsStore.data.map { it[KEY_APP_UPDATE_NEVER] ?: false }.first()

    suspend fun setAppUpdateNeverPrompt(value: Boolean) {
        context.appSettingsStore.edit { it[KEY_APP_UPDATE_NEVER] = value }
    }

    suspend fun getAppUpdatePostponeUntil(): Long =
        context.appSettingsStore.data.map { it[KEY_APP_UPDATE_POSTPONE] ?: 0L }.first()

    suspend fun postponeAppUpdate(days: Int) {
        val until = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
        context.appSettingsStore.edit { it[KEY_APP_UPDATE_POSTPONE] = until }
    }

    suspend fun getDismissedAppVersion(): String? =
        context.appSettingsStore.data.map { it[KEY_DISMISSED_APP_VERSION] }.first()

    suspend fun setDismissedAppVersion(version: String) {
        context.appSettingsStore.edit { it[KEY_DISMISSED_APP_VERSION] = version }
    }

    suspend fun clearAppUpdatePostpone() {
        context.appSettingsStore.edit { it.remove(KEY_APP_UPDATE_POSTPONE) }
    }

    val uiScaleLevelFlow: Flow<Int> =
        context.appSettingsStore.data.map { prefs ->
            (prefs[KEY_UI_SCALE_LEVEL] ?: UI_SCALE_LEVEL_DEFAULT).coerceIn(1, 5)
        }

    suspend fun getUiScaleLevel(): Int = uiScaleLevelFlow.first()

    suspend fun setUiScaleLevel(level: Int) {
        context.appSettingsStore.edit {
            it[KEY_UI_SCALE_LEVEL] = level.coerceIn(1, 5)
        }
    }

    val themeModeFlow: Flow<AppThemeMode> =
        context.appSettingsStore.data.map { prefs ->
            AppThemeMode.fromKey(prefs[KEY_THEME_MODE])
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appSettingsStore.edit { it[KEY_THEME_MODE] = mode.storageKey }
    }

    val bypassProfileFlow: Flow<ConnectionBypassProfile> =
        context.appSettingsStore.data.map { prefs -> prefs.toBypassProfile() }

    suspend fun getBypassProfile(): ConnectionBypassProfile = bypassProfileFlow.first()

    val configTestModeFlow: Flow<ConfigTestMode> =
        context.appSettingsStore.data.map { prefs ->
            ConfigTestMode.entries.firstOrNull {
                it.name == prefs[KEY_CONFIG_TEST_MODE]
            } ?: ConfigTestMode.TCP_ONLY
        }

    suspend fun getConfigTestMode(): ConfigTestMode = configTestModeFlow.first()

    suspend fun setConfigTestMode(mode: ConfigTestMode) {
        context.appSettingsStore.edit { it[KEY_CONFIG_TEST_MODE] = mode.name }
    }

    suspend fun isBackgroundConfigUpdateEnabled(): Boolean =
        context.appSettingsStore.data.map { it[KEY_BG_CONFIG_UPDATE] ?: true }.first()

    suspend fun setBackgroundConfigUpdateEnabled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_BG_CONFIG_UPDATE] = enabled }
    }

    suspend fun isBackgroundPingWifiOnly(): Boolean =
        context.appSettingsStore.data.map { it[KEY_BG_PING_WIFI_ONLY] ?: true }.first()

    suspend fun setBackgroundPingWifiOnly(wifiOnly: Boolean) {
        context.appSettingsStore.edit { it[KEY_BG_PING_WIFI_ONLY] = wifiOnly }
    }

    suspend fun isFullCachePingScheduled(): Boolean =
        context.appSettingsStore.data.map { it[KEY_FULL_CACHE_PING] ?: false }.first()

    suspend fun setFullCachePingScheduled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_FULL_CACHE_PING] = enabled }
    }

    suspend fun isKillSwitchEnabled(): Boolean =
        context.appSettingsStore.data.map { it[KEY_KILL_SWITCH] ?: false }.first()

    suspend fun setKillSwitchEnabled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_KILL_SWITCH] = enabled }
    }

    val strictListProxyVerifiedFlow: Flow<Boolean> =
        context.appSettingsStore.data.map { it[KEY_STRICT_LIST_PROXY] ?: false }

    suspend fun isStrictListProxyVerified(): Boolean = strictListProxyVerifiedFlow.first()

    suspend fun setStrictListProxyVerified(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_STRICT_LIST_PROXY] = enabled }
    }

    val showAllFromSubscriptionFlow: Flow<Boolean> =
        // По умолчанию показываем всё найденное. Иначе создаётся впечатление
        // что "серверов мало": фильтр скрывает N/A/проваленные и >лимита по задержке.
        context.appSettingsStore.data.map { it[KEY_SHOW_ALL_SUBSCRIPTION] ?: true }

    suspend fun isShowAllFromSubscription(): Boolean = showAllFromSubscriptionFlow.first()

    suspend fun setShowAllFromSubscription(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_SHOW_ALL_SUBSCRIPTION] = enabled }
    }

    val showUnconfirmedTunnelConfigsFlow: Flow<Boolean> =
        context.appSettingsStore.data.map { prefs ->
            when {
                prefs.contains(KEY_SHOW_UNCONFIRMED_TUNNEL) ->
                    prefs[KEY_SHOW_UNCONFIRMED_TUNNEL]!!
                prefs[KEY_STRICT_LIST_PROXY] == true -> false
                else -> true
            }
        }

    suspend fun isShowUnconfirmedTunnelConfigs(): Boolean =
        showUnconfirmedTunnelConfigsFlow.first()

    suspend fun setShowUnconfirmedTunnelConfigs(enabled: Boolean) {
        context.appSettingsStore.edit {
            it[KEY_SHOW_UNCONFIRMED_TUNNEL] = enabled
            if (enabled) it[KEY_STRICT_LIST_PROXY] = false
        }
    }

    val autoFailoverFlow: Flow<Boolean> =
        context.appSettingsStore.data.map { it[KEY_AUTO_FAILOVER] ?: true }

    suspend fun isAutoFailoverEnabled(): Boolean = autoFailoverFlow.first()

    suspend fun setAutoFailoverEnabled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_AUTO_FAILOVER] = enabled }
    }

    suspend fun getSplitTunnelMode(): SplitTunnelMode =
        context.appSettingsStore.data.map { prefs ->
            SplitTunnelMode.entries.firstOrNull {
                it.name == prefs[KEY_SPLIT_TUNNEL_MODE]
            } ?: SplitTunnelMode.FULL_TUNNEL
        }.first()

    suspend fun setSplitTunnelMode(mode: SplitTunnelMode) {
        context.appSettingsStore.edit { it[KEY_SPLIT_TUNNEL_MODE] = mode.name }
    }

    suspend fun getSplitTunnelPackages(): Set<String> =
        context.appSettingsStore.data.map { prefs ->
            prefs[KEY_SPLIT_TUNNEL_PACKAGES]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        }.first()

    suspend fun setSplitTunnelPackages(packages: Set<String>) {
        context.appSettingsStore.edit {
            it[KEY_SPLIT_TUNNEL_PACKAGES] = packages.joinToString(",")
        }
    }

    suspend fun isDnsAntiLeakEnabled(): Boolean =
        context.appSettingsStore.data.map { it[KEY_DNS_ANTI_LEAK] ?: false }.first()

    suspend fun setDnsAntiLeakEnabled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_DNS_ANTI_LEAK] = enabled }
    }

    val dnsAntiLeakFlow: Flow<Boolean> =
        context.appSettingsStore.data.map { it[KEY_DNS_ANTI_LEAK] ?: false }

    val appLanguageFlow: Flow<AppLanguage> =
        context.appSettingsStore.data.map { prefs ->
            AppLanguage.fromKey(prefs[KEY_APP_LANGUAGE])
        }

    suspend fun getAppLanguage(): AppLanguage = appLanguageFlow.first()

    suspend fun setAppLanguage(language: AppLanguage) {
        context.appSettingsStore.edit { it[KEY_APP_LANGUAGE] = language.storageKey }
        writeLanguageEarly(context, language)
    }

    /** Синхронизирует язык из DataStore в SharedPreferences (миграция после обновления). */
    suspend fun syncLanguageEarlyCache() {
        writeLanguageEarly(context, getAppLanguage())
    }

    val privacyTimezoneFlow: Flow<PrivacyTimezoneMode> =
        context.appSettingsStore.data.map { prefs ->
            PrivacyTimezoneMode.fromKey(prefs[KEY_PRIVACY_TIMEZONE])
        }

    suspend fun getPrivacyTimezoneMode(): PrivacyTimezoneMode = privacyTimezoneFlow.first()

    suspend fun setPrivacyTimezoneMode(mode: PrivacyTimezoneMode) {
        context.appSettingsStore.edit { it[KEY_PRIVACY_TIMEZONE] = mode.storageKey }
    }

    suspend fun isTunnelStealthEnabled(): Boolean =
        context.appSettingsStore.data.map { it[KEY_TUNNEL_STEALTH] ?: true }.first()

    suspend fun setTunnelStealthEnabled(enabled: Boolean) {
        context.appSettingsStore.edit { it[KEY_TUNNEL_STEALTH] = enabled }
    }

    val tunnelStealthFlow: Flow<Boolean> =
        context.appSettingsStore.data.map { it[KEY_TUNNEL_STEALTH] ?: true }

    suspend fun loadVpnOptionHolders() {
        com.my.vpn.vpn.SplitTunnelHolder.mode = getSplitTunnelMode()
        com.my.vpn.vpn.SplitTunnelHolder.allowedPackages = getSplitTunnelPackages()
    }

    suspend fun saveBypassProfile(profile: ConnectionBypassProfile) {
        context.appSettingsStore.edit { prefs ->
            prefs[KEY_DNS_MODE] = profile.dnsMode.name
            prefs[KEY_DNS_DIRECT] = profile.dnsDirectDoh.orEmpty()
            prefs[KEY_DNS_PROXY] = profile.dnsProxyDoh.orEmpty()
            prefs[KEY_SNI_MODE] = profile.sniMode.name
            prefs[KEY_MANUAL_SNI] = profile.manualSni
            prefs[KEY_SELECTED_SNI] = profile.selectedSni.orEmpty()
            prefs[KEY_SPLIT_RU] = profile.splitRuDirect
            prefs[KEY_DPI_MODE] = profile.dpiMode.name
            prefs[KEY_DPI_PRESET] = profile.dpiPresetId.orEmpty()
            prefs[KEY_DPI_PACKETS] = profile.dpiPackets
            prefs[KEY_DPI_LENGTH] = profile.dpiLength
            prefs[KEY_DPI_INTERVAL] = profile.dpiInterval
            prefs[KEY_TTL_MODE] = profile.ttlMode.name
            prefs[KEY_MANUAL_TTL] = profile.manualTtl
            prefs[KEY_SELECTED_TTL] = profile.selectedTtl ?: -1
            prefs[KEY_MUX_PADDING] = profile.muxPadding
            prefs[KEY_TCP_NODELAY] = profile.tcpNoDelay
        }
    }

    private fun Preferences.toBypassProfile(): ConnectionBypassProfile {
        val dnsMode = enumOrDefault(KEY_DNS_MODE, DnsBypassMode.OFF)
        val sniMode = enumOrDefault(KEY_SNI_MODE, SniBypassMode.OFF)
        val dpiMode = enumOrDefault(KEY_DPI_MODE, DpiBypassMode.OFF)
        val ttlMode = enumOrDefault(KEY_TTL_MODE, TtlBypassMode.OFF)
        val selectedTtl = this[KEY_SELECTED_TTL]?.takeIf { it >= 0 }
        return ConnectionBypassProfile(
            dnsMode = dnsMode,
            dnsDirectDoh = this[KEY_DNS_DIRECT]?.takeIf { it.isNotEmpty() },
            dnsProxyDoh = this[KEY_DNS_PROXY]?.takeIf { it.isNotEmpty() },
            sniMode = sniMode,
            manualSni = this[KEY_MANUAL_SNI] ?: "",
            selectedSni = this[KEY_SELECTED_SNI]?.takeIf { it.isNotEmpty() },
            splitRuDirect = this[KEY_SPLIT_RU] ?: true,
            dpiMode = dpiMode,
            dpiPresetId = this[KEY_DPI_PRESET]?.takeIf { it.isNotEmpty() },
            dpiPackets = this[KEY_DPI_PACKETS] ?: "tlshello",
            dpiLength = this[KEY_DPI_LENGTH] ?: "100-200",
            dpiInterval = this[KEY_DPI_INTERVAL] ?: "10-20",
            ttlMode = ttlMode,
            manualTtl = this[KEY_MANUAL_TTL] ?: 64,
            selectedTtl = selectedTtl,
            muxPadding = this[KEY_MUX_PADDING] ?: false,
            tcpNoDelay = this[KEY_TCP_NODELAY] ?: true
        )
    }

    private inline fun <reified T : Enum<T>> Preferences.enumOrDefault(
        key: Preferences.Key<String>,
        default: T
    ): T = runCatching { enumValueOf<T>(this[key] ?: default.name) }.getOrDefault(default)

    companion object {
        private const val EARLY_PREFS_NAME = "app_settings_early"
        private const val SP_KEY_LANGUAGE = "app_language"

        /**
         * Только для [android.app.Application.attachBaseContext]: DataStore требует
         * applicationContext, которого в этот момент ещё нет.
         */
        fun readLanguageBlocking(context: Context): AppLanguage {
            val prefs = context.getSharedPreferences(EARLY_PREFS_NAME, Context.MODE_PRIVATE)
            return AppLanguage.fromKey(prefs.getString(SP_KEY_LANGUAGE, null))
        }

        fun writeLanguageEarly(context: Context, language: AppLanguage) {
            context.getSharedPreferences(EARLY_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(SP_KEY_LANGUAGE, language.storageKey)
                .commit()
        }

        private val KEY_ACK_SUB_HASH = stringPreferencesKey("ack_subscription_hash")
        private val KEY_APP_UPDATE_NEVER = booleanPreferencesKey("app_update_never")
        private val KEY_APP_UPDATE_POSTPONE = longPreferencesKey("app_update_postpone_until")
        private val KEY_DISMISSED_APP_VERSION = stringPreferencesKey("dismissed_app_version")
        private val KEY_UI_SCALE_LEVEL = intPreferencesKey("ui_scale_level")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DNS_MODE = stringPreferencesKey("dns_bypass_mode")
        private val KEY_DNS_DIRECT = stringPreferencesKey("dns_direct_doh")
        private val KEY_DNS_PROXY = stringPreferencesKey("dns_proxy_doh")
        private val KEY_SNI_MODE = stringPreferencesKey("sni_bypass_mode")
        private val KEY_MANUAL_SNI = stringPreferencesKey("manual_sni")
        private val KEY_SELECTED_SNI = stringPreferencesKey("selected_sni")
        private val KEY_SPLIT_RU = booleanPreferencesKey("split_ru_direct")
        private val KEY_DPI_MODE = stringPreferencesKey("dpi_bypass_mode")
        private val KEY_DPI_PRESET = stringPreferencesKey("dpi_preset_id")
        private val KEY_DPI_PACKETS = stringPreferencesKey("dpi_packets")
        private val KEY_DPI_LENGTH = stringPreferencesKey("dpi_length")
        private val KEY_DPI_INTERVAL = stringPreferencesKey("dpi_interval")
        private val KEY_TTL_MODE = stringPreferencesKey("ttl_bypass_mode")
        private val KEY_MANUAL_TTL = intPreferencesKey("manual_ttl")
        private val KEY_SELECTED_TTL = intPreferencesKey("selected_ttl")
        private val KEY_MUX_PADDING = booleanPreferencesKey("mux_padding")
        private val KEY_TCP_NODELAY = booleanPreferencesKey("tcp_nodelay")
        private val KEY_CONFIG_TEST_MODE = stringPreferencesKey("config_test_mode")
        private val KEY_BG_CONFIG_UPDATE = booleanPreferencesKey("bg_config_update")
        private val KEY_BG_PING_WIFI_ONLY = booleanPreferencesKey("bg_ping_wifi_only")
        private val KEY_FULL_CACHE_PING = booleanPreferencesKey("full_cache_ping")
        private val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
        private val KEY_STRICT_LIST_PROXY = booleanPreferencesKey("strict_list_proxy_verified")
        private val KEY_SHOW_ALL_SUBSCRIPTION = booleanPreferencesKey("show_all_from_subscription")
        private val KEY_SHOW_UNCONFIRMED_TUNNEL =
            booleanPreferencesKey("show_unconfirmed_tunnel_configs")
        private val KEY_AUTO_FAILOVER = booleanPreferencesKey("auto_failover_enabled")
        private val KEY_SPLIT_TUNNEL_MODE = stringPreferencesKey("split_tunnel_mode")
        private val KEY_SPLIT_TUNNEL_PACKAGES = stringPreferencesKey("split_tunnel_packages")
        private val KEY_DNS_ANTI_LEAK = booleanPreferencesKey("dns_anti_leak_enabled")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_PRIVACY_TIMEZONE = stringPreferencesKey("privacy_timezone_mode")
        private val KEY_TUNNEL_STEALTH = booleanPreferencesKey("tunnel_stealth_enabled")
    }
}
