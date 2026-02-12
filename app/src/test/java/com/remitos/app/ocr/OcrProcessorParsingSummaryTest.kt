package com.remitos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrProcessorParsingSummaryTest {
    @Test
    fun `summary returns null when all fields present`() {
        val fields = mapOf(
            OcrFieldKeys.SenderCuit to "20-12345678-9",
            OcrFieldKeys.SenderNombre to "Juan",
            OcrFieldKeys.SenderApellido to "Perez",
            OcrFieldKeys.DestNombre to "Ana",
            OcrFieldKeys.DestApellido to "Gomez",
            OcrFieldKeys.DestDireccion to "Calle 123",
            OcrFieldKeys.DestTelefono to "1122334455",
            OcrFieldKeys.CantBultosTotal to "2",
            OcrFieldKeys.RemitoNumCliente to "A-1",
            OcrFieldKeys.RemitoNumInterno to "B-2",
        )

        assertNull(OcrProcessor.buildParsingErrorSummary(fields))
    }

    @Test
    fun `summary lists missing fields`() {
        val fields = mapOf(
            OcrFieldKeys.SenderCuit to "20-12345678-9",
            OcrFieldKeys.SenderNombre to "Juan",
            OcrFieldKeys.DestNombre to "Ana",
        )

        val summary = OcrProcessor.buildParsingErrorSummary(fields)
        assertEquals(
            "Faltan: Remitente, Destinatario, Dirección, Teléfono, Cantidad de bultos, Remito cliente, Remito interno",
            summary
        )
    }
}
