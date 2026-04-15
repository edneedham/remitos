package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboundList(list: OutboundListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboundLines(lines: List<OutboundLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineStatusHistory(entries: List<OutboundLineStatusHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineEditHistory(entries: List<OutboundLineEditHistoryEntity>)

    @Query(
        """
        SELECT * FROM outbound_line_status_history
        WHERE outbound_line_id = :lineId
        ORDER BY created_at ASC
        """
    )
    suspend fun getLineStatusHistory(lineId: Long): List<OutboundLineStatusHistoryEntity>

    @Query(
        """
        SELECT * FROM outbound_line_edit_history
        WHERE outbound_line_id = :lineId
        ORDER BY created_at ASC
        """
    )
    suspend fun getLineEditHistory(lineId: Long): List<OutboundLineEditHistoryEntity>

    @Query("SELECT * FROM outbound_lists ORDER BY issue_date DESC")
    fun observeOutboundLists(): Flow<List<OutboundListEntity>>

    @RawQuery(observedEntities = [OutboundListEntity::class])
    suspend fun searchOutboundLists(query: SupportSQLiteQuery): List<OutboundListEntity>

    @Query("SELECT * FROM outbound_lists WHERE id = :listId")
    suspend fun getOutboundList(listId: Long): OutboundListEntity?

    @Query("UPDATE outbound_lists SET status = :status, needs_sync = 1 WHERE id = :listId")
    suspend fun updateOutboundListStatus(listId: Long, status: String)

    @Query("SELECT * FROM outbound_lines WHERE outbound_list_id = :listId")
    suspend fun getLinesForList(listId: Long): List<OutboundLineEntity>

    @Query("SELECT * FROM outbound_lines WHERE id = :lineId")
    suspend fun getOutboundLine(lineId: Long): OutboundLineEntity?

    @Query("UPDATE outbound_lines SET status = :status WHERE outbound_list_id = :listId")
    suspend fun updateLineStatusForList(listId: Long, status: String)

    @Query("UPDATE outbound_lines SET status = :status, needs_sync = 1 WHERE id IN (:lineIds)")
    suspend fun updateLineStatus(lineIds: List<Long>, status: String)

    @Query(
        """
        UPDATE outbound_lines
        SET status = :status,
            delivered_qty = :deliveredQty,
            returned_qty = :returnedQty,
            needs_sync = 1
        WHERE id = :lineId
        """
    )
    suspend fun updateLineOutcome(
        lineId: Long,
        status: String,
        deliveredQty: Int,
        returnedQty: Int,
    )

    @Query(
        """
        UPDATE outbound_lines
        SET delivery_number = :deliveryNumber,
            recipient_nombre = :recipientNombre,
            recipient_apellido = :recipientApellido,
            recipient_direccion = :recipientDireccion,
            recipient_telefono = :recipientTelefono,
            missing_qty = :missingQty,
            needs_sync = 1
        WHERE id = :lineId
        """
    )
    suspend fun updateLineDetails(
        lineId: Long,
        deliveryNumber: String,
        recipientNombre: String,
        recipientApellido: String,
        recipientDireccion: String,
        recipientTelefono: String,
        missingQty: Int,
    )

    @Query(
        """
        SELECT l.*, n.remito_num_cliente AS remito_num_cliente, n.remito_num_interno AS remito_num_interno
        FROM outbound_lines l
        INNER JOIN inbound_notes n ON n.id = l.inbound_note_id
        WHERE l.outbound_list_id = :listId
        """
    )
    suspend fun getLinesForListWithRemito(listId: Long): List<OutboundLineWithRemito>

    @Query("SELECT * FROM outbound_lists WHERE needs_sync = 1")
    suspend fun getUnsyncedLists(): List<OutboundListEntity>

    @Query("SELECT * FROM outbound_lines WHERE needs_sync = 1")
    suspend fun getUnsyncedLines(): List<OutboundLineEntity>

    @Query("SELECT * FROM outbound_lines WHERE outbound_list_id = :listId")
    suspend fun getLinesForListSync(listId: Long): List<OutboundLineEntity>

    @Query("SELECT * FROM outbound_lists WHERE cloud_id = :cloudId")
    suspend fun getListByCloudId(cloudId: String): OutboundListEntity?

    @Query("UPDATE outbound_lists SET needs_sync = 0, last_synced_at = :timestamp, cloud_id = :cloudId WHERE id = :id")
    suspend fun markListSynced(id: Long, cloudId: String, timestamp: Long)

    @Query("UPDATE outbound_lines SET needs_sync = 0, last_synced_at = :timestamp, cloud_id = :cloudId WHERE id = :id")
    suspend fun markLineSynced(id: Long, cloudId: String, timestamp: Long)

    @Query("UPDATE outbound_lists SET needs_sync = 1 WHERE id = :id")
    suspend fun markListNeedsSync(id: Long)

    @Query("UPDATE outbound_lines SET needs_sync = 1 WHERE id = :id")
    suspend fun markLineNeedsSync(id: Long)

    @Query("SELECT * FROM outbound_line_status_history WHERE synced = 0")
    suspend fun getUnsyncedStatusHistory(): List<OutboundLineStatusHistoryEntity>

    @Query("SELECT * FROM outbound_line_edit_history WHERE synced = 0")
    suspend fun getUnsyncedEditHistory(): List<OutboundLineEditHistoryEntity>

    @Query("UPDATE outbound_line_status_history SET synced = 1 WHERE id IN (:ids)")
    suspend fun markStatusHistorySynced(ids: List<Long>)

    @Query("UPDATE outbound_line_edit_history SET synced = 1 WHERE id IN (:ids)")
    suspend fun markEditHistorySynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: OutboundListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLines(lines: List<OutboundLineEntity>)

    @Update
    suspend fun updateOutboundList(list: OutboundListEntity)

    @Update
    suspend fun updateOutboundLine(line: OutboundLineEntity)

    @Query("SELECT * FROM outbound_lines WHERE cloud_id = :cloudId")
    suspend fun getLineByCloudId(cloudId: String): OutboundLineEntity?
}
