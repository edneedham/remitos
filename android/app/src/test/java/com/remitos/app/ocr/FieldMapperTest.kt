package com.remitos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldMapperTest {

    private val mapper = FieldMapper()

    @Test
    fun mapFields_exactSynonymMatch() {
        val rawPairs = listOf(
            FieldPair("Destinatario", "SUPERMERCADO EL SOL"),
            FieldPair("CUIT", "30-12345678-9"),
        )

        val result = mapper.mapFields(rawPairs, emptyList())

        assertEquals("SUPERMERCADO EL SOL", result[OcrFieldKeys.DestNombre])
        assertEquals("30-12345678-9", result[OcrFieldKeys.SenderCuit])
    }

    @Test
    fun mapFields_trainingDataOverridesSynonym() {
        val rawPairs = listOf(
            FieldPair("Entregar a", "JUAN PEREZ"),
        )
        val trainingData = listOf(
            FieldMapping("entregar a", OcrFieldKeys.DestDireccion),
        )

        val result = mapper.mapFields(rawPairs, trainingData)

        // Training data overrides the synonym match (which would map to dest_nombre)
        assertEquals("JUAN PEREZ", result[OcrFieldKeys.DestDireccion])
    }

    @Test
    fun mapFields_fuzzyMatchForSimilarLabels() {
        val rawPairs = listOf(
            FieldPair("Destinatari", "JUAN PEREZ"), // typo: missing 'o'
            FieldPair("Direccion", "Calle 123"), // missing accent
        )

        val result = mapper.mapFields(rawPairs, emptyList())

        assertEquals("JUAN PEREZ", result[OcrFieldKeys.DestNombre])
        assertEquals("Calle 123", result[OcrFieldKeys.DestDireccion])
    }

    @Test
    fun mapFields_unmatchedLabelReturnsNull() {
        val rawPairs = listOf(
            FieldPair("Random label", "some value"),
        )

        val result = mapper.mapFields(rawPairs, emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun mapFields_cuitExtractionFromLongerValue() {
        val rawPairs = listOf(
            FieldPair("CUIT", "Empresa: 30-12345678-9 - Contribuyente"),
        )

        val result = mapper.mapFields(rawPairs, emptyList())

        assertEquals("30-12345678-9", result[OcrFieldKeys.SenderCuit])
    }

    @Test
    fun encodeDecodeTrainingData() {
        val mappings = listOf(
            FieldMapping("entregar a", OcrFieldKeys.DestDireccion),
            FieldMapping("ubicacion", OcrFieldKeys.DestDireccion),
        )

        val encoded = FieldMapper.encodeTrainingData(mappings)
        val decoded = FieldMapper.decodeTrainingData(encoded)

        assertEquals(2, decoded.size)
        assertEquals("entregar a", decoded[0].sourceLabel)
        assertEquals(OcrFieldKeys.DestDireccion, decoded[0].canonicalField)
        assertEquals("ubicacion", decoded[1].sourceLabel)
    }

    @Test
    fun decodeTrainingData_handlesEmptyString() {
        val decoded = FieldMapper.decodeTrainingData("")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun decodeTrainingData_handlesInvalidJson() {
        val decoded = FieldMapper.decodeTrainingData("not json")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun extractRawPairs_findsLabelValuePairs() {
        val text = """
            DISTRIBUIDORA SAN MARTIN S.R.L. REMITO
            Av. Gral. Paz 2154 - Cordoba
            C.U.I.T.: 30-12345678-9
            FECHA: 07/OCT/2024
            RAZON SOCIAL/NOMBRE: SUPERMERCADO EL SOL
            DOMICILIO: Belgrano 410
            LOCALIDAD: Rio Cuarto, CBA
            Tel: (0351) 422-3456
        """.trimIndent()

        val pairs = OcrProcessor.extractRawPairs(text)

        assertTrue(pairs.any { it.label == "C.U.I.T." && it.value == "30-12345678-9" })
        assertTrue(pairs.any { it.label == "FECHA" && it.value == "07/OCT/2024" })
        assertTrue(pairs.any { it.label == "RAZON SOCIAL/NOMBRE" && it.value == "SUPERMERCADO EL SOL" })
        assertTrue(pairs.any { it.label == "DOMICILIO" && it.value == "Belgrano 410" })
        assertTrue(pairs.any { it.label == "LOCALIDAD" && it.value == "Rio Cuarto, CBA" })
    }

    @Test
    fun extractRawPairs_doesNotSplitProductNamesWithHyphens() {
        val text = """
            Coca-Cola Original 2.25L
            Fanta Naranja 1.5L
            ITEM: 1
        """.trimIndent()

        val pairs = OcrProcessor.extractRawPairs(text)

        // Should NOT split "Coca-Cola" into label/value
        assertTrue(pairs.none { it.label == "Coca" })
        assertTrue(pairs.none { it.value.contains("Cola") && it.label == "Coca" })
        // But should still extract "ITEM: 1"
        assertTrue(pairs.any { it.label == "ITEM" && it.value == "1" })
    }

    @Test
    fun extractRawPairs_ignoresEmptyLines() {
        val text = """
            
            Some text without colon
            
            Label: Value
        """.trimIndent()

        val pairs = OcrProcessor.extractRawPairs(text)

        assertEquals(1, pairs.size)
        assertEquals("Label", pairs[0].label)
        assertEquals("Value", pairs[0].value)
    }
}
