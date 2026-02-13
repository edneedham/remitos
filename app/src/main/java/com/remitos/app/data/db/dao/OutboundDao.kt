package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboundList(list: OutboundListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboundLines(lines: List<OutboundLineEntity>)

    @Query("SELECT * FROM outbound_lists ORDER BY issue_date DESC")
    fun observeOutboundLists(): Flow<List<OutboundListEntity>>

    @Query("SELECT * FROM outbound_lists WHERE id = :listId")
    suspend fun getOutboundList(listId: Long): OutboundListEntity?

    @Query("UPDATE outbound_lists SET status = :status WHERE id = :listId")
    suspend fun updateOutboundListStatus(listId: Long, status: String)

    @Query("SELECT * FROM outbound_lines WHERE outbound_list_id = :listId")
    suspend fun getLinesForList(listId: Long): List<OutboundLineEntity>

    @Query("UPDATE outbound_lines SET status = :status WHERE outbound_list_id = :listId")
    suspend fun updateLineStatusForList(listId: Long, status: String)

    @Query("UPDATE outbound_lines SET status = :status WHERE id IN (:lineIds)")
    suspend fun updateLineStatus(lineIds: List<Long>, status: String)

    @Query(
        """
        UPDATE outbound_lines
        SET status = :status,
            delivered_qty = :deliveredQty,
            returned_qty = :returnedQty
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
        SELECT l.*, n.remito_num_cliente AS remito_num_cliente, n.remito_num_interno AS remito_num_interno
        FROM outbound_lines l
        INNER JOIN inbound_notes n ON n.id = l.inbound_note_id
        WHERE l.outbound_list_id = :listId
        """
    )
    suspend fun getLinesForListWithRemito(listId: Long): List<OutboundLineWithRemito>
}
