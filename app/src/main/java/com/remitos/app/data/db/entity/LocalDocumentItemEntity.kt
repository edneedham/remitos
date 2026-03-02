package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_document_items",
    foreignKeys = [
        ForeignKey(
            entity = LocalDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["document_id"])]
)
data class LocalDocumentItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "document_id")
    val documentId: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "expected_quantity")
    val expectedQuantity: Int,
    @ColumnInfo(name = "received_quantity")
    val receivedQuantity: Int
)
