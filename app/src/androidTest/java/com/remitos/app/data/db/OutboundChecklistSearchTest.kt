package com.remitos.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.OutboundSearchFilters
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutboundChecklistSearchTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RemitosRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RemitosRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun searchOutboundLists_matchesAllTokensAndFilters() = runBlocking {
        val now = System.currentTimeMillis()
        val noteId = db.inboundDao().insertInbound(
            InboundNoteEntity(
                senderCuit = "20-12345678-9",
                senderNombre = "Juan",
                senderApellido = "Lopez",
                destNombre = "Maria",
                destApellido = "Diaz",
                destDireccion = "Calle 123",
                destTelefono = "111111111",
                cantBultosTotal = 1,
                remitoNumCliente = "R-1",
                remitoNumInterno = "RI-000001",
                status = InboundNoteStatus.Activa,
                scanImagePath = null,
                ocrTextBlob = null,
                ocrConfidenceJson = null,
                createdAt = now,
                updatedAt = now,
            )
        )

        val openListId = db.outboundDao().insertOutboundList(
            OutboundListEntity(
                listNumber = 10,
                issueDate = now,
                driverNombre = "Lucia",
                driverApellido = "Fernandez",
                checklistSignaturePath = null,
                checklistSignedAt = null,
                status = OutboundListStatus.Abierta,
            )
        )
        db.outboundDao().insertOutboundLines(
            listOf(
                OutboundLineEntity(
                    outboundListId = openListId,
                    inboundNoteId = noteId,
                    deliveryNumber = "E-10",
                    recipientNombre = "Ana",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    packageQty = 1,
                    allocatedPackageIds = "",
                    status = OutboundLineStatus.EnDeposito,
                    deliveredQty = 0,
                    returnedQty = 0,
                )
            )
        )

        val closedListId = db.outboundDao().insertOutboundList(
            OutboundListEntity(
                listNumber = 11,
                issueDate = now,
                driverNombre = "Laura",
                driverApellido = "Perez",
                checklistSignaturePath = null,
                checklistSignedAt = null,
                status = OutboundListStatus.Cerrada,
            )
        )
        db.outboundDao().insertOutboundLines(
            listOf(
                OutboundLineEntity(
                    outboundListId = closedListId,
                    inboundNoteId = noteId,
                    deliveryNumber = "E-11",
                    recipientNombre = "Ana",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 999",
                    recipientTelefono = "111111111",
                    packageQty = 1,
                    allocatedPackageIds = "",
                    status = OutboundLineStatus.Entregado,
                    deliveredQty = 1,
                    returnedQty = 0,
                )
            )
        )

        val filters = OutboundSearchFilters(
            query = "Lopez 123",
            listStatuses = setOf(OutboundListStatus.Abierta),
            lineStatuses = setOf(OutboundLineStatus.EnDeposito),
        )
        val results = repository.searchOutboundLists(filters)
        assertEquals(1, results.size)
        assertEquals(openListId, results.first().id)

        val closedFilters = filters.copy(listStatuses = setOf(OutboundListStatus.Cerrada))
        val closedResults = repository.searchOutboundLists(closedFilters)
        assertTrue(closedResults.isEmpty())
    }
}
