package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_device")
data class LocalDeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "company_id")
    val companyId: String,
    @ColumnInfo(name = "warehouse_id")
    val warehouseId: String?,
    @ColumnInfo(name = "registered_at")
    val registeredAt: Long
)
