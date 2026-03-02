package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_session")
data class LocalSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "current_session",
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: String?,
    @ColumnInfo(name = "login_time")
    val loginTime: Long,
    @ColumnInfo(name = "last_activity_time")
    val lastActivityTime: Long
)
