package com.remitos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OcrProcessorTest {
    @Test
    fun parseFields_extractsCuit() {
        val raw = "Remitente: ACME SA\nCUIT 20-12345678-9\n"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("20-12345678-9", fields["sender_cuit"])
        assertEquals(0.8f, confidence["sender_cuit"])
    }

    @Test
    fun parseFields_extractsBultos() {
        val raw = "Cantidad de bultos: 12\nOtro texto"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("12", fields["cant_bultos_total"])
        assertEquals(0.7f, confidence["cant_bultos_total"])
    }

    @Test
    fun parseFields_extractsRemitoCliente() {
        val raw = "Remito N° Cliente: RC-123\nOtro texto"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("RC-123", fields["remito_num_cliente"])
        assertEquals(0.6f, confidence["remito_num_cliente"])
    }

    @Test
    fun parseFields_extractsRemitoInterno() {
        val raw = "Remito N° Interno - 4567\nOtro texto"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("4567", fields["remito_num_interno"])
        assertEquals(0.6f, confidence["remito_num_interno"])
    }

    @Test
    fun parseFields_extractsRemitenteNombreApellido() {
        val raw = "Remitente: Maria Lopez\nCUIT 20-12345678-9"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("Maria", fields["sender_nombre"])
        assertEquals("Lopez", fields["sender_apellido"])
        assertEquals(0.6f, confidence["sender_nombre"])
        assertEquals(0.6f, confidence["sender_apellido"])
    }

    @Test
    fun parseFields_extractsDestinatarioNombreApellido() {
        val raw = "Destinatario: Juan Perez\nOtro texto"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("Juan", fields["dest_nombre"])
        assertEquals("Perez", fields["dest_apellido"])
        assertEquals(0.6f, confidence["dest_nombre"])
        assertEquals(0.6f, confidence["dest_apellido"])
    }

    @Test
    fun parseFields_extractsDestinatarioDireccionTelefono() {
        val raw = "Dirección Destinatario: Calle Falsa 123\nTeléfono Destinatario: 1122334455"

        val (fields, confidence) = OcrProcessor.parseFields(raw)

        assertEquals("Calle Falsa 123", fields["dest_direccion"])
        assertEquals("1122334455", fields["dest_telefono"])
        assertEquals(0.6f, confidence["dest_direccion"])
        assertEquals(0.6f, confidence["dest_telefono"])
    }

    @Test
    fun parseFields_ignoresMissingValues() {
        val raw = "Remitente: ACME SA\nDestinatario: Foo Bar"

        val (fields, _) = OcrProcessor.parseFields(raw)

        assertFalse(fields.containsKey("sender_cuit"))
        assertFalse(fields.containsKey("cant_bultos_total"))
    }
}
