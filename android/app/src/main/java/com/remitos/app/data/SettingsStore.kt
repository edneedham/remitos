package com.remitos.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.remitos.app.ocr.FieldMapper
import com.remitos.app.ocr.FieldMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val perspectiveCorrectionKey = booleanPreferencesKey("perspective_correction_enabled")
    private val totalScansKey = longPreferencesKey("usage_total_scans")
    private val successfulParsesKey = longPreferencesKey("usage_successful_parses")
    private val manualCorrectionsKey = longPreferencesKey("usage_manual_corrections")
    private val totalScanTimeMsKey = longPreferencesKey("usage_total_scan_time_ms")
    private val lastScanTimeMsKey = longPreferencesKey("usage_last_scan_time_ms")

    // Template Configuration Keys
    private val logoUriKey = stringPreferencesKey("template_logo_uri")
    private val columnPesoKey = booleanPreferencesKey("template_column_peso")
    private val columnVolumenKey = booleanPreferencesKey("template_column_volumen")
    private val columnObservacionesKey = booleanPreferencesKey("template_column_observaciones")
    private val legalTextKey = stringPreferencesKey("template_legal_text")

    // Training Data Keys
    private val trainingDataKey = stringPreferencesKey("ocr_training_data")

    val perspectiveCorrectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[perspectiveCorrectionKey] ?: true
    }

    val templateConfig: Flow<TemplateConfig> = context.dataStore.data.map { prefs ->
        TemplateConfig(
            logoUri = prefs[logoUriKey],
            showPeso = prefs[columnPesoKey] ?: true,
            showVolumen = prefs[columnVolumenKey] ?: true,
            showObservaciones = prefs[columnObservacionesKey] ?: true,
            legalText = prefs[legalTextKey] ?: ""
        )
    }

    suspend fun getTemplateConfig(): TemplateConfig {
        return templateConfig.first()
    }

    suspend fun setTemplateConfig(config: TemplateConfig) {
        context.dataStore.edit { prefs ->
            if (config.logoUri != null) {
                prefs[logoUriKey] = config.logoUri
            } else {
                prefs.remove(logoUriKey)
            }
            prefs[columnPesoKey] = config.showPeso
            prefs[columnVolumenKey] = config.showVolumen
            prefs[columnObservacionesKey] = config.showObservaciones
            prefs[legalTextKey] = config.legalText
        }
    }

    val usageStats: Flow<UsageStats> = context.dataStore.data.map { prefs ->
        UsageStats(
            totalScans = prefs[totalScansKey] ?: 0L,
            successfulParses = prefs[successfulParsesKey] ?: 0L,
            manualCorrections = prefs[manualCorrectionsKey] ?: 0L,
            totalScanTimeMs = prefs[totalScanTimeMsKey] ?: 0L,
            lastScanTimeMs = prefs[lastScanTimeMsKey] ?: 0L,
        )
    }

    suspend fun getPerspectiveCorrectionEnabled(): Boolean {
        return perspectiveCorrectionEnabled.first()
    }

    suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[perspectiveCorrectionKey] = enabled
        }
    }

    suspend fun recordScanStarted() {
        context.dataStore.edit { prefs ->
            val current = prefs[totalScansKey] ?: 0L
            prefs[totalScansKey] = current + 1
        }
    }

    suspend fun recordScanResult(durationMs: Long, parseSuccess: Boolean) {
        context.dataStore.edit { prefs ->
            val totalTime = (prefs[totalScanTimeMsKey] ?: 0L) + durationMs
            prefs[totalScanTimeMsKey] = totalTime
            prefs[lastScanTimeMsKey] = durationMs
            if (parseSuccess) {
                val current = prefs[successfulParsesKey] ?: 0L
                prefs[successfulParsesKey] = current + 1
            }
        }
    }

    suspend fun recordManualCorrection() {
        context.dataStore.edit { prefs ->
            val current = prefs[manualCorrectionsKey] ?: 0L
            prefs[manualCorrectionsKey] = current + 1
        }
    }

    val trainingData: Flow<List<FieldMapping>> = context.dataStore.data.map { prefs ->
        FieldMapper.decodeTrainingData(prefs[trainingDataKey] ?: "")
    }

    suspend fun getTrainingData(): List<FieldMapping> {
        return trainingData.first()
    }

    suspend fun addTrainingMapping(sourceLabel: String, canonicalField: String) {
        context.dataStore.edit { prefs ->
            val current = FieldMapper.decodeTrainingData(prefs[trainingDataKey] ?: "")
            val updated = current
                .filter { it.sourceLabel.lowercase() != sourceLabel.lowercase() }
                .toMutableList()
            updated.add(FieldMapping(sourceLabel = sourceLabel, canonicalField = canonicalField))
            prefs[trainingDataKey] = FieldMapper.encodeTrainingData(updated)
        }
    }
}

data class UsageStats(
    val totalScans: Long,
    val successfulParses: Long,
    val manualCorrections: Long,
    val totalScanTimeMs: Long,
    val lastScanTimeMs: Long,
)

data class TemplateConfig(
    val logoUri: String? = null,
    val showPeso: Boolean = true,
    val showVolumen: Boolean = true,
    val showObservaciones: Boolean = true,
    val legalText: String = ""
)
