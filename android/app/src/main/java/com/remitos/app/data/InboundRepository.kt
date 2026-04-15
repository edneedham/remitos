package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import com.remitos.app.data.db.entity.SequenceEntity
import kotlinx.coroutines.flow.Flow

class InboundRepository(private val db: AppDatabase) {
    companion object {
        private const val InboundRemitoSequenceName = "inbound_remito_interno"
    }

    suspend fun createInboundNote(note: InboundNoteEntity): Long {
        return db.withTransaction {
            val remitoInterno = nextInboundRemitoInternoLocked()
            val id = db.inboundDao().insertInbound(note.copy(remitoNumInterno = remitoInterno))
            val packages = (1..note.cantBultosTotal).map { index ->
                InboundPackageEntity(
                    inboundNoteId = id,
                    packageIndex = index,
                    status = InboundPackageStatus.Disponible
                )
            }
            db.inboundDao().insertPackages(packages)
            id
        }
    }

    suspend fun createTestInboundNote(): Long {
        val now = System.currentTimeMillis()
        val testNote = InboundNoteEntity(
            senderCuit = "30-12345678-9",
            senderNombre = "Proveedor",
            senderApellido = "Test",
            destNombre = "Cliente",
            destApellido = "Ejemplo",
            destDireccion = "Av. Test 1234, Buenos Aires",
            destTelefono = "11-1234-5678",
            cantBultosTotal = 5,
            remitoNumCliente = "0001-00012345",
            remitoNumInterno = "",
            status = InboundNoteStatus.Activa,
            scanImagePath = null,
            ocrTextBlob = null,
            ocrConfidenceJson = null,
            createdAt = now,
            updatedAt = now
        )
        return createInboundNote(testNote)
    }

    suspend fun updateInboundNote(note: InboundNoteEntity) {
        db.withTransaction {
            val currentTotal = db.inboundDao().countPackages(note.id)
            val available = db.inboundDao().countPackagesByStatus(note.id, InboundPackageStatus.Disponible)
            val assigned = currentTotal - available
            if (note.cantBultosTotal < assigned) {
                throw IllegalStateException("Hay bultos asignados que no se pueden quitar.")
            }

            if (note.cantBultosTotal > currentTotal) {
                val maxIndex = db.inboundDao().getMaxPackageIndex(note.id) ?: 0
                val newPackages = (1..(note.cantBultosTotal - currentTotal)).map { offset ->
                    InboundPackageEntity(
                        inboundNoteId = note.id,
                        packageIndex = maxIndex + offset,
                        status = InboundPackageStatus.Disponible
                    )
                }
                db.inboundDao().insertPackages(newPackages)
            } else if (note.cantBultosTotal < currentTotal) {
                val toRemove = currentTotal - note.cantBultosTotal
                val packageIds = db.inboundDao()
                    .getPackageIdsForTrim(note.id, InboundPackageStatus.Disponible, toRemove)
                if (packageIds.size < toRemove) {
                    throw IllegalStateException("No hay suficientes bultos disponibles para ajustar la cantidad.")
                }
                db.inboundDao().deletePackages(packageIds)
            }

            db.inboundDao().updateInbound(note.copy(needsSync = true))
        }
    }

    suspend fun voidInboundNote(noteId: Long) {
        db.withTransaction {
            val note = db.inboundDao().getInboundNote(noteId) ?: return@withTransaction
            val voided = note.copy(status = InboundNoteStatus.Anulada, updatedAt = System.currentTimeMillis(), needsSync = true)
            db.inboundDao().updateInbound(voided)
            db.inboundDao().updatePackageStatusForNote(noteId, InboundPackageStatus.Anulado)
        }
    }

    fun observeInboundNotes(): Flow<List<InboundNoteEntity>> {
        return db.inboundDao().observeInboundNotes()
    }

    fun observeInboundNotesFiltered(
        query: String,
        from: Long?,
        to: Long?,
        limit: Int,
    ): Flow<List<InboundNoteEntity>> {
        val normalized = query.trim().lowercase()
        val like = "%$normalized%"
        return db.inboundDao().observeInboundNotesFiltered(normalized, like, from, to, limit)
    }

    suspend fun getInboundNote(noteId: Long): InboundNoteEntity? {
        return db.inboundDao().getInboundNote(noteId)
    }

    fun observeInboundNotesWithAvailable(): Flow<List<InboundNoteWithAvailable>> {
        return db.inboundDao().observeInboundNotesWithAvailable()
    }

    suspend fun getPackagesForNote(noteId: Long): List<InboundPackageEntity> {
        return db.inboundDao().getPackagesForNote(noteId)
    }

    suspend fun updatePackage(packageEntity: InboundPackageEntity) {
        db.inboundDao().updatePackage(packageEntity)
    }

    // Image upload methods
    suspend fun getNotesByUploadStatus(status: com.remitos.app.data.db.entity.UploadStatus): List<InboundNoteEntity> {
        return db.inboundDao().getNotesByUploadStatus(status.name.lowercase())
    }

    suspend fun updateUploadStatus(noteId: Long, status: com.remitos.app.data.db.entity.UploadStatus, retryCount: Int = 0) {
        db.inboundDao().updateUploadStatus(noteId, status.name.lowercase(), retryCount)
    }

    suspend fun updateImageUploadInfo(
        noteId: Long,
        imageUrl: String,
        imageGcsPath: String,
        status: com.remitos.app.data.db.entity.UploadStatus
    ) {
        db.inboundDao().updateImageUploadInfo(
            noteId,
            imageUrl,
            imageGcsPath,
            status.name.lowercase(),
            System.currentTimeMillis()
        )
    }

    suspend fun updateImageUrl(noteId: Long, imageUrl: String) {
        db.inboundDao().updateImageUrl(noteId, imageUrl)
    }

    private suspend fun nextInboundRemitoInternoLocked(): String {
        val current = db.sequenceDao().getSequence(InboundRemitoSequenceName)
        val nextValue = if (current == null) {
            db.sequenceDao().insertSequence(SequenceEntity(InboundRemitoSequenceName, 2))
            1
        } else {
            val value = current.nextValue
            db.sequenceDao().updateSequence(current.copy(nextValue = value + 1))
            value
        }
        return formatInboundRemitoInterno(nextValue)
    }

    private fun formatInboundRemitoInterno(value: Long): String {
        return "RI-${value.toString().padStart(6, '0')}"
    }
}
