package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.remitos.app.data.db.entity.DebugLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugLogDao {
    @Insert
    suspend fun insertLog(log: DebugLogEntity): Long

    @Query("SELECT * FROM debug_logs ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DebugLogEntity>>

    @Query("SELECT COUNT(*) FROM debug_logs")
    suspend fun countLogs(): Long

    @Query(
        "DELETE FROM debug_logs WHERE id IN (SELECT id FROM debug_logs ORDER BY created_at ASC LIMIT :excess)"
    )
    suspend fun deleteOldest(excess: Int)
}
