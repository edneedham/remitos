package com.remitos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remitos.app.data.db.entity.LocalDeviceEntity

@Dao
interface LocalDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: LocalDeviceEntity)

    @Query("SELECT * FROM local_device WHERE device_id = :deviceId")
    suspend fun getById(deviceId: String): LocalDeviceEntity?

    @Query("SELECT * FROM local_device LIMIT 1")
    suspend fun getDevice(): LocalDeviceEntity?

    @Query("DELETE FROM local_device")
    suspend fun deleteAll()
}
