package com.remitos.app.ocr

data class FieldMapping(
    val sourceLabel: String,
    val canonicalField: String,
)

class FieldMapper {

    private val synonymMap: Map<String, String> = buildSynonymMap()

    fun mapFields(
        rawPairs: List<FieldPair>,
        trainingData: List<FieldMapping>,
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()

        for (pair in rawPairs) {
            val normalizedLabel = normalize(pair.label)
            val canonicalKey = when {
                // Layer 1: Training data (learned from user corrections)
                trainingData.any { normalize(it.sourceLabel) == normalizedLabel } -> {
                    trainingData.first { normalize(it.sourceLabel) == normalizedLabel }.canonicalField
                }
                // Layer 2: Exact synonym match
                synonymMap.containsKey(normalizedLabel) -> {
                    synonymMap[normalizedLabel]!!
                }
                // Layer 3: Fuzzy match against known synonyms
                else -> {
                    findFuzzyMatch(normalizedLabel)
                }
            }

            if (canonicalKey != null) {
                if (canonicalKey == OcrFieldKeys.SenderCuit) {
                    val cuitMatch = Regex("\\b\\d{2}-\\d{8}-\\d{1}\\b").find(pair.value)
                    if (cuitMatch != null) {
                        result[canonicalKey] = cuitMatch.value
                    }
                } else {
                    result[canonicalKey] = pair.value.trim()
                }
            }
        }

        return result
    }

    fun mapToFieldPairs(
        rawPairs: List<FieldPair>,
        trainingData: List<FieldMapping>,
    ): List<FieldPair> {
        return rawPairs.map { pair ->
            val normalizedLabel = normalize(pair.label)
            val canonicalKey = trainingData.firstOrNull { normalize(it.sourceLabel) == normalizedLabel }?.canonicalField
                ?: synonymMap[normalizedLabel]
                ?: findFuzzyMatch(normalizedLabel)

            if (canonicalKey != null) {
                FieldPair(label = canonicalKey, value = pair.value)
            } else {
                pair
            }
        }
    }

    private fun normalize(label: String): String {
        return label
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace(Regex("[^a-z\\s]"), "")
            .trim()
    }

    private fun findFuzzyMatch(normalizedLabel: String): String? {
        val bestMatch = synonymMap.entries
            .minByOrNull { (synonym, _) ->
                levenshteinDistance(normalizedLabel, synonym)
            }

        return if (bestMatch != null) {
            val distance = levenshteinDistance(normalizedLabel, bestMatch.key)
            val maxLen = maxOf(normalizedLabel.length, bestMatch.key.length)
            val similarity = 1.0 - (distance.toDouble() / maxLen)
            if (similarity >= FUZZY_THRESHOLD) bestMatch.value else null
        } else {
            null
        }
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }

    companion object {
        private const val FUZZY_THRESHOLD = 0.7
        private const val DELIMITER = "\n"
        private const val SEPARATOR = "|"

        fun encodeTrainingData(mappings: List<FieldMapping>): String {
            return mappings.joinToString(DELIMITER) { "${it.sourceLabel}$SEPARATOR${it.canonicalField}" }
        }

        fun decodeTrainingData(data: String): List<FieldMapping> {
            if (data.isBlank()) return emptyList()
            return data.split(DELIMITER)
                .filter { it.contains(SEPARATOR) }
                .mapNotNull { line ->
                    val parts = line.split(SEPARATOR, limit = 2)
                    if (parts.size == 2) {
                        FieldMapping(sourceLabel = parts[0], canonicalField = parts[1])
                    } else {
                        null
                    }
                }
        }

        private fun buildSynonymMap(): Map<String, String> {
            val map = mutableMapOf<String, String>()

            // Sender CUIT
            listOf("cuit", "cuil", "cuit cuil", "cuit remitente", "cuit proveedor", "cuit emisor",
                "c u i t").forEach { map[it] = OcrFieldKeys.SenderCuit }

            // Sender name
            listOf("remitente", "proveedor", "emisor", "vendedor", "razon social remitente",
                "razon social proveedor", "desde", "origen").forEach { map[it] = OcrFieldKeys.SenderNombre }

            // Dest name
            listOf("razon social nombre", "razon social", "nombre", "destinatario", "cliente",
                "receptor", "comprador", "consignatario", "entregar a", "enviar a",
                "destino", "hasta").forEach { map[it] = OcrFieldKeys.DestNombre }

            // Dest address
            listOf("direccion", "domicilio", "direccion de entrega", "domicilio de entrega",
                "direccion destinatario", "domicilio destinatario", "direccion cliente",
                "domicilio cliente", "lugar de entrega", "direccion de envio").forEach {
                map[it] = OcrFieldKeys.DestDireccion
            }

            // Localidad/City
            listOf("localidad", "ciudad", "provincia", "cp", "codigo postal").forEach {
                map[it] = OcrFieldKeys.Localidad
            }

            // Transportista/Carrier
            listOf("transportista", "carrier", "expreso", "flete", "logistica").forEach {
                map[it] = OcrFieldKeys.Transportista
            }

            // IVA Condicion
            listOf("iva", "condicion iva", "iibb", "i v a", "iva condicion").forEach {
                map[it] = OcrFieldKeys.IvaCondicion
            }

            // Fecha/Date
            listOf("fecha", "date", "dia", "fecha emision", "fecha de emision").forEach {
                map[it] = OcrFieldKeys.Fecha
            }

            // Recibido por
            listOf("recibi conforme", "recibio", "firma", "recibido por", "conforme").forEach {
                map[it] = OcrFieldKeys.RecibidoPor
            }

            // Dest phone
            listOf("telefono", "tel", "celular", "contacto", "cel", "phone").forEach {
                map[it] = OcrFieldKeys.DestTelefono
            }

            // Bultos
            listOf("cantidad de bultos", "cant bultos", "cant.", "cant", "bultos",
                "cantidad", "qty", "piezas", "unidades").forEach {
                map[it] = OcrFieldKeys.CantBultosTotal
            }

            // Document number
            listOf("remito cliente", "remito", "nota de entrega", "guia de despacho",
                "orden de entrega", "factura", "comprobante", "documento",
                "numero", "nro", "num").forEach {
                map[it] = OcrFieldKeys.RemitoNumCliente
            }

            return map
        }
    }
}
