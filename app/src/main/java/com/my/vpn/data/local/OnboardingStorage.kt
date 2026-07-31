package com.my.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

class OnboardingStorage(private val context: Context) {

    suspend fun isCompleted(): Boolean {
        return context.onboardingDataStore.data
            .map { it[KEY_COMPLETED] ?: false }
            .first()
    }

    suspend fun setCompleted(completed: Boolean = true) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_COMPLETED] = completed
        }
    }

    suspend fun hasSeenInstructionReminder(): Boolean {
        return context.onboardingDataStore.data
            .map { it[KEY_INSTRUCTION_REMINDER] ?: false }
            .first()
    }

    suspend fun setInstructionReminderSeen(seen: Boolean = true) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_INSTRUCTION_REMINDER] = seen
        }
    }

    companion object {
        private val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_INSTRUCTION_REMINDER = booleanPreferencesKey("instruction_reminder_seen")
    }
}
