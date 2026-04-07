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
import com.remitos.app.data.db.dao.LocalDeviceDao
import com.remitos.app.data.db.dao.LocalDocumentDao
import com.remitos.app.data.db.dao.LocalScannedCodeDao
import com.remitos.app.data.db.dao.LocalSessionDao
import com.remitos.app.data.db.dao.LocalUserDao
import com.remitos.app.data.db.dao.OutboundDao
import com.remitos.app.data.db.dao.SequenceDao
import com.remitos.app.data.db.dao.SyncQueueDao
import com.remitos.app.data.db.entity.DebugLogEntity
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.InboundPackageEntity
import com.remitos.app.data.db.entity.LocalDeviceEntity
import com.remitos.app.data.db.entity.LocalDocumentEntity
import com.remitos.app.data.db.entity.LocalDocumentItemEntity
import com.remitos.app.data.db.entity.LocalScannedCodeEntity
import com.remitos.app.data.db.entity.LocalSessionEntity
import com.remitos.app.data.db.entity.LocalUserEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.data.db.entity.SequenceEntity
import com.remitos.app.data.db.entity.SyncQueueEntity

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
        LocalDeviceEntity::class,
        LocalUserEntity::class,
        LocalSessionEntity::class,
        LocalDocumentEntity::class,
        LocalDocumentItemEntity::class,
        LocalScannedCodeEntity::class,
        SyncQueueEntity::class,
    ],
    version = 14
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inboundDao(): InboundDao
    abstract fun outboundDao(): OutboundDao
    abstract fun sequenceDao(): SequenceDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun localDeviceDao(): LocalDeviceDao
    abstract fun localUserDao(): LocalUserDao
    abstract fun localSessionDao(): LocalSessionDao
    abstract fun localDocumentDao(): LocalDocumentDao
    abstract fun localScannedCodeDao(): LocalScannedCodeDao
    abstract fun syncQueueDao(): SyncQueueDao

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_device (
                        device_id TEXT PRIMARY KEY NOT NULL,
                        company_id TEXT NOT NULL,
                        warehouse_id TEXT,
                        registered_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL UNIQUE,
                        role TEXT NOT NULL,
                        pin_hash TEXT,
                        status TEXT NOT NULL,
                        last_synced_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_documents (
                        id TEXT PRIMARY KEY NOT NULL,
                        cloud_id TEXT,
                        warehouse_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        supplier_name TEXT,
                        document_number TEXT,
                        ocr_confidence REAL,
                        ocr_engine_used TEXT,
                        status TEXT NOT NULL,
                        created_by TEXT,
                        created_at_local INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_documents_warehouse_id ON local_documents(warehouse_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_documents_status ON local_documents(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_documents_type ON local_documents(type)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_document_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        document_id TEXT NOT NULL,
                        description TEXT NOT NULL,
                        expected_quantity INTEGER NOT NULL,
                        received_quantity INTEGER NOT NULL,
                        FOREIGN KEY(document_id) REFERENCES local_documents(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_document_items_document_id ON local_document_items(document_id)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_scanned_codes (
                        id TEXT PRIMARY KEY NOT NULL,
                        document_id TEXT NOT NULL,
                        document_item_id TEXT,
                        raw_value TEXT NOT NULL,
                        parsed_gtin TEXT,
                        parsed_sscc TEXT,
                        parsed_batch TEXT,
                        parsed_expiry INTEGER,
                        matched INTEGER NOT NULL DEFAULT 0,
                        scanned_by TEXT,
                        scanned_at_local INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(document_id) REFERENCES local_documents(id) ON DELETE CASCADE,
                        FOREIGN KEY(document_item_id) REFERENCES local_document_items(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_scanned_codes_document_id ON local_scanned_codes(document_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_scanned_codes_document_item_id ON local_scanned_codes(document_item_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_scanned_codes_raw_value ON local_scanned_codes(raw_value)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        retry_count INTEGER NOT NULL DEFAULT 0,
                        last_attempt_at INTEGER,
                        status TEXT NOT NULL,
                        UNIQUE(entity_type, entity_id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_last_attempt_at ON sync_queue(last_attempt_at)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_users ADD COLUMN warehouse_id TEXT")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inbound_notes ADD COLUMN extra_fields_json TEXT DEFAULT '{}'")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove barcode-related columns from inbound_packages table
                // SQLite doesn't support DROP COLUMN, so we recreate the table
                
                // Create new table without barcode columns
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS inbound_packages_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        inbound_note_id INTEGER NOT NULL,
                        package_index INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY(inbound_note_id) REFERENCES inbound_notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                
                // Copy data from old table to new table (only keeping non-barcode columns)
                db.execSQL(
                    """
                    INSERT INTO inbound_packages_new (id, inbound_note_id, package_index, status)
                    SELECT id, inbound_note_id, package_index, status FROM inbound_packages
                    """.trimIndent()
                )
                
                // Drop old table
                db.execSQL("DROP TABLE inbound_packages")
                
                // Rename new table to original name
                db.execSQL("ALTER TABLE inbound_packages_new RENAME TO inbound_packages")
                
                // Recreate index
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inbound_packages_inbound_note_id ON inbound_packages(inbound_note_id)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add barcode-related columns to inbound_packages table
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN barcode_raw TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN gtin TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN batch_lot TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN expiry_date TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN sscc TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN serial_number TEXT")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN scanned_at INTEGER")
                db.execSQL("ALTER TABLE inbound_packages ADD COLUMN scanned_by TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename pin_hash column to password_hash in local_users table
                // SQLite doesn't support ALTER COLUMN, so we recreate the table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_users_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL UNIQUE,
                        role TEXT NOT NULL,
                        password_hash TEXT,
                        status TEXT NOT NULL,
                        warehouse_id TEXT,
                        last_synced_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO local_users_new (id, username, role, password_hash, status, warehouse_id, last_synced_at)
                    SELECT id, username, role, pin_hash, status, warehouse_id, last_synced_at FROM local_users
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS local_users")
                db.execSQL("ALTER TABLE local_users_new RENAME TO local_users")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_session (
                        id TEXT PRIMARY KEY NOT NULL,
                        user_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        warehouse_id TEXT,
                        login_time INTEGER NOT NULL,
                        last_activity_time INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Build database with default name (for backward compatibility).
         * @deprecated Use DatabaseManager instead for multi-user support.
         */
        @Deprecated("Use DatabaseManager.getDatabase() instead")
        fun build(context: Context): AppDatabase {
            return build(context, "remitos.db")
        }

        /**
         * Build database with custom name (for multi-user support).
         * @param context Application context
         * @param databaseName Name of the database file
         */
        fun build(context: Context, databaseName: String): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                databaseName
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14
            ).build()
        }
    }
}
