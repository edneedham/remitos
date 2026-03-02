package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_scanned_codes",
    foreignKeys = [
        ForeignKey(
            entity = LocalDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocalDocumentItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_item_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["document_id"]),
        Index(value = ["document_item_id"]),
        Index(value = ["raw_value"])
    ]
)
data class LocalScannedCodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "document_id")
    val documentId: String,
    @ColumnInfo(name = "document_item_id")
    val documentItemId: String?,
    @ColumnInfo(name = "raw_value")
    val rawValue: String,
    @ColumnInfo(name = "parsed_gtin")
    val parsedGtin: String?,
    @ColumnInfo(name = "parsed_sscc")
    val parsedSscc: String?,
    @ColumnInfo(name = "parsed_batch")
    val parsedBatch: String?,
    @ColumnInfo(name = "parsed_expiry")
    val parsedExpiry: Long?,
    @ColumnInfo(name = "matched")
    val matched: Boolean,
    @ColumnInfo(name = "scanned_by")
    val scannedBy: String?,
    @ColumnInfo(name = "scanned_at_local")
    val scannedAtLocal: Long,
    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
