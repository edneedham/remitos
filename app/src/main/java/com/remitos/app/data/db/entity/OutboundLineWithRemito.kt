package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo

data class OutboundLineWithRemito(
    val id: Long,
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
    @ColumnInfo(name = "delivered_qty")
    val deliveredQty: Int,
    @ColumnInfo(name = "returned_qty")
    val returnedQty: Int,
    @ColumnInfo(name = "remito_num_cliente")
    val remitoNumCliente: String
)
