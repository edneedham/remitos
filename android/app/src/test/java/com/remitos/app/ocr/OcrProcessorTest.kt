package com.remitos.app.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrProcessorTest {
    private val processor = OcrProcessor()

    @Test
    fun shouldApplyClahe_returnsTrueForLowContrast() {
        assertTrue(processor.shouldApplyClahe(12.0))
    }

    @Test
    fun shouldApplyClahe_returnsFalseForHighContrast() {
        assertFalse(processor.shouldApplyClahe(60.0))
    }
}
