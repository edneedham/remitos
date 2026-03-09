package com.remitos.app.data

import android.content.Context
import androidx.room.Room
import com.remitos.app.data.db.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages database instances for multiple users.
 * Each user gets their own isolated database file.
 */
object DatabaseManager {
    private val databases = mutableMapOf<String, AppDatabase>()
    private val mutex = Mutex()
    
    /**
     * Get or create a database instance for a specific user.
     * @param context Application context
     * @param userId Unique user identifier (email or user ID)
     * @return Database instance for the user
     */
    suspend fun getDatabase(context: Context, userId: String): AppDatabase = mutex.withLock {
        databases.getOrPut(userId) {
            createDatabase(context, userId)
        }
    }
    
    /**
     * Get database for offline mode (no user account).
     */
    suspend fun getOfflineDatabase(context: Context): AppDatabase = mutex.withLock {
        databases.getOrPut("offline") {
            createDatabase(context, "offline")
        }
    }
    
    /**
     * Close a specific user's database.
     */
    suspend fun closeDatabase(userId: String) = mutex.withLock {
        databases.remove(userId)?.close()
    }
    
    /**
     * Delete a user's database completely.
     * Use when logging out and choosing to delete local data.
     */
    suspend fun deleteDatabase(context: Context, userId: String) = mutex.withLock {
        closeDatabase(userId)
        context.deleteDatabase("remitos_$userId")
    }
    
    /**
     * Close all open databases.
     * Call when app is shutting down or switching modes.
     */
    suspend fun closeAllDatabases() = mutex.withLock {
        databases.values.forEach { it.close() }
        databases.clear()
    }
    
    /**
     * List all user databases on device.
     */
    fun listUserDatabases(context: Context): List<String> {
        return context.databaseList()
            .filter { it.startsWith("remitos_") && !it.endsWith("-journal") && !it.endsWith("-shm") && !it.endsWith("-wal") }
            .map { it.removePrefix("remitos_") }
    }
    
    private fun createDatabase(context: Context, userId: String): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "remitos_$userId"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
