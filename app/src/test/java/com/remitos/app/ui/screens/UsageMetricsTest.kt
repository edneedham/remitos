package com.remitos.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageMetricsTest {
    @Test
    fun `parse success requires fields`() {
        assertFalse(isParseSuccessful(emptyMap()))
        assertTrue(isParseSuccessful(mapOf("sender_cuit" to "20-12345678-9")))
    }

    @Test
    fun `manual correction false when no ocr fields`() {
        val draft = InboundDraftState(senderCuit = "20-12345678-9")
        assertFalse(hasManualCorrections(emptyMap(), draft))
    }

    @Test
    fun `manual correction false when values match`() {
        val draft = InboundDraftState(
            senderCuit = "20-12345678-9",
            senderNombre = "Juan",
            senderApellido = "Perez",
        )
        val ocrFields = mapOf(
            "sender_cuit" to "20-12345678-9",
            "sender_nombre" to "Juan",
            "sender_apellido" to "Perez",
        )

        assertFalse(hasManualCorrections(ocrFields, draft))
    }

    @Test
    fun `manual correction true when value differs`() {
        val draft = InboundDraftState(
            senderCuit = "20-12345678-9",
            senderNombre = "Juan",
            senderApellido = "Gomez",
        )
        val ocrFields = mapOf(
            "sender_cuit" to "20-12345678-9",
            "sender_apellido" to "Perez",
        )

        assertTrue(hasManualCorrections(ocrFields, draft))
    }

    @Test
    fun `manual correction ignores whitespace differences`() {
        val draft = InboundDraftState(senderCuit = "20-12345678-9 ")
        val ocrFields = mapOf("sender_cuit" to "20-12345678-9")

        assertFalse(hasManualCorrections(ocrFields, draft))
    }
}
