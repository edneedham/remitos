package com.remitos.app.ui.screens

internal fun isParseSuccessful(fields: Map<String, String>): Boolean {
    return fields.isNotEmpty()
}

internal fun hasManualCorrections(ocrFields: Map<String, String>, draft: InboundDraftState): Boolean {
    if (ocrFields.isEmpty()) return false

    val pairs = listOf(
        "sender_cuit" to draft.senderCuit,
        "sender_nombre" to draft.senderNombre,
        "sender_apellido" to draft.senderApellido,
        "dest_nombre" to draft.destNombre,
        "dest_apellido" to draft.destApellido,
        "dest_direccion" to draft.destDireccion,
        "dest_telefono" to draft.destTelefono,
        "cant_bultos_total" to draft.cantBultosTotal,
        "remito_num_cliente" to draft.remitoNumCliente,
        "remito_num_interno" to draft.remitoNumInterno,
    )

    return pairs.any { (key, value) ->
        val original = ocrFields[key] ?: return@any false
        original.trim() != value.trim()
    }
}
