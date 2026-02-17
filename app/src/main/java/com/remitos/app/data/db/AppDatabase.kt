package com.remitos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.db.dao.DebugLogDao
import com.remitos.app.data.db.dao.InboundDao
import com.remitos.app.data.db.dao.OutboundDao
import com.remitos.app.data.db.dao.SequenceDao
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.SequenceEntity

@Database(
    entities = [
        InboundNoteEntity::class,
        InboundPackageEntity::class,
        OutboundListEntity::class,
        OutboundLineEntity::class,
        OutboundLineEditHistoryEntity::class,
        OutboundLineStatusHistoryEntity::class,
        SequenceEntity::class,
        DebugLogEntity::class,
    ],
    version = 7
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
                    "ALTER TABLE inbound_notes ADD COLUMN status TEXT NOT NULL DEFAULT '${InboundNoteStatus.Activa}'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE outbound_lines ADD COLUMN status TEXT NOT NULL DEFAULT '${OutboundLineStatus.EnDeposito}'"
                )
                db.execSQL(
                    "ALTER TABLE outbound_lists ADD COLUMN checklist_signature_path TEXT"
                )
                db.execSQL(
                    "ALTER TABLE outbound_lists ADD COLUMN checklist_signed_at INTEGER"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outbound_line_status_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        outbound_line_id INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(outbound_line_id) REFERENCES outbound_lines(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_line_status_history_outbound_line_id " +
                        "ON outbound_line_status_history(outbound_line_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_line_status_history_created_at " +
                        "ON outbound_line_status_history(created_at)"
                )
                db.execSQL(
                    "UPDATE outbound_lines SET status = '${OutboundLineStatus.EnDeposito}' " +
                        "WHERE status IN ('pendiente', 'devuelto')"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE outbound_lines ADD COLUMN missing_qty INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outbound_line_edit_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        outbound_line_id INTEGER NOT NULL,
                        field_name TEXT NOT NULL,
                        old_value TEXT NOT NULL,
                        new_value TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(outbound_line_id) REFERENCES outbound_lines(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_line_edit_history_outbound_line_id " +
                        "ON outbound_line_edit_history(outbound_line_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_line_edit_history_created_at " +
                        "ON outbound_line_edit_history(created_at)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_lists_status ON outbound_lists(status)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_lists_issue_date ON outbound_lists(issue_date)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_lines_outbound_list_id " +
                        "ON outbound_lines(outbound_list_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_outbound_lines_status ON outbound_lines(status)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inbound_notes_remito_num_cliente " +
                        "ON inbound_notes(remito_num_cliente)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inbound_notes_remito_num_interno " +
                        "ON inbound_notes(remito_num_interno)"
                )
            }
        }

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "remitos.db"
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            )
                .build()
        }
    }
}
