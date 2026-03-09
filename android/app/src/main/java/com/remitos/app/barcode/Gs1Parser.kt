package com.remitos.app.barcode

/**
 * Data class representing parsed GS1 barcode information.
 */
data class Gs1ParsedData(
    val gtin: String? = null,
    val sscc: String? = null,
    val batch: String? = null,
    val expiryDate: String? = null,
    val serialNumber: String? = null,
    val productionDate: String? = null,
    val rawBarcode: String
) {
    /**
     * Returns true if any GS1 fields were successfully parsed.
     */
    fun hasGs1Data(): Boolean = gtin != null || sscc != null || batch != null ||
            expiryDate != null || serialNumber != null || productionDate != null
}

/**
 * GS1 Application Identifiers (AI) constants.
 */
object Gs1ApplicationIdentifiers {
    const val SSCC = "00"           // Serial Shipping Container Code (18 digits)
    const val GTIN = "01"           // Global Trade Item Number (14 digits)
    const val CONTENT_GTIN = "02"   // GTIN of contained trade items (14 digits)
    const val BATCH_LOT = "10"      // Batch or Lot number (variable, ends with FNC1)
    const val PRODUCTION_DATE = "11"  // Production date (YYMMDD - 6 digits)
    const val EXPIRY_DATE = "17"    // Expiration date (YYMMDD - 6 digits)
    const val SERIAL_NUMBER = "21"  // Serial number (variable, ends with FNC1)
    const val COUNT = "37"          // Count of trade items (variable, ends with FNC1)
}

/**
 * Parser for GS1-128 and other GS1 barcode standards.
 *
 * Supports:
 * - GS1-128 (Code 128 with FNC1)
 * - GS1 DataBar
 * - GS1 DataMatrix
 * - GS1 QR Code
 */
class Gs1Parser {

    companion object {
        private const val FNC1 = '\u001D'  // ASCII GS (Group Separator) used as FNC1
        private const val FNC1_ALT = ']'    // Some scanners use ] as FNC1 indicator
    }

    /**
     * Parse barcode data and extract GS1 Application Identifiers.
     *
     * @param barcodeData The raw barcode string
     * @return Gs1ParsedData containing extracted fields and raw barcode
     */
    fun parse(barcodeData: String): Gs1ParsedData {
        if (barcodeData.isBlank()) {
            return Gs1ParsedData(rawBarcode = barcodeData)
        }

        // Normalize FNC1 characters
        val normalized = barcodeData
            .replace(FNC1_ALT.toString(), FNC1.toString())
            .replace(FNC1.toString(), FNC1.toString())

        // Try to parse as GS1 format
        val parsed = parseGs1Format(normalized)

        // If no GS1 data found, try to detect simple formats
        if (!parsed.hasGs1Data()) {
            return detectSimpleFormat(barcodeData)
        }

        return parsed.copy(rawBarcode = barcodeData)
    }

    /**
     * Parse GS1-128 format with Application Identifiers.
     */
    private fun parseGs1Format(data: String): Gs1ParsedData {
        var remaining = data
        var gtin: String? = null
        var sscc: String? = null
        var batch: String? = null
        var expiryDate: String? = null
        var serialNumber: String? = null
        var productionDate: String? = null

        // Remove leading ]C1 or similar symbology identifiers
        remaining = remaining.replace(Regex("^\\][A-Z][0-9]"), "")

        while (remaining.isNotEmpty()) {
            // Find the next AI (2 or 3 digits at the start)
            val aiMatch = Regex("^(\\d{2,3})").find(remaining)
                ?: break

            val ai = aiMatch.groupValues[1]
            remaining = remaining.substring(ai.length)

            when (ai) {
                Gs1ApplicationIdentifiers.GTIN -> {
                    gtin = extractFixedLength(remaining, 14)
                    remaining = remaining.drop(14)
                }
                Gs1ApplicationIdentifiers.SSCC -> {
                    sscc = extractFixedLength(remaining, 18)
                    remaining = remaining.drop(18)
                }
                Gs1ApplicationIdentifiers.CONTENT_GTIN -> {
                    // Skip content GTIN or use it if no main GTIN
                    if (gtin == null) {
                        gtin = extractFixedLength(remaining, 14)
                    }
                    remaining = remaining.drop(14)
                }
                Gs1ApplicationIdentifiers.BATCH_LOT -> {
                    val (value, rest) = extractVariableLength(remaining)
                    batch = value
                    remaining = rest
                }
                Gs1ApplicationIdentifiers.EXPIRY_DATE -> {
                    expiryDate = extractFixedLength(remaining, 6)
                    remaining = remaining.drop(6)
                }
                Gs1ApplicationIdentifiers.PRODUCTION_DATE -> {
                    productionDate = extractFixedLength(remaining, 6)
                    remaining = remaining.drop(6)
                }
                Gs1ApplicationIdentifiers.SERIAL_NUMBER -> {
                    val (value, rest) = extractVariableLength(remaining)
                    serialNumber = value
                    remaining = rest
                }
                Gs1ApplicationIdentifiers.COUNT -> {
                    // Extract count but we don't store it currently
                    val (value, rest) = extractVariableLength(remaining)
                    remaining = rest
                }
                else -> {
                    // Unknown AI - try to skip it
                    // For unknown AIs, assume fixed length if next chars are digits
                    if (remaining.length >= 2 && remaining[0].isDigit()) {
                        // Try to determine if it's fixed or variable length
                        remaining = skipUnknownAi(remaining)
                    } else {
                        break
                    }
                }
            }
        }

        return Gs1ParsedData(
            gtin = gtin,
            sscc = sscc,
            batch = batch,
            expiryDate = formatExpiryDate(expiryDate),
            serialNumber = serialNumber,
            productionDate = formatProductionDate(productionDate),
            rawBarcode = data
        )
    }

    /**
     * Detect simple barcode formats (GTIN-13, GTIN-14, SSCC-18, etc.)
     */
    private fun detectSimpleFormat(data: String): Gs1ParsedData {
        val cleanData = data.trim()

        return when {
            // GTIN-14 (14 digits, often starts with 0-9)
            cleanData.length == 14 && cleanData.all { it.isDigit() } -> {
                Gs1ParsedData(
                    gtin = cleanData,
                    rawBarcode = data
                )
            }
            // GTIN-13 (EAN-13, 13 digits)
            cleanData.length == 13 && cleanData.all { it.isDigit() } -> {
                Gs1ParsedData(
                    gtin = "0$cleanData",  // Convert to GTIN-14
                    rawBarcode = data
                )
            }
            // GTIN-12 (UPC-A, 12 digits)
            cleanData.length == 12 && cleanData.all { it.isDigit() } -> {
                Gs1ParsedData(
                    gtin = "00$cleanData",  // Convert to GTIN-14
                    rawBarcode = data
                )
            }
            // SSCC-18 (18 digits, starts with 0-9)
            cleanData.length == 18 && cleanData.all { it.isDigit() } -> {
                Gs1ParsedData(
                    sscc = cleanData,
                    rawBarcode = data
                )
            }
            // EAN-8 (8 digits)
            cleanData.length == 8 && cleanData.all { it.isDigit() } -> {
                Gs1ParsedData(
                    gtin = "000000$cleanData",  // Convert to GTIN-14
                    rawBarcode = data
                )
            }
            // Unknown format - store raw only
            else -> {
                Gs1ParsedData(rawBarcode = data)
            }
        }
    }

    /**
     * Extract fixed-length field.
     */
    private fun extractFixedLength(data: String, length: Int): String? {
        return if (data.length >= length) {
            data.substring(0, length)
        } else {
            null
        }
    }

    /**
     * Extract variable-length field (ends with FNC1 or end of string).
     * Returns pair of (value, remaining_string).
     */
    private fun extractVariableLength(data: String): Pair<String?, String> {
        val fnc1Index = data.indexOf(FNC1)
        return if (fnc1Index >= 0) {
            Pair(data.substring(0, fnc1Index), data.substring(fnc1Index + 1))
        } else {
            // Check if next part starts with an AI (2+ digits followed by data)
            val nextAiMatch = Regex("^\\d{2}").find(data)
            if (nextAiMatch != null && nextAiMatch.range.first > 0) {
                // There's likely a new AI starting, so current field ends here
                Pair(data.substring(0, nextAiMatch.range.first), data.substring(nextAiMatch.range.first))
            } else {
                Pair(data, "")
            }
        }
    }

    /**
     * Skip unknown AI by trying to detect its length.
     */
    private fun skipUnknownAi(data: String): String {
        // Try to find where the next known AI starts
        val knownAis = listOf(
            Gs1ApplicationIdentifiers.GTIN,
            Gs1ApplicationIdentifiers.SSCC,
            Gs1ApplicationIdentifiers.BATCH_LOT,
            Gs1ApplicationIdentifiers.EXPIRY_DATE,
            Gs1ApplicationIdentifiers.SERIAL_NUMBER
        )

        for (knownAi in knownAis) {
            val index = data.indexOf(knownAi)
            if (index > 0) {
                return data.substring(index)
            }
        }

        // If no known AI found, skip 2 characters and continue
        return data.drop(2)
    }

    /**
     * Format expiry date from YYMMDD to DD/MM/YYYY.
     */
    private fun formatExpiryDate(date: String?): String? {
        if (date == null || date.length != 6) return date

        val year = date.substring(0, 2).toInt()
        val month = date.substring(2, 4)
        val day = date.substring(4, 6)

        // Determine century (assume 00-49 = 2000-2049, 50-99 = 1950-1999)
        val fullYear = if (year < 50) 2000 + year else 1900 + year

        return "$day/$month/$fullYear"
    }

    /**
     * Format production date from YYMMDD to DD/MM/YYYY.
     */
    private fun formatProductionDate(date: String?): String? {
        return formatExpiryDate(date)  // Same format
    }
}
