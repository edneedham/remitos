package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.OutboundListStatus
import com.remitos.app.data.OutboundSearchFilters
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.OutboundListEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class OutboundHistoryViewModel(
    private val repository: RemitosRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val listStatusFilter = MutableStateFlow(setOf(OutboundListStatus.Abierta))
    private val lineStatusFilter = MutableStateFlow(emptySet<String>())

    val outboundLists: StateFlow<List<OutboundListEntity>> = combine(
        searchQuery.debounce(300).distinctUntilChanged(),
        listStatusFilter,
        lineStatusFilter,
        repository.observeOutboundLists(),
    ) { query, listStatuses, lineStatuses, _ ->
        OutboundSearchFilters(
            query = query,
            listStatuses = listStatuses,
            lineStatuses = lineStatuses,
        )
    }
        .flatMapLatest { filters ->
            flow {
                val results = withContext(ioDispatcher) {
                    repository.searchOutboundLists(filters)
                }
                emit(results)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reprintState = MutableStateFlow<OutboundReprintState?>(null)
    val isReprinting = MutableStateFlow(false)

    val selectedListStatuses: StateFlow<Set<String>> = listStatusFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listStatusFilter.value)
    val selectedLineStatuses: StateFlow<Set<String>> = lineStatusFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), lineStatusFilter.value)
    val currentQuery: StateFlow<String> = searchQuery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), searchQuery.value)

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun toggleListStatus(status: String) {
        val current = listStatusFilter.value.toMutableSet()
        if (!current.add(status)) {
            current.remove(status)
        }
        if (current.isEmpty()) {
            current.add(OutboundListStatus.Abierta)
        }
        listStatusFilter.value = current
    }

    fun toggleLineStatus(status: String) {
        val current = lineStatusFilter.value.toMutableSet()
        if (!current.add(status)) {
            current.remove(status)
        }
        lineStatusFilter.value = current
    }

    fun clearLineStatusFilters() {
        lineStatusFilter.value = emptySet()
    }

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
