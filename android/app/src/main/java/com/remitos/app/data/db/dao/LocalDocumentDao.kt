package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.remitos.app.data.db.entity.LocalDocumentEntity
import com.remitos.app.data.db.entity.LocalDocumentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: LocalDocumentEntity): Long

    @Update
    suspend fun update(document: LocalDocumentEntity)

    @Query("SELECT * FROM local_documents WHERE id = :id")
    suspend fun getById(id: String): LocalDocumentEntity?

    @Query("SELECT * FROM local_documents WHERE cloud_id = :cloudId")
    suspend fun getByCloudId(cloudId: String): LocalDocumentEntity?

    @Query("SELECT * FROM local_documents WHERE synced = 0")
    suspend fun getUnsynced(): List<LocalDocumentEntity>

    @Query("SELECT * FROM local_documents WHERE warehouse_id = :warehouseId ORDER BY created_at_local DESC")
    fun observeByWarehouse(warehouseId: String): Flow<List<LocalDocumentEntity>>

    @Query("SELECT * FROM local_documents WHERE status = :status ORDER BY created_at_local DESC")
    fun observeByStatus(status: String): Flow<List<LocalDocumentEntity>>

    @Query("UPDATE local_documents SET synced = 1, cloud_id = :cloudId WHERE id = :id")
    suspend fun markSynced(id: String, cloudId: String)

    @Query("DELETE FROM local_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LocalDocumentItemEntity)

    @Query("SELECT * FROM local_document_items WHERE document_id = :documentId")
    suspend fun getItemsByDocumentId(documentId: String): List<LocalDocumentItemEntity>

    @Query("SELECT * FROM local_document_items WHERE document_id = :documentId")
    fun observeItemsByDocumentId(documentId: String): Flow<List<LocalDocumentItemEntity>>

    @Query("DELETE FROM local_document_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
}
