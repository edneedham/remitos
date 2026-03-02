package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.remitos.app.data.db.entity.LocalUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: LocalUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<LocalUserEntity>)

    @Update
    suspend fun update(user: LocalUserEntity)

    @Query("SELECT * FROM local_users WHERE id = :id")
    suspend fun getById(id: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE username = :username")
    suspend fun getByUsername(username: String): LocalUserEntity?

    @Query("SELECT * FROM local_users")
    fun observeAll(): Flow<List<LocalUserEntity>>

    @Query("SELECT * FROM local_users WHERE status = 'active'")
    fun observeActive(): Flow<List<LocalUserEntity>>

    @Query("DELETE FROM local_users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM local_users")
    suspend fun deleteAll()
}
