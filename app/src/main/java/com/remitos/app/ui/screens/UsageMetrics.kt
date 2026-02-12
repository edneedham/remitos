package com.remitos.app.ui.screens

import com.remitos.app.ocr.OcrFieldKeys

internal fun isParseSuccessful(fields: Map<String, String>): Boolean {
    return fields.isNotEmpty()
}

internal fun hasManualCorrections(ocrFields: Map<String, String>, draft: InboundDraftState): Boolean {
    if (ocrFields.isEmpty()) return false

    val pairs = listOf(
        OcrFieldKeys.SenderCuit to draft.senderCuit,
        OcrFieldKeys.SenderNombre to draft.senderNombre,
        OcrFieldKeys.SenderApellido to draft.senderApellido,
        OcrFieldKeys.DestNombre to draft.destNombre,
        OcrFieldKeys.DestApellido to draft.destApellido,
        OcrFieldKeys.DestDireccion to draft.destDireccion,
        OcrFieldKeys.DestTelefono to draft.destTelefono,
        OcrFieldKeys.CantBultosTotal to draft.cantBultosTotal,
        OcrFieldKeys.RemitoNumCliente to draft.remitoNumCliente,
        OcrFieldKeys.RemitoNumInterno to draft.remitoNumInterno,
    )

    return pairs.any { (key, value) ->
        val original = ocrFields[key] ?: return@any false
        original.trim() != value.trim()
    }
}
