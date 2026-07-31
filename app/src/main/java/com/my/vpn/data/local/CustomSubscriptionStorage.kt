package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.vpn.data.model.CustomSubscription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.customSubsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "custom_subscriptions"
)

class CustomSubscriptionStorage(private val context: Context) {

    private val gson = Gson()

    suspend fun loadAll(): List<CustomSubscription> {
        val json = context.customSubsDataStore.data.map { it[KEY_LIST] }.first() ?: return emptyList()
        val type = object : TypeToken<List<CustomSubscription>>() {}.type
        return runCatching { gson.fromJson<List<CustomSubscription>>(json, type) }.getOrDefault(emptyList())
    }

    suspend fun saveAll(subs: List<CustomSubscription>) {
        context.customSubsDataStore.edit { prefs ->
            prefs[KEY_LIST] = gson.toJson(subs)
        }
    }

    suspend fun add(sub: CustomSubscription) {
        val current = loadAll().toMutableList()
        current.add(sub)
        saveAll(current)
    }

    suspend fun remove(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    suspend fun replaceAll(subs: List<CustomSubscription>) {
        saveAll(subs)
    }

    suspend fun updateTraffic(id: String, traffic: com.my.vpn.data.model.SubscriptionTraffic) {
        saveAll(
            loadAll().map { sub ->
                if (sub.id == id) sub.copy(traffic = traffic) else sub
            }
        )
    }

    companion object {
        private val KEY_LIST = stringPreferencesKey("custom_subs_json")
    }
}
