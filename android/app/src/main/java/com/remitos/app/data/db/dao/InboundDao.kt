package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.InboundNoteWithAvailable
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInbound(note: InboundNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<InboundPackageEntity>)

    @Query("SELECT * FROM inbound_notes ORDER BY created_at DESC")
    fun observeInboundNotes(): Flow<List<InboundNoteEntity>>

    @Query(
        """
        SELECT * FROM inbound_notes
        WHERE (
            :query = '' OR
            LOWER(sender_cuit) LIKE :queryLike OR
            LOWER(sender_nombre) LIKE :queryLike OR
            LOWER(sender_apellido) LIKE :queryLike OR
            LOWER(dest_nombre) LIKE :queryLike OR
            LOWER(dest_apellido) LIKE :queryLike OR
            LOWER(remito_num_cliente) LIKE :queryLike OR
            LOWER(remito_num_interno) LIKE :queryLike
        )
        AND (:from IS NULL OR created_at >= :from)
        AND (:to IS NULL OR created_at <= :to)
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeInboundNotesFiltered(
        query: String,
        queryLike: String,
        from: Long?,
        to: Long?,
        limit: Int,
    ): Flow<List<InboundNoteEntity>>

    @Query(
        """
        SELECT n.*, COUNT(p.id) AS available_count
        FROM inbound_notes n
        LEFT JOIN inbound_packages p
            ON p.inbound_note_id = n.id AND p.status = 'disponible'
        GROUP BY n.id
        ORDER BY n.created_at DESC
        """
    )
    fun observeInboundNotesWithAvailable(): Flow<List<InboundNoteWithAvailable>>

    @Query("SELECT * FROM inbound_notes WHERE id = :id")
    suspend fun getInboundNote(id: Long): InboundNoteEntity?

    @Update
    suspend fun updateInbound(note: InboundNoteEntity)

    @Query("SELECT COUNT(*) FROM inbound_packages WHERE inbound_note_id = :noteId AND status = 'disponible'")
    suspend fun countAvailablePackages(noteId: Long): Int

    @Query("SELECT COUNT(*) FROM inbound_packages WHERE inbound_note_id = :noteId")
    suspend fun countPackages(noteId: Long): Int

    @Query("SELECT COUNT(*) FROM inbound_packages WHERE inbound_note_id = :noteId AND status = :status")
    suspend fun countPackagesByStatus(noteId: Long, status: String): Int

    @Query("SELECT MAX(package_index) FROM inbound_packages WHERE inbound_note_id = :noteId")
    suspend fun getMaxPackageIndex(noteId: Long): Int?

    @Query(
        """
        SELECT id FROM inbound_packages
        WHERE inbound_note_id = :noteId AND status = 'disponible'
        ORDER BY package_index
        LIMIT :limit
        """
    )
    suspend fun getAvailablePackageIds(noteId: Long, limit: Int): List<Long>

    @Query(
        """
        SELECT id FROM inbound_packages
        WHERE inbound_note_id = :noteId AND status = :status
        ORDER BY package_index DESC
        LIMIT :limit
        """
    )
    suspend fun getPackageIdsForTrim(noteId: Long, status: String, limit: Int): List<Long>

    @Query("DELETE FROM inbound_packages WHERE id IN (:packageIds)")
    suspend fun deletePackages(packageIds: List<Long>)

    @Query("UPDATE inbound_packages SET status = :status WHERE id IN (:packageIds)")
    suspend fun updatePackageStatus(packageIds: List<Long>, status: String)

    @Query("UPDATE inbound_packages SET status = :status WHERE inbound_note_id = :noteId")
    suspend fun updatePackageStatusForNote(noteId: Long, status: String)

    @Query("SELECT * FROM inbound_packages WHERE inbound_note_id = :noteId ORDER BY package_index")
    suspend fun getPackagesForNote(noteId: Long): List<InboundPackageEntity>

    @Update
    suspend fun updatePackage(packageEntity: InboundPackageEntity)

    @Query("SELECT * FROM inbound_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): InboundNoteEntity?
}
