package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.LocalScannedCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalScannedCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(code: LocalScannedCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<LocalScannedCodeEntity>)

    @Query("SELECT * FROM local_scanned_codes WHERE id = :id")
    suspend fun getById(id: String): LocalScannedCodeEntity?

    @Query("SELECT * FROM local_scanned_codes WHERE document_id = :documentId ORDER BY scanned_at_local DESC")
    fun observeByDocumentId(documentId: String): Flow<List<LocalScannedCodeEntity>>

    @Query("SELECT * FROM local_scanned_codes WHERE document_id = :documentId ORDER BY scanned_at_local DESC")
    suspend fun getByDocumentId(documentId: String): List<LocalScannedCodeEntity>

    @Query("SELECT * FROM local_scanned_codes WHERE synced = 0")
    suspend fun getUnsynced(): List<LocalScannedCodeEntity>

    @Query("SELECT * FROM local_scanned_codes WHERE raw_value = :rawValue AND document_id = :documentId")
    suspend fun findByRawValue(rawValue: String, documentId: String): LocalScannedCodeEntity?

    @Query("UPDATE local_scanned_codes SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM local_scanned_codes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM local_scanned_codes WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    @Query("SELECT COUNT(*) FROM local_scanned_codes WHERE document_id = :documentId AND matched = 1")
    suspend fun countMatched(documentId: String): Int

    @Query("SELECT COUNT(*) FROM local_scanned_codes WHERE document_id = :documentId")
    suspend fun countTotal(documentId: String): Int
}
