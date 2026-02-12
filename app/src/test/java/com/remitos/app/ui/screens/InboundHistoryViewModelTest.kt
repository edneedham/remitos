package com.remitos.app.ui.screens

import com.remitos.app.data.InboundNoteStatus
import com.remitos.app.data.db.entity.InboundNoteEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InboundHistoryViewModelTest {
    @Test
    fun filterNotes_matchesSearchAndDateRange() {
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        val day1 = LocalDate.of(2024, 12, 1)
        val day2 = LocalDate.of(2024, 12, 2)
        val day3 = LocalDate.of(2024, 12, 3)

        val notes = listOf(
            buildNote(
                senderCuit = "20-12345678-9",
                senderNombre = "ACME",
                destNombre = "Juan",
                remitoNumCliente = "RC-1",
                createdAt = toMillis(day1, zone)
            ),
            buildNote(
                senderCuit = "23-00000000-1",
                senderNombre = "Beta",
                destNombre = "Maria",
                remitoNumCliente = "RC-2",
                createdAt = toMillis(day2, zone)
            ),
            buildNote(
                senderCuit = "27-99999999-9",
                senderNombre = "Gamma",
                destNombre = "Pedro",
                remitoNumCliente = "RC-3",
                createdAt = toMillis(day3, zone)
            )
        )

        val result = InboundHistoryViewModel.filterNotes(
            notes = notes,
            searchQuery = "maria",
            fromDate = "2024-12-02",
            toDate = "2024-12-03",
            zoneId = zone
        )

        assertEquals(1, result.size)
        assertEquals("23-00000000-1", result.first().senderCuit)
    }

    private fun buildNote(
        senderCuit: String,
        senderNombre: String,
        destNombre: String,
        remitoNumCliente: String,
        createdAt: Long
    ): InboundNoteEntity {
        return InboundNoteEntity(
            senderCuit = senderCuit,
            senderNombre = senderNombre,
            senderApellido = "",
            destNombre = destNombre,
            destApellido = "",
            destDireccion = "",
            destTelefono = "",
            cantBultosTotal = 1,
            remitoNumCliente = remitoNumCliente,
            remitoNumInterno = "",
            status = InboundNoteStatus.Activa,
            scanImagePath = null,
            ocrTextBlob = null,
            ocrConfidenceJson = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    private fun toMillis(date: LocalDate, zoneId: ZoneId): Long {
        return Instant.from(date.atStartOfDay(zoneId)).toEpochMilli()
    }
}
