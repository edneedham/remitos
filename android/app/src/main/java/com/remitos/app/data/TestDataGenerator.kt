package com.remitos.app.data

import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Generator for realistic test/demonstration data.
 * Creates sample remitos with believable data for testing and demos.
 */
class TestDataGenerator(private val repository: RemitosRepository) {

    companion object {
        private val SUPPLIERS = listOf(
            Triple("30-12345678-9", "Distribuidora", "Sur S.A."),
            Triple("30-98765432-1", "Logística", "Express SRL"),
            Triple("30-55555555-5", "Almacén", "Central S.A."),
            Triple("30-11111111-1", "Importadora", "Norte SRL"),
            Triple("30-22222222-2", "Mayorista", "Buenos Aires S.A.")
        )

        private val DESTINATIONS = listOf(
            Quadruple("María", "González", "Av. Corrientes 1234, CABA", "11-5555-1234"),
            Quadruple("Juan", "Pérez", "Calle Florida 567, CABA", "11-5555-5678"),
            Quadruple("Ana", "Rodríguez", "Av. Santa Fe 890, Palermo", "11-5555-9012"),
            Quadruple("Carlos", "López", "Calle Córdoba 345, Villa Crespo", "11-5555-3456"),
            Quadruple("Laura", "Martínez", "Av. Libertador 678, Belgrano", "11-5555-7890"),
            Quadruple("Diego", "Sánchez", "Calle Cabildo 901, Núñez", "11-5555-2345"),
            Quadruple("Valentina", "Fernández", "Av. Cabildo 234, Belgrano", "11-5555-6789"),
            Quadruple("Martín", "Gómez", "Calle Thames 456, Villa Crespo", "11-5555-0123")
        )

        private val PRODUCT_TYPES = listOf(
            "Electrodomésticos",
            "Ropa y accesorios",
            "Alimentos",
            "Muebles",
            "Tecnología",
            "Herramientas",
            "Juguetes",
            "Libros"
        )
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Generate a set of realistic test remitos.
     * Creates 5-8 remitos with varying states and dates.
     */
    suspend fun generateTestData() {
        withContext(Dispatchers.IO) {
            // Check if data already exists
            val existingNotes = mutableListOf<InboundNoteEntity>()
            repository.observeInboundNotes().collect { notes ->
                existingNotes.addAll(notes)
            }
            
            if (existingNotes.isNotEmpty()) {
                return@withContext // Don't generate if data already exists
            }

            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            val oneWeek = 7 * oneDay

            // Generate 6 realistic remitos
            val testRemitos = listOf(
                createRemito(
                    supplierIndex = 0,
                    destinationIndex = 0,
                    packageCount = 3,
                    createdAt = now - (2 * oneDay),
                    scannedCount = 3,  // Fully scanned
                    status = InboundNoteStatus.Activa
                ),
                createRemito(
                    supplierIndex = 1,
                    destinationIndex = 1,
                    packageCount = 5,
                    createdAt = now - (5 * oneDay),
                    scannedCount = 5,
                    status = InboundNoteStatus.Activa
                ),
                createRemito(
                    supplierIndex = 2,
                    destinationIndex = 2,
                    packageCount = 2,
                    createdAt = now - oneWeek,
                    scannedCount = 0,  // Not scanned yet
                    status = InboundNoteStatus.Activa
                ),
                createRemito(
                    supplierIndex = 3,
                    destinationIndex = 3,
                    packageCount = 8,
                    createdAt = now - (3 * oneDay),
                    scannedCount = 6,  // Partially scanned
                    status = InboundNoteStatus.Activa
                ),
                createRemito(
                    supplierIndex = 4,
                    destinationIndex = 4,
                    packageCount = 4,
                    createdAt = now - (10 * oneDay),
                    scannedCount = 0,
                    status = InboundNoteStatus.Anulada  // Voided
                ),
                createRemito(
                    supplierIndex = 0,
                    destinationIndex = 5,
                    packageCount = 6,
                    createdAt = now - (1 * oneDay),
                    scannedCount = 2,  // Partially scanned
                    status = InboundNoteStatus.Activa
                )
            )

            testRemitos.forEach { (note, scannedCount) ->
                val noteId = repository.createInboundNote(note)
                
                // If this remito should have scanned barcodes, scan them
                if (scannedCount > 0) {
                    val packages = repository.getPackagesForNote(noteId)
                    packages.take(scannedCount).forEach { pkg ->
                        val gs1Barcode = generateGs1Barcode(
                            gtin = "779${(100000000..999999999).random()}",
                            batch = "L${(1000..9999).random()}",
                            expiry = generateFutureDate()
                        )
                        repository.updatePackage(
                            pkg.copy(
                                barcodeRaw = gs1Barcode,
                                gtin = extractGtin(gs1Barcode),
                                batchLot = extractBatch(gs1Barcode),
                                expiryDate = extractExpiry(gs1Barcode),
                                scannedAt = note.createdAt + (1000..3600000).random(),
                                scannedBy = "usuario_demo"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createRemito(
        supplierIndex: Int,
        destinationIndex: Int,
        packageCount: Int,
        createdAt: Long,
        scannedCount: Int,
        status: String
    ): Pair<InboundNoteEntity, Int> {
        val supplier = SUPPLIERS[supplierIndex]
        val dest = DESTINATIONS[destinationIndex]
        
        val note = InboundNoteEntity(
            senderCuit = supplier.first,
            senderNombre = supplier.second,
            senderApellido = supplier.third,
            destNombre = dest.first,
            destApellido = dest.second,
            destDireccion = dest.third,
            destTelefono = dest.fourth,
            cantBultosTotal = packageCount,
            remitoNumCliente = generateRemitoNumber(),
            remitoNumInterno = "",
            status = status,
            scanImagePath = null,
            ocrTextBlob = null,
            ocrConfidenceJson = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )
        
        return Pair(note, scannedCount)
    }

    private fun generateRemitoNumber(): String {
        val sucursal = (1..10).random().toString().padStart(4, '0')
        val numero = (1..99999).random().toString().padStart(8, '0')
        return "$sucursal-$numero"
    }

    private fun generateGs1Barcode(gtin: String, batch: String, expiry: String): String {
        // GS1-128 format: ]C1 + AI(01) GTIN + AI(10) Batch + AI(17) Expiry
        val paddedGtin = gtin.padStart(14, '0')
        val expiryCompressed = expiry.replace("/", "").takeLast(6) // YYMMDD
        return "]C101$paddedGtin\u001D10$batch\u001D17$expiryCompressed"
    }

    private fun extractGtin(barcode: String): String? {
        val gtinStart = barcode.indexOf("01") + 2
        return if (gtinStart > 1 && barcode.length >= gtinStart + 14) {
            barcode.substring(gtinStart, gtinStart + 14)
        } else null
    }

    private fun extractBatch(barcode: String): String? {
        val batchStart = barcode.indexOf("10") + 2
        val fnc1Index = barcode.indexOf('\u001D', batchStart)
        return if (batchStart > 1 && fnc1Index > batchStart) {
            barcode.substring(batchStart, fnc1Index)
        } else if (batchStart > 1 && barcode.length > batchStart) {
            barcode.substring(batchStart)
        } else null
    }

    private fun extractExpiry(barcode: String): String? {
        val expiryStart = barcode.indexOf("17") + 2
        return if (expiryStart > 1 && barcode.length >= expiryStart + 6) {
            val yy = barcode.substring(expiryStart, expiryStart + 2)
            val mm = barcode.substring(expiryStart + 2, expiryStart + 4)
            val dd = barcode.substring(expiryStart + 4, expiryStart + 6)
            val fullYear = if (yy.toInt() < 50) "20$yy" else "19$yy"
            "$dd/$mm/$fullYear"
        } else null
    }

    private fun generateFutureDate(): String {
        val months = (6..18).random()
        val days = (1..28).random()
        val year = 2025 + (months / 12)
        val month = (months % 12) + 1
        return "${days.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }
}

// Extension to make random work on ranges
private fun IntRange.random(): Int = (start..endInclusive).random()
