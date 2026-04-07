package com.remitos.app.data

import android.content.Context
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.LocalUserEntity
import com.remitos.app.data.db.entity.LocalDeviceEntity
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
            android.util.Log.d("TestDataGenerator", "Starting generation")
            // Clear existing operational data first so we can generate fresh demo data
            repository.clearOperationalData()
            android.util.Log.d("TestDataGenerator", "Data cleared")

            val now = System.currentTimeMillis()
            val oneHour = 60 * 60 * 1000L
            val oneDay = 24 * oneHour

            val createdNotes = mutableListOf<Pair<Long, InboundNoteEntity>>()

            val inboundConfigs = listOf(
                InboundConfig(0, 0, 4, now - 2 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(1, 1, 6, now - 5 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(2, 2, 3, now - 7 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(3, 3, 8, now - 3 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(4, 4, 5, now - 10 * oneDay, InboundNoteStatus.Anulada),
                InboundConfig(0, 5, 7, now - 1 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(1, 6, 4, now - 4 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(2, 7, 6, now - 6 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(3, 8, 3, now - 8 * oneDay, InboundNoteStatus.Activa),
                InboundConfig(4, 9, 5, now - 12 * oneDay, InboundNoteStatus.Activa)
            )

            inboundConfigs.forEach { config ->
                val noteId = createInboundNote(config)
                val note = repository.getInboundNote(noteId)!!
                createdNotes.add(Pair(noteId, note))
            }
            android.util.Log.d("TestDataGenerator", "Inbound created: ${createdNotes.size}")

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
            // Create demo users for User Management screen
            createDemoUsers()
            
            // Create demo device registration
            createDemoDevice()
            
            android.util.Log.d("TestDataGenerator", "Generation complete")
        }
    }
    
    private suspend fun createDemoUsers() {
        val db = repository.db
        val userDao = db.localUserDao()
        
        // Check if users already exist
        val existingUsers = userDao.observeAll().first()
        if (existingUsers.isNotEmpty()) {
            android.util.Log.d("TestDataGenerator", "Users already exist, skipping")
            return
        }
        
        // Create admin user
        userDao.insert(
            LocalUserEntity(
                id = "admin",
                username = "admin",
                role = "company_owner",
                passwordHash = PasswordHasher.hash("demo1234"),
                status = "active",
                warehouseId = "22222222-2222-2222-2222-222222222222",
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        
        // Create warehouse admin
        userDao.insert(
            LocalUserEntity(
                id = "jefedeposito",
                username = "jefedeposito",
                role = "warehouse_admin",
                passwordHash = PasswordHasher.hash("demo1234"),
                status = "active",
                warehouseId = "22222222-2222-2222-2222-222222222222",
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        
        // Create operators
        val operators = listOf(
            "m.gomez" to "Miguel Gomez",
            "j.perez" to "Juan Perez", 
            "l.rodriguez" to "Lucia Rodriguez"
        )
        
        operators.forEachIndexed { index, (username, fullName) ->
            userDao.insert(
                LocalUserEntity(
                    id = "operator_$index",
                    username = username,
                    role = "operator",
                    passwordHash = PasswordHasher.hash("demo1234"),
                    status = if (index == 2) "inactive" else "active", // One inactive for variety
                    warehouseId = "22222222-2222-2222-2222-222222222222",
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
        
        android.util.Log.d("TestDataGenerator", "Created ${operators.size + 2} demo users")
    }
    
    private suspend fun createDemoDevice() {
        val db = repository.db
        val deviceDao = db.localDeviceDao()
        
        // Check if device already exists
        val existingDevice = deviceDao.getDevice()
        if (existingDevice != null) {
            android.util.Log.d("TestDataGenerator", "Device already exists, skipping")
            return
        }
        
        deviceDao.insert(
            LocalDeviceEntity(
                deviceId = "demo-device-001",
                companyId = "LOGSUR",
                warehouseId = "22222222-2222-2222-2222-222222222222",
                registeredAt = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L // 30 days ago
            )
        )
        
        android.util.Log.d("TestDataGenerator", "Created demo device")
    }
    
    suspend fun seedTemplateConfig(context: Context) {
        val settingsStore = SettingsStore(context)
        
        settingsStore.setTemplateConfig(
            TemplateConfig(
                logoUri = null, // Will use default
                showPeso = true,
                showVolumen = true,
                showObservaciones = true,
                legalText = "Logística del Sur S.A. - CUIT 30-12345678-9\nAv. General Paz 1234, CABA\nTel: 011-4567-8900\n\nDocumento no válido como factura. Conservar en buen estado."
            )
        )
        
        android.util.Log.d("TestDataGenerator", "Seeded template config")
    }

    private data class InboundConfig(
        val supplierIndex: Int,
        val destinationIndex: Int,
        val packageCount: Int,
        val createdAt: Long,
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

    private fun generateFutureDate(): String {
        val months = (6..18).random()
        val days = (1..28).random()
        val year = 2026 + (months / 12)
        val month = (months % 12) + 1
        return "${days.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }
}
