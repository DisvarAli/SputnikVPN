package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.vpn.data.model.ConnectionBypassProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.perServerBypassStore: DataStore<Preferences> by preferencesDataStore(
    name = "per_server_bypass"
)

class PerServerBypassStorage(private val context: Context) {

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, ConnectionBypassProfile>>() {}.type

    suspend fun get(dedupeKey: String): ConnectionBypassProfile? {
        val map = loadMap()
        return map[dedupeKey]
    }

    suspend fun save(dedupeKey: String, profile: ConnectionBypassProfile) {
        val map = loadMap().toMutableMap()
        map[dedupeKey] = profile
        persist(map)
    }

    suspend fun clear() {
        context.perServerBypassStore.edit { it.remove(KEY_MAP) }
    }

    suspend fun exportAll(): Map<String, ConnectionBypassProfile> = loadMap()

    suspend fun importAll(map: Map<String, ConnectionBypassProfile>) {
        persist(map)
    }

    private suspend fun loadMap(): Map<String, ConnectionBypassProfile> {
        val raw = context.perServerBypassStore.data.map { it[KEY_MAP] }.first()
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, ConnectionBypassProfile>>(raw, type)
        }.getOrDefault(emptyMap())
    }

    private suspend fun persist(map: Map<String, ConnectionBypassProfile>) {
        context.perServerBypassStore.edit {
            it[KEY_MAP] = gson.toJson(map)
        }
    }

    companion object {
        private val KEY_MAP = stringPreferencesKey("bypass_by_server")
    }
}
