package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.remitos.app.data.db.entity.SequenceEntity

@Dao
interface SequenceDao {
    @Query("SELECT * FROM sequences WHERE name = :name")
    suspend fun getSequence(name: String): SequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: SequenceEntity)

    @Update
    suspend fun updateSequence(sequence: SequenceEntity)
}
