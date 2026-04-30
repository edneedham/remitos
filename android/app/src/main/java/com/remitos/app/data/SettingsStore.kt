package com.remitos.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.remitos.app.ocr.FieldMapper
import com.remitos.app.ocr.FieldMapping
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val perspectiveCorrectionKey = booleanPreferencesKey("perspective_correction_enabled")
    private val successfulParsesKey = longPreferencesKey("usage_successful_parses")
    private val manualCorrectionsKey = longPreferencesKey("usage_manual_corrections")

    // Template Configuration Keys
    private val logoUriKey = stringPreferencesKey("template_logo_uri")
    private val columnPesoKey = booleanPreferencesKey("template_column_peso")
    private val columnVolumenKey = booleanPreferencesKey("template_column_volumen")
    private val columnObservacionesKey = booleanPreferencesKey("template_column_observaciones")
    private val legalTextKey = stringPreferencesKey("template_legal_text")

    // Training Data Keys
    private val trainingDataKey = stringPreferencesKey("ocr_training_data")

    // Image Upload Settings
    private val imageUploadTimingKey = stringPreferencesKey("image_upload_timing")

    /** True after the user has saved at least one inbound remito (first-run onboarding complete). */
    private val firstInboundOnboardingDoneKey = booleanPreferencesKey("first_inbound_onboarding_done")

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

    val usageStats: Flow<DeviceUsageStats> = context.dataStore.data.map { prefs ->
        DeviceUsageStats(
            successfulParses = prefs[successfulParsesKey] ?: 0L,
            manualCorrections = prefs[manualCorrectionsKey] ?: 0L,
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

    val uploadTiming: Flow<UploadTiming> = context.dataStore.data.map { prefs ->
        val value = prefs[imageUploadTimingKey] ?: UploadTiming.IMMEDIATE.name
        UploadTiming.valueOf(value)
    }

    suspend fun getUploadTiming(): UploadTiming {
        return uploadTiming.first()
    }

    suspend fun setUploadTiming(timing: UploadTiming) {
        context.dataStore.edit { prefs ->
            prefs[imageUploadTimingKey] = timing.name
        }
    }

    val firstInboundOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[firstInboundOnboardingDoneKey] ?: false
    }

    suspend fun isFirstInboundOnboardingDone(): Boolean =
        firstInboundOnboardingDone.first()

    suspend fun setFirstInboundOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[firstInboundOnboardingDoneKey] = true
        }
    }
}

enum class UploadTiming {
    IMMEDIATE,   // Upload immediately after saving remito
    WIFI_ONLY    // Queue and upload only when on WiFi
}

data class DeviceUsageStats(
    val successfulParses: Long,
    val manualCorrections: Long,
)

data class TemplateConfig(
    val logoUri: String? = null,
    val showPeso: Boolean = true,
    val showVolumen: Boolean = true,
    val showObservaciones: Boolean = true,
    val legalText: String = ""
)
