package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutboundHistoryViewModel(
    private val repository: RemitosRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val outboundLists: StateFlow<List<OutboundListEntity>> = repository
        .observeOutboundLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reprintState = MutableStateFlow<OutboundReprintState?>(null)
    val isReprinting = MutableStateFlow(false)

    fun requestReprint(listId: Long) {
        if (isReprinting.value) return
        isReprinting.value = true
        reprintState.value = null

        viewModelScope.launch {
            try {
                val list = withContext(ioDispatcher) {
                    repository.getOutboundList(listId)
                }
                if (list == null) {
                    reprintState.value = OutboundReprintState.Error(
                        "No se encontro la lista para reimprimir."
                    )
                } else {
                    val lines = withContext(ioDispatcher) {
                        repository.getOutboundLinesWithRemito(listId)
                    }
                    reprintState.value = OutboundReprintState.Ready(
                        OutboundPrintPayload(list, lines)
                    )
                }
            } catch (error: Exception) {
                reprintState.value = OutboundReprintState.Error(
                    "No se pudo preparar la reimpresion. Intenta de nuevo."
                )
            } finally {
                isReprinting.value = false
            }
        }
    }

    fun clearReprintState() {
        reprintState.value = null
    }
}

sealed interface OutboundReprintState {
    data class Ready(val payload: OutboundPrintPayload) : OutboundReprintState
    data class Error(val message: String) : OutboundReprintState
}
