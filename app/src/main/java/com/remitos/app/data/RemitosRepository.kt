package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
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
    }

    suspend fun createInboundNote(note: InboundNoteEntity): Long {
        return db.withTransaction {
            val id = db.inboundDao().insertInbound(note)
            val packages = (1..note.cantBultosTotal).map { index ->
                InboundPackageEntity(
                    inboundNoteId = id,
                    packageIndex = index,
                    status = "disponible"
                )
            }
            db.inboundDao().insertPackages(packages)
            id
        }
    }

    fun observeInboundNotes(): Flow<List<InboundNoteEntity>> {
        return db.inboundDao().observeInboundNotes()
    }

    fun observeInboundNotesWithAvailable(): Flow<List<InboundNoteWithAvailable>> {
        return db.inboundDao().observeInboundNotesWithAvailable()
    }

    fun observeOutboundLists(): Flow<List<OutboundListEntity>> {
        return db.outboundDao().observeOutboundLists()
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

            db.inboundDao().updatePackageStatus(packageIds, "asignado")
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

                db.inboundDao().updatePackageStatus(packageIds, "asignado")
                val lineWithList = line.copy(
                    outboundListId = listId,
                    allocatedPackageIds = packageIds.joinToString(",")
                )
                db.outboundDao().insertOutboundLines(listOf(lineWithList))
            }
            listId
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
}
