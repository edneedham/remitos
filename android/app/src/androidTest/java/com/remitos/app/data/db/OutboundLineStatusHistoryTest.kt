package com.remitos.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.OutboundListStatus
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
class OutboundLineStatusHistoryTest {
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
    fun updateOutboundLineOutcome_appendsHistoryAndUpdatesStatus() = runBlocking {
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

        val listId = db.outboundDao().insertOutboundList(
            OutboundListEntity(
                listNumber = 1,
                issueDate = now,
                driverNombre = "Ana",
                driverApellido = "Perez",
                checklistSignaturePath = null,
                checklistSignedAt = null,
                status = OutboundListStatus.Abierta,
            )
        )

        db.outboundDao().insertOutboundLines(
            listOf(
                OutboundLineEntity(
                    outboundListId = listId,
                    inboundNoteId = noteId,
                    deliveryNumber = "E-1",
                    recipientNombre = "Ana",
                    recipientApellido = "Lopez",
                    recipientDireccion = "Calle 123",
                    recipientTelefono = "111111111",
                    packageQty = 1,
                    allocatedPackageIds = "",
                    status = OutboundLineStatus.EnDeposito,
                    deliveredQty = 0,
                    returnedQty = 0,
                    missingQty = 0,
                )
            )
        )

        val lineId = db.outboundDao().getLinesForList(listId).first().id

        repository.updateOutboundLineOutcome(lineId, OutboundLineStatus.Entregado, deliveredQty = 1, returnedQty = 0)
        repository.updateOutboundLineOutcome(lineId, OutboundLineStatus.EnDeposito, deliveredQty = 0, returnedQty = 0)

        val history = repository.getOutboundLineStatusHistory(lineId)
        assertEquals(2, history.size)
        assertEquals(OutboundLineStatus.Entregado, history[0].status)
        assertEquals(OutboundLineStatus.EnDeposito, history[1].status)
        assertTrue(history[0].createdAt > 0)
        assertTrue(history[1].createdAt >= history[0].createdAt)

        val updatedLine = db.outboundDao().getLinesForList(listId).first()
        assertEquals(OutboundLineStatus.EnDeposito, updatedLine.status)
    }
}
