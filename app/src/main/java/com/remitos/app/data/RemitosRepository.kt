package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.SequenceEntity
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

class RemitosRepository(private val db: AppDatabase) {
    companion object {
        private const val ListSequenceName = "outbound_list"
        private const val InboundRemitoSequenceName = "inbound_remito_interno"
        private const val MaxDebugLogs = 200
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

    suspend fun getOutboundLineEditHistory(lineId: Long): List<OutboundLineEditHistoryEntity> {
        return db.outboundDao().getLineEditHistory(lineId)
    }

    suspend fun searchOutboundLists(filters: OutboundSearchFilters): List<OutboundListEntity> {
        val query = buildOutboundSearchQuery(filters)
        return db.outboundDao().searchOutboundLists(query)
    }

    suspend fun markOutboundInTransit(listId: Long) {
        db.withTransaction {
            val lines = db.outboundDao().getLinesForList(listId)
            val targets = lines.filter {
                it.status != OutboundLineStatus.Entregado && it.status != OutboundLineStatus.EnTransito
            }
            if (targets.isEmpty()) return@withTransaction

            val now = System.currentTimeMillis()
            val historyEntries = targets.map { line ->
                OutboundLineStatusHistoryEntity(
                    outboundLineId = line.id,
                    status = OutboundLineStatus.EnTransito,
                    createdAt = now
                )
            }
            db.outboundDao().insertLineStatusHistory(historyEntries)
            db.outboundDao().updateLineStatus(targets.map { it.id }, OutboundLineStatus.EnTransito)
        }
    }

    suspend fun updateOutboundLineOutcome(
        lineId: Long,
        status: String,
        deliveredQty: Int,
        returnedQty: Int,
    ) {
        db.withTransaction {
            val history = OutboundLineStatusHistoryEntity(
                outboundLineId = lineId,
                status = status,
                createdAt = System.currentTimeMillis()
            )
            db.outboundDao().insertLineStatusHistory(listOf(history))
            db.outboundDao().updateLineOutcome(lineId, status, deliveredQty, returnedQty)
        }
    }

    suspend fun updateOutboundLineDetails(
        lineId: Long,
        deliveryNumber: String,
        recipientNombre: String,
        recipientApellido: String,
        recipientDireccion: String,
        recipientTelefono: String,
        missingQty: Int,
        reason: String,
    ) {
        db.withTransaction {
            val current = db.outboundDao().getOutboundLine(lineId) ?: return@withTransaction
            val entries = mutableListOf<OutboundLineEditHistoryEntity>()
            val now = System.currentTimeMillis()

            fun record(field: String, oldValue: String, newValue: String) {
                if (oldValue == newValue) return
                entries.add(
                    OutboundLineEditHistoryEntity(
                        outboundLineId = lineId,
                        fieldName = field,
                        oldValue = oldValue,
                        newValue = newValue,
                        reason = reason,
                        createdAt = now
                    )
                )
            }

            record("delivery_number", current.deliveryNumber, deliveryNumber)
            record("recipient_nombre", current.recipientNombre, recipientNombre)
            record("recipient_apellido", current.recipientApellido, recipientApellido)
            record("recipient_direccion", current.recipientDireccion, recipientDireccion)
            record("recipient_telefono", current.recipientTelefono, recipientTelefono)
            record("missing_qty", current.missingQty.toString(), missingQty.toString())

            if (entries.isEmpty()) return@withTransaction
            db.outboundDao().insertLineEditHistory(entries)
            db.outboundDao().updateLineDetails(
                lineId = lineId,
                deliveryNumber = deliveryNumber,
                recipientNombre = recipientNombre,
                recipientApellido = recipientApellido,
                recipientDireccion = recipientDireccion,
                recipientTelefono = recipientTelefono,
                missingQty = missingQty,
            )
        }
    }

    suspend fun getOutboundLineStatusHistory(lineId: Long): List<OutboundLineStatusHistoryEntity> {
        return db.outboundDao().getLineStatusHistory(lineId)
    }

    suspend fun closeOutboundList(listId: Long) {
        db.outboundDao().updateOutboundListStatus(listId, OutboundListStatus.Cerrada)
    }

    internal fun buildOutboundSearchQuery(filters: OutboundSearchFilters): SupportSQLiteQuery {
        val normalizedTokens = normalizeSearchTokens(filters.query)
        val args = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        if (filters.listStatuses.isNotEmpty()) {
            conditions += "ol.status IN (${placeholders(filters.listStatuses.size)})"
            args.addAll(filters.listStatuses)
        }

        if (filters.lineStatuses.isNotEmpty()) {
            conditions +=
                """
                EXISTS (
                    SELECT 1 FROM outbound_lines l2
                    WHERE l2.outbound_list_id = ol.id
                    AND l2.status IN (${placeholders(filters.lineStatuses.size)})
                )
                """.trimIndent()
            args.addAll(filters.lineStatuses)
        }

        normalizedTokens.forEach { token ->
            val like = "%$token%"
            conditions +=
                """
                (
                    CAST(ol.list_number AS TEXT) LIKE ?
                    OR LOWER(ol.driver_nombre) LIKE ?
                    OR LOWER(ol.driver_apellido) LIKE ?
                    OR LOWER(l.delivery_number) LIKE ?
                    OR LOWER(l.recipient_nombre) LIKE ?
                    OR LOWER(l.recipient_apellido) LIKE ?
                    OR LOWER(l.recipient_direccion) LIKE ?
                    OR LOWER(l.recipient_telefono) LIKE ?
                    OR LOWER(n.remito_num_cliente) LIKE ?
                    OR LOWER(n.remito_num_interno) LIKE ?
                )
                """.trimIndent()
            repeat(10) { index ->
                args.add(if (index == 0) "%$token%" else like)
            }
        }

        val whereClause = if (conditions.isEmpty()) "1=1" else conditions.joinToString(" AND ")
        val sql =
            """
            SELECT DISTINCT ol.*
            FROM outbound_lists ol
            LEFT JOIN outbound_lines l ON l.outbound_list_id = ol.id
            LEFT JOIN inbound_notes n ON n.id = l.inbound_note_id
            WHERE $whereClause
            ORDER BY ol.issue_date DESC
            """.trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun normalizeSearchTokens(query: String): List<String> {
        return query
            .trim()
            .lowercase()
            .split("\n", " ", "\t")
            .map { it.trim() }
            .filter { it.length >= 2 }
    }

    private fun placeholders(count: Int): String {
        return List(count) { "?" }.joinToString(",")
    }
}
