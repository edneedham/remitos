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
class OutboundLineEditHistoryTest {
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
    fun updateOutboundLineDetails_appendsEditHistory() = runBlocking {
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

        repository.updateOutboundLineDetails(
            lineId = lineId,
            deliveryNumber = "E-2",
            recipientNombre = "Ana",
            recipientApellido = "Gomez",
            recipientDireccion = "Calle 456",
            recipientTelefono = "222222222",
            missingQty = 1,
            reason = "Corrección de datos",
        )

        val edits = repository.getOutboundLineEditHistory(lineId)
        assertTrue(edits.any { it.fieldName == "delivery_number" })
        assertTrue(edits.any { it.fieldName == "recipient_apellido" })
        assertTrue(edits.any { it.fieldName == "missing_qty" })
        assertEquals("Corrección de datos", edits.first().reason)

        val updatedLine = db.outboundDao().getOutboundLine(lineId)
        assertEquals("E-2", updatedLine?.deliveryNumber)
        assertEquals("Gomez", updatedLine?.recipientApellido)
        assertEquals(1, updatedLine?.missingQty)
    }
}
