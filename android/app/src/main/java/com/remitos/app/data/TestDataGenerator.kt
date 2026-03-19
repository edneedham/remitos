package com.remitos.app.data

import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TestDataGenerator(private val repository: RemitosRepository) {

    companion object {
        private val SUPPLIERS = listOf(
            Supplier("30-50279317-5", "Arcor", "S.A.I.C."),
            Supplier("30-50000173-5", "Molinos", "Río de la Plata"),
            Supplier("30-53888694-4", "Droguería", "del Sud S.A."),
            Supplier("30-70123456-1", "Distribuidora", "Los Andes SRL"),
            Supplier("30-61234567-8", "Química", "Austral S.A.")
        )

        private val DESTINATIONS = listOf(
            Destination("Autoservicio", "El Sol", "Av. Rivadavia 4560, CABA", "11-4567-8901"),
            Destination("Farmacia", "San Martín", "San Martín 120, Quilmes", "11-4225-6789"),
            Destination("Supermercado", "La Familia", "Av. Vélez Sarsfield 1500, Córdoba", "351-456-7890"),
            Destination("Facundo", "Quiroga", "Calle 14 Nro 567, La Plata", "221-456-7890"),
            Destination("Camila", "Sosa", "Güemes 3450, CABA", "11-4890-1234"),
            Destination("Kiosco", "Don José", "Av. Corrientes 2345, CABA", "11-4956-7890"),
            Destination("Maximiliano", "Ríos", "Belgrano 890, Rosario", "341-567-8901"),
            Destination("Verónica", "Luna", "Av. Maipú 1234, Vicente López", "11-4790-1234"),
            Destination("Panadería", "La Estación", "Av. San Martín 567, Mendoza", "261-456-7890"),
            Destination("Librería", "Del Centro", "Peatonal 234, Tucumán", "381-456-7890")
        )

        private val DRIVERS = listOf(
            Driver("Roberto", "Maidana", "AF 123 CD"),
            Driver("Hugo", "Benítez", "AB 456 GH"),
            Driver("Miguel", "Ávila", "AC 789 JK")
        )
    }

    private data class Supplier(val cuit: String, val nombre: String, val apellido: String)
    private data class Destination(val nombre: String, val apellido: String, val direccion: String, val telefono: String)
    private data class Driver(val nombre: String, val apellido: String, val patente: String)

    suspend fun generateTestData() {
        withContext(Dispatchers.IO) {
            val existingNotes = repository.observeInboundNotes().first()
            if (existingNotes.isNotEmpty()) {
                return@withContext
            }

            val now = System.currentTimeMillis()
            val oneHour = 60 * 60 * 1000L
            val oneDay = 24 * oneHour

            val createdNotes = mutableListOf<Pair<Long, InboundNoteEntity>>()

            val inboundConfigs = listOf(
                InboundConfig(0, 0, 4, now - 2 * oneDay, 4, InboundNoteStatus.Activa),
                InboundConfig(1, 1, 6, now - 5 * oneDay, 6, InboundNoteStatus.Activa),
                InboundConfig(2, 2, 3, now - 7 * oneDay, 0, InboundNoteStatus.Activa),
                InboundConfig(3, 3, 8, now - 3 * oneDay, 5, InboundNoteStatus.Activa),
                InboundConfig(4, 4, 5, now - 10 * oneDay, 0, InboundNoteStatus.Anulada),
                InboundConfig(0, 5, 7, now - 1 * oneDay, 3, InboundNoteStatus.Activa),
                InboundConfig(1, 6, 4, now - 4 * oneDay, 4, InboundNoteStatus.Activa),
                InboundConfig(2, 7, 6, now - 6 * oneDay, 6, InboundNoteStatus.Activa),
                InboundConfig(3, 8, 3, now - 8 * oneDay, 0, InboundNoteStatus.Activa),
                InboundConfig(4, 9, 5, now - 12 * oneDay, 5, InboundNoteStatus.Activa)
            )

            inboundConfigs.forEach { config ->
                val noteId = createInboundNote(config)
                val note = repository.getInboundNote(noteId)!!
                createdNotes.add(Pair(noteId, note))

                if (config.scannedCount > 0) {
                    scanPackages(noteId, config.scannedCount, note.createdAt)
                }
            }

            val fullyScannedNotes = createdNotes.filter { (_, note) ->
                val packages = repository.getPackagesForNote(note.id)
                packages.all { it.status == InboundPackageStatus.Asignado || it.status == InboundPackageStatus.Disponible }
            }.filter { (_, note) ->
                val packages = repository.getPackagesForNote(note.id)
                packages.count { it.status == InboundPackageStatus.Disponible } > 0
            }

            if (fullyScannedNotes.size >= 4) {
                val list1Notes = fullyScannedNotes.take(2)
                val list1Id = createOutboundList(
                    driverIndex = 0,
                    issueDate = now - 6 * oneHour,
                    status = OutboundListStatus.Abierta
                )
                val list1Lines = mutableListOf<OutboundLineEntity>()
                list1Notes.forEach { (noteId, note) ->
                    val packages = repository.getPackagesForNote(noteId)
                    val available = packages.filter { it.status == InboundPackageStatus.Disponible }
                    if (available.isNotEmpty()) {
                        list1Lines.add(createOutboundLine(list1Id, noteId, note, available.size))
                    }
                }
                if (list1Lines.isNotEmpty()) {
                    repository.createOutboundWithAllocations(
                        OutboundListEntity(
                            id = list1Id,
                            listNumber = repository.nextOutboundListNumber() - 1,
                            issueDate = now - 6 * oneHour,
                            driverNombre = DRIVERS[0].nombre,
                            driverApellido = DRIVERS[0].apellido,
                            checklistSignaturePath = null,
                            checklistSignedAt = null,
                            status = OutboundListStatus.Abierta
                        ),
                        list1Lines
                    )
                    val savedLines = repository.getOutboundLines(list1Id)
                    if (savedLines.size >= 4) {
                        val statusHistory = mutableListOf<OutboundLineStatusHistoryEntity>()
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = savedLines[0].id,
                            status = OutboundLineStatus.EnTransito,
                            createdAt = now - 5 * oneHour
                        ))
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = savedLines[0].id,
                            status = OutboundLineStatus.Entregado,
                            createdAt = now - 4 * oneHour
                        ))
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = savedLines[1].id,
                            status = OutboundLineStatus.EnTransito,
                            createdAt = now - 5 * oneHour
                        ))
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = savedLines[2].id,
                            status = OutboundLineStatus.EnTransito,
                            createdAt = now - 5 * oneHour
                        ))
                        repository.insertOutboundLineStatusHistory(statusHistory)
                        repository.updateOutboundLineOutcome(savedLines[0].id, OutboundLineStatus.Entregado, savedLines[0].packageQty, 0)
                        repository.updateOutboundLineOutcome(savedLines[1].id, OutboundLineStatus.EnTransito, 0, 0)
                        repository.updateOutboundLineOutcome(savedLines[2].id, OutboundLineStatus.EnTransito, 0, 0)
                    }
                }

                val list2Notes = fullyScannedNotes.drop(2).take(2)
                val list2Id = createOutboundList(
                    driverIndex = 1,
                    issueDate = now - 1 * oneHour,
                    status = OutboundListStatus.Abierta
                )
                val list2Lines = mutableListOf<OutboundLineEntity>()
                list2Notes.forEach { (noteId, note) ->
                    val packages = repository.getPackagesForNote(noteId)
                    val available = packages.filter { it.status == InboundPackageStatus.Disponible }
                    if (available.isNotEmpty()) {
                        list2Lines.add(createOutboundLine(list2Id, noteId, note, available.size))
                    }
                }
                if (list2Lines.isNotEmpty()) {
                    repository.createOutboundWithAllocations(
                        OutboundListEntity(
                            id = list2Id,
                            listNumber = repository.nextOutboundListNumber() - 1,
                            issueDate = now - 1 * oneHour,
                            driverNombre = DRIVERS[1].nombre,
                            driverApellido = DRIVERS[1].apellido,
                            checklistSignaturePath = null,
                            checklistSignedAt = null,
                            status = OutboundListStatus.Abierta
                        ),
                        list2Lines
                    )
                }
            }

            if (fullyScannedNotes.size >= 6) {
                val list3Notes = fullyScannedNotes.drop(4).take(2)
                val list3Id = createOutboundList(
                    driverIndex = 2,
                    issueDate = now - 2 * oneDay,
                    status = OutboundListStatus.Cerrada
                )
                val list3Lines = mutableListOf<OutboundLineEntity>()
                list3Notes.forEach { (noteId, note) ->
                    val packages = repository.getPackagesForNote(noteId)
                    val available = packages.filter { it.status == InboundPackageStatus.Disponible }
                    if (available.isNotEmpty()) {
                        list3Lines.add(createOutboundLine(list3Id, noteId, note, available.size))
                    }
                }
                if (list3Lines.isNotEmpty()) {
                    repository.createOutboundWithAllocations(
                        OutboundListEntity(
                            id = list3Id,
                            listNumber = repository.nextOutboundListNumber() - 1,
                            issueDate = now - 2 * oneDay,
                            driverNombre = DRIVERS[2].nombre,
                            driverApellido = DRIVERS[2].apellido,
                            checklistSignaturePath = "/data/user/0/com.remitos.app/files/signatures/demo_signature.png",
                            checklistSignedAt = now - 2 * oneDay + 8 * oneHour,
                            status = OutboundListStatus.Cerrada
                        ),
                        list3Lines
                    )
                    val savedLines = repository.getOutboundLines(list3Id)
                    val statusHistory = mutableListOf<OutboundLineStatusHistoryEntity>()
                    savedLines.forEachIndexed { index, line ->
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = line.id,
                            status = OutboundLineStatus.EnTransito,
                            createdAt = now - 2 * oneDay + (1 + index) * oneHour
                        ))
                        statusHistory.add(OutboundLineStatusHistoryEntity(
                            outboundLineId = line.id,
                            status = OutboundLineStatus.Entregado,
                            createdAt = now - 2 * oneDay + (4 + index) * oneHour
                        ))
                        repository.updateOutboundLineOutcome(line.id, OutboundLineStatus.Entregado, line.packageQty, 0)
                    }
                    repository.insertOutboundLineStatusHistory(statusHistory)
                }
            }

            val allLists = repository.searchOutboundLists(OutboundSearchFilters())
            if (allLists.isNotEmpty()) {
                val firstList = allLists.first()
                val lines = repository.getOutboundLines(firstList.id)
                if (lines.isNotEmpty()) {
                    val editHistory = listOf(
                        OutboundLineEditHistoryEntity(
                            outboundLineId = lines[0].id,
                            fieldName = "recipient_direccion",
                            oldValue = "Av. Rivadavla 4560, CABA",
                            newValue = "Av. Rivadavia 4560, CABA",
                            reason = "Corrección de error de lectura OCR",
                            createdAt = firstList.issueDate + 30 * 60 * 1000L
                        )
                    )
                    repository.insertOutboundLineEditHistory(editHistory)
                }
            }
        }
    }

    private data class InboundConfig(
        val supplierIndex: Int,
        val destinationIndex: Int,
        val packageCount: Int,
        val createdAt: Long,
        val scannedCount: Int,
        val status: String
    )

    private suspend fun createInboundNote(config: InboundConfig): Long {
        val supplier = SUPPLIERS[config.supplierIndex]
        val dest = DESTINATIONS[config.destinationIndex]

        val note = InboundNoteEntity(
            senderCuit = supplier.cuit,
            senderNombre = supplier.nombre,
            senderApellido = supplier.apellido,
            destNombre = dest.nombre,
            destApellido = dest.apellido,
            destDireccion = dest.direccion,
            destTelefono = dest.telefono,
            cantBultosTotal = config.packageCount,
            remitoNumCliente = generateRemitoNumber(),
            remitoNumInterno = "",
            status = config.status,
            scanImagePath = null,
            ocrTextBlob = null,
            ocrConfidenceJson = null,
            createdAt = config.createdAt,
            updatedAt = config.createdAt
        )

        return repository.createInboundNote(note)
    }

    private suspend fun scanPackages(noteId: Long, count: Int, createdAt: Long) {
        val packages = repository.getPackagesForNote(noteId)
        packages.take(count).forEachIndexed { index, pkg ->
            val gtin = "779${(100000000..999999999).random()}"
            val batch = "L${(1000..9999).random()}"
            val expiry = generateFutureDate()
            val gs1Barcode = generateGs1Barcode(gtin, batch, expiry)

            repository.updatePackage(
                pkg.copy(
                    barcodeRaw = gs1Barcode,
                    gtin = extractGtin(gs1Barcode),
                    batchLot = extractBatch(gs1Barcode),
                    expiryDate = extractExpiry(gs1Barcode),
                    scannedAt = createdAt + (index + 1) * 60 * 1000L,
                    scannedBy = "demo_user"
                )
            )
        }
    }

    private suspend fun createOutboundList(driverIndex: Int, issueDate: Long, status: String): Long {
        val driver = DRIVERS[driverIndex]
        val listNumber = repository.nextOutboundListNumber()

        val list = OutboundListEntity(
            listNumber = listNumber,
            issueDate = issueDate,
            driverNombre = driver.nombre,
            driverApellido = driver.apellido,
            checklistSignaturePath = null,
            checklistSignedAt = null,
            status = status
        )

        return repository.createOutboundList(list)
    }

    private fun createOutboundLine(listId: Long, noteId: Long, note: InboundNoteEntity, packageQty: Int): OutboundLineEntity {
        return OutboundLineEntity(
            outboundListId = listId,
            inboundNoteId = noteId,
            deliveryNumber = "ENT-${(1000..9999).random()}",
            recipientNombre = note.destNombre,
            recipientApellido = note.destApellido,
            recipientDireccion = note.destDireccion,
            recipientTelefono = note.destTelefono,
            packageQty = packageQty,
            allocatedPackageIds = "",
            status = OutboundLineStatus.EnDeposito,
            deliveredQty = 0,
            returnedQty = 0,
            missingQty = 0
        )
    }

    private fun generateRemitoNumber(): String {
        val sucursal = (1..10).random().toString().padStart(4, '0')
        val numero = (1..99999).random().toString().padStart(8, '0')
        return "$sucursal-$numero"
    }

    private fun generateGs1Barcode(gtin: String, batch: String, expiry: String): String {
        val paddedGtin = gtin.padStart(14, '0')
        val expiryCompressed = expiry.replace("/", "").takeLast(6)
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
        val year = 2026 + (months / 12)
        val month = (months % 12) + 1
        return "${days.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }
}

private fun IntRange.random(): Int = (start..endInclusive).random()
