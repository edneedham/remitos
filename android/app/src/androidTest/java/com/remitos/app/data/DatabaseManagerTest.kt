package com.remitos.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.remitos.app.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up all test databases
        DatabaseManager.closeAllDatabases()
        context.databaseList()
            .filter { it.startsWith("remitos_test") }
            .forEach { context.deleteDatabase(it) }
    }

    @Test
    fun `getDatabase creates separate databases for different users`() = runBlocking {
        // Act
        val db1 = DatabaseManager.getDatabase(context, "test_user_1")
        val db2 = DatabaseManager.getDatabase(context, "test_user_2")

        // Assert
        assertNotNull(db1)
        assertNotNull(db2)
        assertNotSame(db1, db2)

        // Verify databases are isolated
        val db1Name = context.databaseList().find { it.contains("test_user_1") }
        val db2Name = context.databaseList().find { it.contains("test_user_2") }
        assertNotNull(db1Name)
        assertNotNull(db2Name)
        assertNotEquals(db1Name, db2Name)
    }

    @Test
    fun `getDatabase returns same instance for same user`() = runBlocking {
        // Act
        val db1 = DatabaseManager.getDatabase(context, "test_user_same")
        val db2 = DatabaseManager.getDatabase(context, "test_user_same")

        // Assert - should be same instance (cached)
        assertSame(db1, db2)
    }

    @Test
    fun `getOfflineDatabase creates shared offline database`() = runBlocking {
        // Act
        val offlineDb1 = DatabaseManager.getOfflineDatabase(context)
        val offlineDb2 = DatabaseManager.getOfflineDatabase(context)

        // Assert
        assertNotNull(offlineDb1)
        assertSame(offlineDb1, offlineDb2)
    }

    @Test
    fun `closeDatabase removes from cache`() = runBlocking {
        // Arrange
        val db1 = DatabaseManager.getDatabase(context, "test_close_user")
        
        // Act
        DatabaseManager.closeDatabase("test_close_user")
        val db2 = DatabaseManager.getDatabase(context, "test_close_user")

        // Assert - should be different instance after close
        assertNotSame(db1, db2)
    }

    @Test
    fun `deleteDatabase removes database file`() = runBlocking {
        // Arrange
        val userId = "test_delete_user"
        DatabaseManager.getDatabase(context, userId)
        
        // Verify database exists
        assertTrue(context.databaseList().any { it.contains(userId) })

        // Act
        DatabaseManager.deleteDatabase(context, userId)

        // Assert
        assertFalse(context.databaseList().any { it.contains(userId) })
    }

    @Test
    fun `listUserDatabases returns all user databases`() = runBlocking {
        // Arrange
        DatabaseManager.getDatabase(context, "list_user_1")
        DatabaseManager.getDatabase(context, "list_user_2")
        DatabaseManager.getOfflineDatabase(context)

        // Act
        val users = DatabaseManager.listUserDatabases(context)

        // Assert
        assertTrue(users.contains("list_user_1"))
        assertTrue(users.contains("list_user_2"))
        assertTrue(users.contains("offline"))
    }

    @Test
    fun `closeAllDatabases clears all cached instances`() = runBlocking {
        // Arrange
        val db1 = DatabaseManager.getDatabase(context, "close_all_1")
        val db2 = DatabaseManager.getDatabase(context, "close_all_2")
        
        // Act
        DatabaseManager.closeAllDatabases()
        
        // Assert - getting again should create new instances
        val newDb1 = DatabaseManager.getDatabase(context, "close_all_1")
        val newDb2 = DatabaseManager.getDatabase(context, "close_all_2")
        
        assertNotSame(db1, newDb1)
        assertNotSame(db2, newDb2)
    }

    @Test
    fun `database operations work correctly after switching users`() = runBlocking {
        // Arrange
        val user1Db = DatabaseManager.getDatabase(context, "switch_user_1")
        val user2Db = DatabaseManager.getDatabase(context, "switch_user_2")

        // Act - Insert data for user 1
        user1Db.inboundDao().insertInbound(
            com.remitos.app.data.db.entity.InboundNoteEntity(
                id = 0,
                remitoNumCliente = "REM001",
                remitoNumInterno = "RI-000001",
                cantBultosTotal = 5,
                cuitRemitente = "20-12345678-9",
                nombreRemitente = "Test Sender",
                createdAt = System.currentTimeMillis()
            )
        )

        // Insert data for user 2
        user2Db.inboundDao().insertInbound(
            com.remitos.app.data.db.entity.InboundNoteEntity(
                id = 0,
                remitoNumCliente = "REM002",
                remitoNumInterno = "RI-000002",
                cantBultosTotal = 3,
                cuitRemitente = "20-98765432-1",
                nombreRemitente = "Another Sender",
                createdAt = System.currentTimeMillis()
            )
        )

        // Assert - Data should be isolated
        val user1Notes = user1Db.inboundDao().getInboundNotes()
        val user2Notes = user2Db.inboundDao().getInboundNotes()

        assertEquals(1, user1Notes.size)
        assertEquals("REM001", user1Notes[0].remitoNumCliente)
        
        assertEquals(1, user2Notes.size)
        assertEquals("REM002", user2Notes[0].remitoNumCliente)
    }
}
