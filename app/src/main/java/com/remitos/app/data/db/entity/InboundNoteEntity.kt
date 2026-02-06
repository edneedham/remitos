package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbound_notes")
data class InboundNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sender_cuit")
    val senderCuit: String,
    @ColumnInfo(name = "sender_nombre")
    val senderNombre: String,
    @ColumnInfo(name = "sender_apellido")
    val senderApellido: String,
    @ColumnInfo(name = "dest_nombre")
    val destNombre: String,
    @ColumnInfo(name = "dest_apellido")
    val destApellido: String,
    @ColumnInfo(name = "dest_direccion")
    val destDireccion: String,
    @ColumnInfo(name = "dest_telefono")
    val destTelefono: String,
    @ColumnInfo(name = "cant_bultos_total")
    val cantBultosTotal: Int,
    @ColumnInfo(name = "remito_num_cliente")
    val remitoNumCliente: String,
    @ColumnInfo(name = "remito_num_interno")
    val remitoNumInterno: String,
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
