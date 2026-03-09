package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class InboundNoteWithAvailable(
    @Embedded
    val note: InboundNoteEntity,
    @ColumnInfo(name = "available_count")
    val availableCount: Int
)
