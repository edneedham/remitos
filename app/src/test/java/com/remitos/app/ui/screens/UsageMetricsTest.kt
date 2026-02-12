package com.remitos.app.ui.screens

import com.remitos.app.ocr.OcrFieldKeys

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageMetricsTest {
    @Test
    fun `parse success requires fields`() {
        assertFalse(isParseSuccessful(emptyMap()))
        assertTrue(isParseSuccessful(mapOf(OcrFieldKeys.SenderCuit to "20-12345678-9")))
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
            OcrFieldKeys.SenderCuit to "20-12345678-9",
            OcrFieldKeys.SenderNombre to "Juan",
            OcrFieldKeys.SenderApellido to "Perez",
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
            OcrFieldKeys.SenderCuit to "20-12345678-9",
            OcrFieldKeys.SenderApellido to "Perez",
        )

        assertTrue(hasManualCorrections(ocrFields, draft))
    }

    @Test
    fun `manual correction ignores whitespace differences`() {
        val draft = InboundDraftState(senderCuit = "20-12345678-9 ")
        val ocrFields = mapOf(OcrFieldKeys.SenderCuit to "20-12345678-9")

        assertFalse(hasManualCorrections(ocrFields, draft))
    }
}
