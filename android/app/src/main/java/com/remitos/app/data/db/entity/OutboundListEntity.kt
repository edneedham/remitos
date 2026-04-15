package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbound_lists",
    indices = [
        Index(value = ["status"]),
        Index(value = ["issue_date"]),
        Index(value = ["needs_sync"]),
    ]
)
data class OutboundListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "list_number")
    val listNumber: Long,
    @ColumnInfo(name = "issue_date")
    val issueDate: Long,
    @ColumnInfo(name = "driver_nombre")
    val driverNombre: String,
    @ColumnInfo(name = "driver_apellido")
    val driverApellido: String,
    @ColumnInfo(name = "checklist_signature_path")
    val checklistSignaturePath: String?,
    @ColumnInfo(name = "checklist_signed_at")
    val checklistSignedAt: Long?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "cloud_id", defaultValue = "NULL")
    val cloudId: String? = null,
    @ColumnInfo(name = "last_synced_at", defaultValue = "0")
    val lastSyncedAt: Long = 0,
    @ColumnInfo(name = "needs_sync", defaultValue = "1")
    val needsSync: Boolean = true
)
