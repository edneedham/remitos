package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbound_line_edit_history",
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
data class OutboundLineEditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "outbound_line_id")
    val outboundLineId: Long,
    @ColumnInfo(name = "field_name")
    val fieldName: String,
    @ColumnInfo(name = "old_value")
    val oldValue: String,
    @ColumnInfo(name = "new_value")
    val newValue: String,
    @ColumnInfo(name = "reason")
    val reason: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "synced", defaultValue = "0")
    val synced: Boolean = false
)
