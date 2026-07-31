package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.vpn.data.model.MirrorType
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.data.model.VpnConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_configs")

class ConfigStorage(private val context: Context) {

    private val gson = Gson()
    private val cacheFile: File
        get() = File(context.filesDir, "config_cache/configs.json")

    suspend fun saveConfigs(
        configs: List<VpnConfig>,
        contentHash: String,
        sourceStats: Map<SubscriptionListType, Int> = emptyMap(),
        builtinTraffic: Map<SubscriptionListType, SubscriptionTraffic> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(gson.toJson(configs))
        context.configDataStore.edit { prefs ->
            prefs.remove(KEY_CONFIGS_LEGACY)
            prefs[KEY_HASH] = contentHash
            prefs[KEY_SOURCE_STATS] = gson.toJson(sourceStats.mapKeys { it.key.name })
            prefs[KEY_BUILTIN_TRAFFIC] = gson.toJson(builtinTraffic.mapKeys { it.key.name })
            prefs[KEY_UPDATED_AT] = System.currentTimeMillis()
            prefs[KEY_CACHE_VERSION] = CACHE_VERSION
        }
    }

    suspend fun loadConfigs(): List<VpnConfig> = withContext(Dispatchers.IO) {
        if (cacheFile.exists() && cacheFile.length() > 2) {
            return@withContext parseConfigList(cacheFile.readText())
        }
        val legacy = context.configDataStore.data.map { it[KEY_CONFIGS_LEGACY] }.first()
        if (!legacy.isNullOrBlank()) {
            val list = parseConfigList(legacy)
            if (list.isNotEmpty()) {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(legacy)
                context.configDataStore.edit { it.remove(KEY_CONFIGS_LEGACY) }
            }
            return@withContext list
        }
        emptyList()
    }

    private fun parseConfigList(json: String): List<VpnConfig> {
        val type = object : TypeToken<List<VpnConfig>>() {}.type
        return runCatching { gson.fromJson<List<VpnConfig>>(json, type) }.getOrDefault(emptyList())
    }

    suspend fun loadSourceStats(): Map<SubscriptionListType, Int> {
        val json = context.configDataStore.data.map { it[KEY_SOURCE_STATS] }.first() ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val raw = runCatching {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, type) as Map<String, Int>
        }.getOrElse { emptyMap() }
        return raw.mapNotNull { (name, count) ->
            runCatching { SubscriptionListType.valueOf(name) to count }.getOrNull()
        }.toMap()
    }

    suspend fun loadBuiltinTraffic(): Map<SubscriptionListType, SubscriptionTraffic> {
        val json = context.configDataStore.data.map { it[KEY_BUILTIN_TRAFFIC] }.first() ?: return emptyMap()
        val type = object : TypeToken<Map<String, SubscriptionTraffic>>() {}.type
        val raw = runCatching {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, type) as Map<String, SubscriptionTraffic>
        }.getOrElse { emptyMap() }
        return raw.mapNotNull { (name, traffic) ->
            runCatching { SubscriptionListType.valueOf(name) to traffic }.getOrNull()
        }.toMap()
    }

    suspend fun getContentHash(): String? {
        return context.configDataStore.data.map { it[KEY_HASH] }.first()
    }

    suspend fun getLastUpdatedAt(): Long {
        return context.configDataStore.data.map { it[KEY_UPDATED_AT] ?: 0L }.first()
    }

    suspend fun getPreferredMirror(): MirrorType? {
        val name = context.configDataStore.data.map { it[KEY_MIRROR] }.first() ?: return null
        return runCatching { MirrorType.valueOf(name) }.getOrNull()
    }

    suspend fun savePreferredMirror(mirror: MirrorType) {
        context.configDataStore.edit { prefs ->
            prefs[KEY_MIRROR] = mirror.name
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching { cacheFile.delete() }
        context.configDataStore.edit { it.clear() }
    }

    companion object {
        private const val CACHE_VERSION = 2
        private val KEY_CONFIGS_LEGACY = stringPreferencesKey("configs_json")
        private val KEY_CACHE_VERSION = intPreferencesKey("cache_version")
        private val KEY_HASH = stringPreferencesKey("content_hash")
        private val KEY_SOURCE_STATS = stringPreferencesKey("source_stats_json")
        private val KEY_BUILTIN_TRAFFIC = stringPreferencesKey("builtin_traffic_json")
        private val KEY_MIRROR = stringPreferencesKey("preferred_mirror")
        private val KEY_UPDATED_AT = longPreferencesKey("updated_at")
    }
}
