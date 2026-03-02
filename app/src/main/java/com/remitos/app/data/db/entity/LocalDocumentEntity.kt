package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "local_documents",
    indices = [
        Index(value = ["warehouse_id"]),
        Index(value = ["status"]),
        Index(value = ["type"])
    ]
)
data class LocalDocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "cloud_id")
    val cloudId: String?,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "supplier_name")
    val supplierName: String?,
    @ColumnInfo(name = "document_number")
    val documentNumber: String?,
    @ColumnInfo(name = "ocr_confidence")
    val ocrConfidence: Double?,
    @ColumnInfo(name = "ocr_engine_used")
    val ocrEngineUsed: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_by")
    val createdBy: String?,
    @ColumnInfo(name = "created_at_local")
    val createdAtLocal: Long,
    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
