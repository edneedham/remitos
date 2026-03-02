package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.remitos.app.data.db.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY id ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingLimited(limit: Int): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun getByEntity(entityType: String, entityId: String): SyncQueueEntity?

    @Query("SELECT * FROM sync_queue WHERE status = 'failed' AND retry_count < :maxRetries ORDER BY last_attempt_at ASC")
    suspend fun getRetryable(maxRetries: Int): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1, last_attempt_at = :timestamp, status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)

    @Query("DELETE FROM sync_queue WHERE status = 'completed'")
    suspend fun deleteCompleted()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'failed'")
    fun observeFailedCount(): Flow<Int>
}
