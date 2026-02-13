package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.remitos.app.ocr.OcrFieldKeys

@Entity(tableName = "inbound_notes")
data class InboundNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = OcrFieldKeys.SenderCuit)
    val senderCuit: String,
    @ColumnInfo(name = OcrFieldKeys.SenderNombre)
    val senderNombre: String,
    @ColumnInfo(name = OcrFieldKeys.SenderApellido)
    val senderApellido: String,
    @ColumnInfo(name = OcrFieldKeys.DestNombre)
    val destNombre: String,
    @ColumnInfo(name = OcrFieldKeys.DestApellido)
    val destApellido: String,
    @ColumnInfo(name = OcrFieldKeys.DestDireccion)
    val destDireccion: String,
    @ColumnInfo(name = OcrFieldKeys.DestTelefono)
    val destTelefono: String,
    @ColumnInfo(name = OcrFieldKeys.CantBultosTotal)
    val cantBultosTotal: Int,
    @ColumnInfo(name = OcrFieldKeys.RemitoNumCliente)
    val remitoNumCliente: String,
    @ColumnInfo(name = OcrFieldKeys.RemitoNumInterno)
    val remitoNumInterno: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "scan_image_path")
    val scanImagePath: String?,
    @ColumnInfo(name = "ocr_text_blob")
    val ocrTextBlob: String?,
    @ColumnInfo(name = "ocr_confidence_json")
    val ocrConfidenceJson: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
