package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.SequenceEntity
import kotlinx.coroutines.flow.Flow

class RemitosRepository(private val db: AppDatabase) {
    companion object {
        private const val ListSequenceName = "outbound_list"
        private const val MaxDebugLogs = 200
    }

    suspend fun createInboundNote(note: InboundNoteEntity): Long {
        return db.withTransaction {
            val id = db.inboundDao().insertInbound(note)
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

            db.inboundDao().updateInbound(note)
        }
    }

    suspend fun voidInboundNote(noteId: Long) {
        db.withTransaction {
            val note = db.inboundDao().getInboundNote(noteId) ?: return@withTransaction
            val voided = note.copy(status = InboundNoteStatus.Anulada, updatedAt = System.currentTimeMillis())
            db.inboundDao().updateInbound(voided)
            db.inboundDao().updatePackageStatusForNote(noteId, InboundPackageStatus.Anulado)
        }
    }

    fun observeInboundNotes(): Flow<List<InboundNoteEntity>> {
        return db.inboundDao().observeInboundNotes()
    }

    suspend fun getInboundNote(noteId: Long): InboundNoteEntity? {
        return db.inboundDao().getInboundNote(noteId)
    }

    fun observeInboundNotesWithAvailable(): Flow<List<InboundNoteWithAvailable>> {
        return db.inboundDao().observeInboundNotesWithAvailable()
    }

    fun observeOutboundLists(): Flow<List<OutboundListEntity>> {
        return db.outboundDao().observeOutboundLists()
    }

    fun observeDebugLogs(limit: Int = MaxDebugLogs): Flow<List<DebugLogEntity>> {
        return db.debugLogDao().observeRecent(limit)
    }

    suspend fun nextOutboundListNumber(): Long {
        return db.withTransaction {
            val current = db.sequenceDao().getSequence(ListSequenceName)
            if (current == null) {
                db.sequenceDao().insertSequence(SequenceEntity(ListSequenceName, 2))
                1
            } else {
                val next = current.nextValue
                db.sequenceDao().updateSequence(current.copy(nextValue = next + 1))
                next
            }
        }
    }

    suspend fun createOutboundList(list: OutboundListEntity): Long {
        return db.outboundDao().insertOutboundList(list)
    }

    suspend fun createOutboundWithAllocation(
        list: OutboundListEntity,
        line: OutboundLineEntity
    ): Long {
        return db.withTransaction {
            val listId = db.outboundDao().insertOutboundList(list)
            val packageIds = db.inboundDao().getAvailablePackageIds(
                noteId = line.inboundNoteId,
                limit = line.packageQty
            )

            if (packageIds.size < line.packageQty) {
                throw IllegalStateException("Paquetes insuficientes")
            }

            db.inboundDao().updatePackageStatus(packageIds, InboundPackageStatus.Asignado)
            val lineWithList = line.copy(
                outboundListId = listId,
                allocatedPackageIds = packageIds.joinToString(",")
            )
            db.outboundDao().insertOutboundLines(listOf(lineWithList))
            listId
        }
    }

    suspend fun createOutboundWithAllocations(
        list: OutboundListEntity,
        lines: List<OutboundLineEntity>
    ): Long {
        return db.withTransaction {
            val listId = db.outboundDao().insertOutboundList(list)
            lines.forEach { line ->
                val packageIds = db.inboundDao().getAvailablePackageIds(
                    noteId = line.inboundNoteId,
                    limit = line.packageQty
                )

                if (packageIds.size < line.packageQty) {
                    throw IllegalStateException("Paquetes insuficientes")
                }

            db.inboundDao().updatePackageStatus(packageIds, InboundPackageStatus.Asignado)
            val lineWithList = line.copy(
                outboundListId = listId,
                allocatedPackageIds = packageIds.joinToString(",")
            )
            db.outboundDao().insertOutboundLines(listOf(lineWithList))
        }
        listId
    }
    }

    suspend fun insertDebugLog(log: DebugLogEntity) {
        db.withTransaction {
            db.debugLogDao().insertLog(log)
            val total = db.debugLogDao().countLogs()
            val excess = calculateExcessLogs(total, MaxDebugLogs)
            if (excess > 0) {
                db.debugLogDao().deleteOldest(excess)
            }
        }
    }

    suspend fun getOutboundList(listId: Long): OutboundListEntity? {
        return db.outboundDao().getOutboundList(listId)
    }

    suspend fun getOutboundLines(listId: Long): List<OutboundLineEntity> {
        return db.outboundDao().getLinesForList(listId)
    }

    suspend fun getOutboundLinesWithRemito(listId: Long): List<OutboundLineWithRemito> {
        return db.outboundDao().getLinesForListWithRemito(listId)
    }

    suspend fun signOutboundChecklist(
        listId: Long,
        lineIds: List<Long>,
        signaturePath: String,
        signedAt: Long,
    ) {
        db.withTransaction {
            db.outboundDao().updateChecklistSignature(listId, signaturePath, signedAt)
            if (lineIds.isNotEmpty()) {
                db.outboundDao().updateLineStatus(lineIds, OutboundLineStatus.EnTransito)
            }
        }
    }

    suspend fun updateOutboundLineOutcome(
        lineId: Long,
        status: String,
        deliveredQty: Int,
        returnedQty: Int,
    ) {
        db.outboundDao().updateLineOutcome(lineId, status, deliveredQty, returnedQty)
    }

    suspend fun closeOutboundList(listId: Long) {
        db.outboundDao().updateOutboundListStatus(listId, OutboundListStatus.Cerrada)
    }
}
