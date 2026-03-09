package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_logs")
data class DebugLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "scan_id")
    val scanId: Long?,
    @ColumnInfo(name = "ocr_confidence_json")
    val ocrConfidenceJson: String?,
    @ColumnInfo(name = "preprocess_time_ms")
    val preprocessTimeMs: Long?,
    @ColumnInfo(name = "failure_reason")
    val failureReason: String?,
    @ColumnInfo(name = "image_width")
    val imageWidth: Int?,
    @ColumnInfo(name = "image_height")
    val imageHeight: Int?,
    @ColumnInfo(name = "device_model")
    val deviceModel: String?,
    @ColumnInfo(name = "parsing_error_summary")
    val parsingErrorSummary: String?,
)
