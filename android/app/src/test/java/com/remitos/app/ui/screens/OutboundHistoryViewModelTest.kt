package com.remitos.app.ui.screens

import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OutboundHistoryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RemitosRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.observeOutboundLists()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun requestReprint_setsReadyStateWhenListExists() = runTest {
        val viewModel = OutboundHistoryViewModel(repository, ioDispatcher = testDispatcher)
        val list = OutboundListEntity(
            id = 4L,
            listNumber = 22L,
            issueDate = 123L,
            driverNombre = "Ana",
            driverApellido = "Lopez",
            checklistSignaturePath = null,
            checklistSignedAt = null,
            status = OutboundListStatus.Abierta
        )
        whenever(repository.getOutboundList(4L)).thenReturn(list)
        whenever(repository.getOutboundLinesWithRemito(4L)).thenReturn(emptyList())

        viewModel.requestReprint(4L)
        advanceUntilIdle()

        val state = viewModel.reprintState.value
        assertTrue(state is OutboundReprintState.Ready)
        val payload = (state as OutboundReprintState.Ready).payload
        assertEquals(22L, payload.list.listNumber)
    }

    @Test
    fun requestReprint_setsErrorWhenListMissing() = runTest {
        val viewModel = OutboundHistoryViewModel(repository, ioDispatcher = testDispatcher)
        whenever(repository.getOutboundList(9L)).thenReturn(null)

        viewModel.requestReprint(9L)
        advanceUntilIdle()

        val state = viewModel.reprintState.value
        assertTrue(state is OutboundReprintState.Error)
    }
}
