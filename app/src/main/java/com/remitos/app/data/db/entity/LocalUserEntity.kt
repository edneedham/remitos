package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class LocalUserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "pin_hash")
    val pinHash: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: String?,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long
)
