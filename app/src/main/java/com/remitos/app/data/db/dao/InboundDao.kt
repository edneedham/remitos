package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInbound(note: InboundNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<InboundPackageEntity>)

    @Query("SELECT * FROM inbound_notes ORDER BY created_at DESC")
    fun observeInboundNotes(): Flow<List<InboundNoteEntity>>

    @Query("SELECT * FROM inbound_notes WHERE id = :id")
    suspend fun getInboundNote(id: Long): InboundNoteEntity?

    @Query("SELECT COUNT(*) FROM inbound_packages WHERE inbound_note_id = :noteId AND status = 'disponible'")
    suspend fun countAvailablePackages(noteId: Long): Int
}
