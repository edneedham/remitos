package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.LocalSessionEntity

@Dao
interface LocalSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LocalSessionEntity)

    @Query("SELECT * FROM local_session WHERE id = 'current_session'")
    suspend fun getSession(): LocalSessionEntity?

    @Query("DELETE FROM local_session")
    suspend fun clearSession()

    @Query("UPDATE local_session SET last_activity_time = :time WHERE id = 'current_session'")
    suspend fun updateLastActivity(time: Long)
}
