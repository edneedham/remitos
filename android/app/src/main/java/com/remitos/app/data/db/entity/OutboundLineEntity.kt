package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbound_lines",
    foreignKeys = [
        ForeignKey(
            entity = OutboundListEntity::class,
            parentColumns = ["id"],
            childColumns = ["outbound_list_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InboundNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["inbound_note_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("outbound_list_id"),
        Index("inbound_note_id"),
        Index("status"),
    ]
)
data class OutboundLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "outbound_list_id")
    val outboundListId: Long,
    @ColumnInfo(name = "inbound_note_id")
    val inboundNoteId: Long,
    @ColumnInfo(name = "delivery_number")
    val deliveryNumber: String,
    @ColumnInfo(name = "recipient_nombre")
    val recipientNombre: String,
    @ColumnInfo(name = "recipient_apellido")
    val recipientApellido: String,
    @ColumnInfo(name = "recipient_direccion")
    val recipientDireccion: String,
    @ColumnInfo(name = "recipient_telefono")
    val recipientTelefono: String,
    @ColumnInfo(name = "package_qty")
    val packageQty: Int,
    @ColumnInfo(name = "allocated_package_ids")
    val allocatedPackageIds: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "delivered_qty")
    val deliveredQty: Int,
    @ColumnInfo(name = "returned_qty")
    val returnedQty: Int,
    @ColumnInfo(name = "missing_qty")
    val missingQty: Int
)
