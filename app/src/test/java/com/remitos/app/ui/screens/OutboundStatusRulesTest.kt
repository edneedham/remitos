package com.remitos.app.ui.screens

import com.remitos.app.data.OutboundLineStatus
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboundStatusRulesTest {
    @Test
    fun canCloseList_returnsTrueWhenAllFinal() {
        val lines = listOf(
            buildLine(status = OutboundLineStatus.Entregado),
            buildLine(status = OutboundLineStatus.Entregado),
        )

        assertTrue(canCloseList(lines))
    }

    @Test
    fun canCloseList_returnsFalseWhenNotFinal() {
        val lines = listOf(
            buildLine(status = OutboundLineStatus.EnTransito),
            buildLine(status = OutboundLineStatus.Entregado),
        )

        assertFalse(canCloseList(lines))
    }

    private fun buildLine(status: String): OutboundLineWithRemito {
        return OutboundLineWithRemito(
            id = 1L,
            outboundListId = 1L,
            inboundNoteId = 1L,
            deliveryNumber = "E-1",
            recipientNombre = "Ana",
            recipientApellido = "Lopez",
            recipientDireccion = "Calle 123",
            recipientTelefono = "111111111",
            packageQty = 1,
            allocatedPackageIds = "1",
            status = status,
            deliveredQty = 0,
            returnedQty = 0,
            remitoNumCliente = "R-1",
            remitoNumInterno = "RI-000001",
        )
    }
}
