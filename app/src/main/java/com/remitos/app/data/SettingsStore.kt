package com.remitos.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val perspectiveCorrectionKey = booleanPreferencesKey("perspective_correction_enabled")

    val perspectiveCorrectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[perspectiveCorrectionKey] ?: true
    }

    suspend fun getPerspectiveCorrectionEnabled(): Boolean {
        return perspectiveCorrectionEnabled.first()
    }

    suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[perspectiveCorrectionKey] = enabled
        }
    }
}
