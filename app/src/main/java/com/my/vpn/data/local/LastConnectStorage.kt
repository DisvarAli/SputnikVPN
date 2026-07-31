package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lastConnectStore by preferencesDataStore(name = "last_connect")

class LastConnectStorage(private val context: Context) {

    suspend fun saveConfigId(id: String, displayName: String? = null) {
        context.lastConnectStore.edit { prefs ->
            prefs[KEY_CONFIG_ID] = id
            if (!displayName.isNullOrBlank()) {
                prefs[KEY_CONFIG_NAME] = displayName
            }
        }
    }

    suspend fun getConfigId(): String? =
        context.lastConnectStore.data.map { it[KEY_CONFIG_ID] }.first()

    suspend fun getConfigDisplayName(): String? =
        context.lastConnectStore.data.map { it[KEY_CONFIG_NAME] }.first()

    companion object {
        private val KEY_CONFIG_ID = stringPreferencesKey("last_config_id")
        private val KEY_CONFIG_NAME = stringPreferencesKey("last_config_name")
    }
}
