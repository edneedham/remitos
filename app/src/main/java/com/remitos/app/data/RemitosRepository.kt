package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.SequenceEntity

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
}
