package com.familysms.forwarder.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "family_sms_settings"
)

data class AppSettings(
    val isMasterEnabled: Boolean = false,
    val destinationNumber: String = "",
    val includeTimestamp: Boolean = false,
    val dedupeWindowSeconds: Int = 60,
    val notifyOnError: Boolean = true,
    val notifyOnForwarded: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val DESTINATION_NUMBER = stringPreferencesKey("destination_number")
        val INCLUDE_TIMESTAMP = booleanPreferencesKey("include_timestamp")
        val DEDUPE_WINDOW_SECONDS = intPreferencesKey("dedupe_window_seconds")
        val NOTIFY_ON_ERROR = booleanPreferencesKey("notify_on_error")
        val NOTIFY_ON_FORWARDED = booleanPreferencesKey("notify_on_forwarded")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            isMasterEnabled = prefs[Keys.MASTER_ENABLED] ?: false,
            destinationNumber = prefs[Keys.DESTINATION_NUMBER] ?: "",
            includeTimestamp = prefs[Keys.INCLUDE_TIMESTAMP] ?: false,
            dedupeWindowSeconds = prefs[Keys.DEDUPE_WINDOW_SECONDS] ?: 60,
            notifyOnError = prefs[Keys.NOTIFY_ON_ERROR] ?: true,
            notifyOnForwarded = prefs[Keys.NOTIFY_ON_FORWARDED] ?: false
        )
    }

    suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

    suspend fun setMasterEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.MASTER_ENABLED] = enabled
        }
    }

    suspend fun setDestinationNumber(number: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DESTINATION_NUMBER] = number.trim()
        }
    }

    suspend fun setIncludeTimestamp(include: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.INCLUDE_TIMESTAMP] = include
        }
    }

    suspend fun setDedupeWindowSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DEDUPE_WINDOW_SECONDS] = seconds.coerceIn(10, 600)
        }
    }

    suspend fun setNotifyOnError(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NOTIFY_ON_ERROR] = enabled
        }
    }

    suspend fun setNotifyOnForwarded(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NOTIFY_ON_FORWARDED] = enabled
        }
    }
}
