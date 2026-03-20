package com.remitos.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remitos.app.data.RemitosRepository
import com.remitos.app.data.db.entity.InboundNoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboundHistoryViewModel @Inject constructor(
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
        searchQuery.debounce(300).distinctUntilChanged(),
        fromDate.debounce(300).distinctUntilChanged(),
        toDate.debounce(300).distinctUntilChanged(),
        pageLimit
    ) { query, from, to, limit ->
        InboundQueryState(query, from, to, limit)
    }
        .flatMapLatest { state ->
            val zoneId = ZoneId.systemDefault()
            val fromMillis = parseDateStart(state.fromDate, zoneId)
            val toMillis = parseDateEnd(state.toDate, zoneId)
            repository.observeInboundNotesFiltered(
                query = state.query,
                from = fromMillis,
                to = toMillis,
                limit = state.limit + 1,
            )
        }
        .map { notes ->
            val limit = pageLimit.value
            val hasMore = notes.size > limit
            canLoadMore.value = hasMore
            if (hasMore) notes.take(limit) else notes
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    private fun parseDateStart(value: String, zoneId: ZoneId): Long? {
        val date = parseDate(value) ?: return null
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun parseDateEnd(value: String, zoneId: ZoneId): Long? {
        val date = parseDate(value) ?: return null
        return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }

    private fun parseDate(value: String): java.time.LocalDate? {
        return runCatching { java.time.LocalDate.parse(value.trim()) }.getOrNull()
    }
}

private data class InboundQueryState(
    val query: String,
    val fromDate: String,
    val toDate: String,
    val limit: Int,
)
