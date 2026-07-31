package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.vpn.data.model.TrafficSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.trafficSessionStore: DataStore<Preferences> by preferencesDataStore(name = "traffic_sessions")

class TrafficSessionStorage(private val context: Context) {

    private val gson = Gson()
    private val listType = object : TypeToken<List<TrafficSession>>() {}.type

    val sessionsFlow: Flow<List<TrafficSession>> =
        context.trafficSessionStore.data.map { prefs ->
            parse(prefs[KEY_SESSIONS])
        }

    suspend fun getSessions(): List<TrafficSession> = sessionsFlow.first()

    suspend fun addSession(session: TrafficSession) {
        context.trafficSessionStore.edit { prefs ->
            val current = parse(prefs[KEY_SESSIONS]).toMutableList()
            current.add(0, session)
            prefs[KEY_SESSIONS] = gson.toJson(current.take(MAX_SESSIONS))
        }
    }

    suspend fun clear() {
        context.trafficSessionStore.edit { it.remove(KEY_SESSIONS) }
    }

    private fun parse(raw: String?): List<TrafficSession> =
        runCatching {
            if (raw.isNullOrBlank()) emptyList()
            else gson.fromJson<List<TrafficSession>>(raw, listType)
        }.getOrDefault(emptyList())

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("sessions_json")
        private const val MAX_SESSIONS = 100
    }
}
