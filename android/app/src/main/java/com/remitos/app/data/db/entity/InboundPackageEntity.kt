package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inbound_packages",
    foreignKeys = [
        ForeignKey(
            entity = InboundNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["inbound_note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inbound_note_id")]
)
data class InboundPackageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "inbound_note_id")
    val inboundNoteId: Long,
    @ColumnInfo(name = "package_index")
    val packageIndex: Int,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "barcode_raw")
    val barcodeRaw: String? = null,
    @ColumnInfo(name = "gtin")
    val gtin: String? = null,
    @ColumnInfo(name = "batch_lot")
    val batchLot: String? = null,
    @ColumnInfo(name = "expiry_date")
    val expiryDate: String? = null,
    @ColumnInfo(name = "sscc")
    val sscc: String? = null,
    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null,
    @ColumnInfo(name = "scanned_at")
    val scannedAt: Long? = null,
    @ColumnInfo(name = "scanned_by")
    val scannedBy: String? = null
)
