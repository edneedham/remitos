package com.remitos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrProcessorGeneralParsingTest {
    @Test
    fun `parses factura labels into fields`() {
        val raw = """
            FACTURA A
            Proveedor: Acme SA
            CUIT: 30-12345678-9
            Cliente: Juan Perez
            Domicilio: Calle Falsa 123
            Tel: 11 2345-6789
            Factura N°: A-0001-00012345
            Bultos: 3
        """.trimIndent()

        val (fields, _) = OcrProcessor.parseFields(raw)

        assertEquals("30-12345678-9", fields[OcrFieldKeys.SenderCuit])
        assertEquals("Acme", fields[OcrFieldKeys.SenderNombre])
        assertEquals("SA", fields[OcrFieldKeys.SenderApellido])
        assertEquals("Juan", fields[OcrFieldKeys.DestNombre])
        assertEquals("Perez", fields[OcrFieldKeys.DestApellido])
        assertEquals("Calle Falsa 123", fields[OcrFieldKeys.DestDireccion])
        assertEquals("112345-6789", fields[OcrFieldKeys.DestTelefono])
        assertEquals("A-0001-00012345", fields[OcrFieldKeys.RemitoNumCliente])
        assertEquals("3", fields[OcrFieldKeys.CantBultosTotal])
    }

    @Test
    fun `parses labels on next line without stealing other labels`() {
        val raw = """
            Nota de entrega
            Destinatario:
            Maria Lopez
            Direccion de entrega:
            Av Siempre Viva 742
            Remito cliente
            R-778
        """.trimIndent()

        val (fields, _) = OcrProcessor.parseFields(raw)

        assertNull(fields[OcrFieldKeys.SenderNombre])
        assertEquals("Maria", fields[OcrFieldKeys.DestNombre])
        assertEquals("Lopez", fields[OcrFieldKeys.DestApellido])
        assertEquals("Av Siempre Viva 742", fields[OcrFieldKeys.DestDireccion])
        assertEquals("R-778", fields[OcrFieldKeys.RemitoNumCliente])
    }

    @Test
    fun `falls back to regex for cuit and bultos`() {
        val raw = """
            CUIT 20-11112222-3
            Cantidad de bultos: 5
        """.trimIndent()

        val (fields, _) = OcrProcessor.parseFields(raw)

        assertEquals("20-11112222-3", fields[OcrFieldKeys.SenderCuit])
        assertEquals("5", fields[OcrFieldKeys.CantBultosTotal])
    }
}
