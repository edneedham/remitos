package com.remitos.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sequences")
data class SequenceEntity(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "next_value")
    val nextValue: Long
)
