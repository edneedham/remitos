package com.remitos.app.data

import androidx.room.withTransaction
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.DebugLogEntity
import kotlinx.coroutines.flow.Flow

class AuditRepository(private val db: AppDatabase) {
    companion object {
        private const val MaxDebugLogs = 200
    }

    fun observeDebugLogs(limit: Int = MaxDebugLogs): Flow<List<DebugLogEntity>> {
        return db.debugLogDao().observeRecent(limit)
    }

    suspend fun insertDebugLog(log: DebugLogEntity) {
        db.withTransaction {
            db.debugLogDao().insertLog(log)
            val total = db.debugLogDao().countLogs()
            val excess = calculateExcessLogs(total.toLong(), MaxDebugLogs)
            if (excess > 0) {
                db.debugLogDao().deleteOldest(excess)
            }
        }
    }
}
