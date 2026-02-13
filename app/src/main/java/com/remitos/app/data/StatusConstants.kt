package com.remitos.app.data

object InboundPackageStatus {
    const val Disponible = "disponible"
    const val Asignado = "asignado"
    const val Anulado = "anulado"
}

object InboundNoteStatus {
    const val Activa = "activa"
    const val Anulada = "anulada"
}

object OutboundListStatus {
    const val Abierta = "abierta"
    const val Cerrada = "cerrada"
}

object OutboundLineStatus {
    const val Pendiente = "pendiente"
    const val EnDeposito = "en_deposito"
    const val EnTransito = "en_transito"
    const val Entregado = "entregado"
    const val Devuelto = "devuelto"
}
