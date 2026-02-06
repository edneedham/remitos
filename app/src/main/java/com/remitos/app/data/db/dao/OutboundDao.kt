package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.OutboundLineEntity
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

    @Query("SELECT * FROM outbound_lines WHERE outbound_list_id = :listId")
    suspend fun getLinesForList(listId: Long): List<OutboundLineEntity>
}
