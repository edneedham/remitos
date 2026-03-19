package com.remitos.app.data

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
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

/**
 * Facade repository for backward compatibility.
 * Delegates to domain-specific repositories.
 */
class RemitosRepository(private val db: AppDatabase) {

    private val inboundRepository = InboundRepository(db)
    private val outboundRepository = OutboundRepository(db)
    private val auditRepository = AuditRepository(db)

    // Inbound
    suspend fun createInboundNote(note: InboundNoteEntity): Long = inboundRepository.createInboundNote(note)
    suspend fun createTestInboundNote(): Long = inboundRepository.createTestInboundNote()
    suspend fun updateInboundNote(note: InboundNoteEntity) = inboundRepository.updateInboundNote(note)
    suspend fun voidInboundNote(noteId: Long) = inboundRepository.voidInboundNote(noteId)
    fun observeInboundNotes(): Flow<List<InboundNoteEntity>> = inboundRepository.observeInboundNotes()
    fun observeInboundNotesFiltered(query: String, from: Long?, to: Long?, limit: Int): Flow<List<InboundNoteEntity>> =
        inboundRepository.observeInboundNotesFiltered(query, from, to, limit)
    suspend fun getInboundNote(noteId: Long): InboundNoteEntity? = inboundRepository.getInboundNote(noteId)
    fun observeInboundNotesWithAvailable(): Flow<List<InboundNoteWithAvailable>> = inboundRepository.observeInboundNotesWithAvailable()
    suspend fun getPackagesForNote(noteId: Long): List<InboundPackageEntity> = inboundRepository.getPackagesForNote(noteId)
    suspend fun updatePackage(packageEntity: InboundPackageEntity) = inboundRepository.updatePackage(packageEntity)

    // Outbound
    fun observeOutboundLists(): Flow<List<OutboundListEntity>> = outboundRepository.observeOutboundLists()
    suspend fun nextOutboundListNumber(): Long = outboundRepository.nextOutboundListNumber()
    suspend fun createOutboundList(list: OutboundListEntity): Long = outboundRepository.createOutboundList(list)
    suspend fun createOutboundWithAllocation(list: OutboundListEntity, line: OutboundLineEntity): Long =
        outboundRepository.createOutboundWithAllocation(list, line)
    suspend fun createOutboundWithAllocations(list: OutboundListEntity, lines: List<OutboundLineEntity>): Long =
        outboundRepository.createOutboundWithAllocations(list, lines)
    suspend fun getOutboundList(listId: Long): OutboundListEntity? = outboundRepository.getOutboundList(listId)
    suspend fun getOutboundLines(listId: Long): List<OutboundLineEntity> = outboundRepository.getOutboundLines(listId)
    suspend fun getOutboundLinesWithRemito(listId: Long): List<OutboundLineWithRemito> = outboundRepository.getOutboundLinesWithRemito(listId)
    suspend fun getOutboundLineEditHistory(lineId: Long): List<OutboundLineEditHistoryEntity> = outboundRepository.getOutboundLineEditHistory(lineId)
    suspend fun searchOutboundLists(filters: OutboundSearchFilters, limit: Int? = null): List<OutboundListEntity> =
        outboundRepository.searchOutboundLists(filters, limit)
    suspend fun markOutboundInTransit(listId: Long) = outboundRepository.markOutboundInTransit(listId)
    suspend fun updateOutboundLineOutcome(lineId: Long, status: String, deliveredQty: Int, returnedQty: Int) =
        outboundRepository.updateOutboundLineOutcome(lineId, status, deliveredQty, returnedQty)
    suspend fun updateOutboundLineDetails(lineId: Long, deliveryNumber: String, recipientNombre: String, recipientApellido: String, recipientDireccion: String, recipientTelefono: String, missingQty: Int, reason: String) =
        outboundRepository.updateOutboundLineDetails(lineId, deliveryNumber, recipientNombre, recipientApellido, recipientDireccion, recipientTelefono, missingQty, reason)
    suspend fun getOutboundLineStatusHistory(lineId: Long): List<OutboundLineStatusHistoryEntity> = outboundRepository.getOutboundLineStatusHistory(lineId)
    suspend fun closeOutboundList(listId: Long) = outboundRepository.closeOutboundList(listId)
    internal fun buildOutboundSearchQuery(filters: OutboundSearchFilters, limit: Int? = null): SupportSQLiteQuery =
        outboundRepository.buildOutboundSearchQuery(filters, limit)

    // Audit / Debug
    fun observeDebugLogs(limit: Int = 200): Flow<List<DebugLogEntity>> = auditRepository.observeDebugLogs(limit)
    suspend fun insertDebugLog(log: DebugLogEntity) = auditRepository.insertDebugLog(log)
    
    // Demo data helpers
    suspend fun insertOutboundLineStatusHistory(entries: List<OutboundLineStatusHistoryEntity>) =
        outboundRepository.insertLineStatusHistory(entries)
    suspend fun insertOutboundLineEditHistory(entries: List<OutboundLineEditHistoryEntity>) =
        outboundRepository.insertLineEditHistory(entries)
}