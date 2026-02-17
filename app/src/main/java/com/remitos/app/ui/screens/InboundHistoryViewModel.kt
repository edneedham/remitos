package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class InboundHistoryViewModel(
    repository: RemitosRepository
) : ViewModel() {
    private val pageSize = 20
    private val searchQuery = MutableStateFlow("")
    private val fromDate = MutableStateFlow("")
    private val toDate = MutableStateFlow("")
    private val pageLimit = MutableStateFlow(pageSize)
    private val canLoadMore = MutableStateFlow(false)

    val searchQueryState: StateFlow<String> = searchQuery
    val fromDateState: StateFlow<String> = fromDate
    val toDateState: StateFlow<String> = toDate

    val filteredNotes: StateFlow<List<InboundNoteEntity>> = combine(
        repository.observeInboundNotes(),
        searchQuery,
        fromDate,
        toDate,
        pageLimit
    ) { notes, query, from, to, limit ->
        val filtered = filterNotes(notes, query, from, to, ZoneId.systemDefault())
        val hasMore = filtered.size > limit
        canLoadMore.value = hasMore
        if (hasMore) filtered.take(limit) else filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val canLoadMoreState: StateFlow<Boolean> = canLoadMore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), canLoadMore.value)

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
        pageLimit.value = pageSize
    }

    fun updateFromDate(value: String) {
        fromDate.value = value
        pageLimit.value = pageSize
    }

    fun updateToDate(value: String) {
        toDate.value = value
        pageLimit.value = pageSize
    }

    fun loadMore() {
        if (!canLoadMore.value) return
        pageLimit.value += pageSize
    }

    companion object {
        internal fun filterNotes(
            notes: List<InboundNoteEntity>,
            searchQuery: String,
            fromDate: String,
            toDate: String,
            zoneId: ZoneId
        ): List<InboundNoteEntity> {
            val query = searchQuery.trim().lowercase()
            val from = parseDateStart(fromDate, zoneId)
            val to = parseDateEnd(toDate, zoneId)

            return notes.asSequence()
                .filter { note ->
                    val textMatch = if (query.isBlank()) {
                        true
                    } else {
                        listOf(
                            note.senderCuit,
                            note.senderNombre,
                            note.senderApellido,
                            note.destNombre,
                            note.destApellido,
                            note.remitoNumCliente,
                            note.remitoNumInterno
                        ).any { it.lowercase().contains(query) }
                    }

                    val fromMatch = from?.let { note.createdAt >= it } ?: true
                    val toMatch = to?.let { note.createdAt <= it } ?: true
                    textMatch && fromMatch && toMatch
                }
                .sortedByDescending { it.createdAt }
                .toList()
        }

        private fun parseDateStart(value: String, zoneId: ZoneId): Long? {
            val date = parseDate(value) ?: return null
            return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

        private fun parseDateEnd(value: String, zoneId: ZoneId): Long? {
            val date = parseDate(value) ?: return null
            return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        }

        private fun parseDate(value: String): LocalDate? {
            return runCatching { LocalDate.parse(value.trim()) }.getOrNull()
        }
    }
}
