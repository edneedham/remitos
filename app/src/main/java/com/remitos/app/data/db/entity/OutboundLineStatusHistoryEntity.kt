package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbound_line_status_history",
    foreignKeys = [
        ForeignKey(
            entity = OutboundLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["outbound_line_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("outbound_line_id"), Index("created_at")]
)
data class OutboundLineStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "outbound_line_id")
    val outboundLineId: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
