package com.remitos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.remitos.app.data.db.dao.DebugLogDao
import com.remitos.app.data.db.dao.InboundDao
import com.remitos.app.data.db.dao.OutboundDao
import com.remitos.app.data.db.dao.SequenceDao
import com.remitos.app.data.db.entity.DebugLogEntity
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
        SequenceEntity::class,
        DebugLogEntity::class,
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inboundDao(): InboundDao
    abstract fun outboundDao(): OutboundDao
    abstract fun sequenceDao(): SequenceDao
    abstract fun debugLogDao(): DebugLogDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debug_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        created_at INTEGER NOT NULL,
                        scan_id INTEGER,
                        ocr_confidence_json TEXT,
                        preprocess_time_ms INTEGER,
                        failure_reason TEXT,
                        image_width INTEGER,
                        image_height INTEGER,
                        device_model TEXT,
                        parsing_error_summary TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE inbound_notes ADD COLUMN status TEXT NOT NULL DEFAULT 'activa'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE outbound_lines ADD COLUMN status TEXT NOT NULL DEFAULT 'pendiente'"
                )
                db.execSQL(
                    "ALTER TABLE outbound_lists ADD COLUMN checklist_signature_path TEXT"
                )
                db.execSQL(
                    "ALTER TABLE outbound_lists ADD COLUMN checklist_signed_at INTEGER"
                )
            }
        }

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "remitos.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}
