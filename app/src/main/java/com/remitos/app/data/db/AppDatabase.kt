package com.remitos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.remitos.app.data.db.dao.InboundDao
import com.remitos.app.data.db.dao.OutboundDao
import com.remitos.app.data.db.dao.SequenceDao
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.SequenceEntity

@Database(
    entities = [
        InboundNoteEntity::class,
        InboundPackageEntity::class,
        OutboundListEntity::class,
        OutboundLineEntity::class,
        SequenceEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inboundDao(): InboundDao
    abstract fun outboundDao(): OutboundDao
    abstract fun sequenceDao(): SequenceDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "remitos.db"
            ).build()
        }
    }
}
