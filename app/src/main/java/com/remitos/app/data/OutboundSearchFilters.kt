package com.remitos.app.data

data class OutboundSearchFilters(
    val query: String = "",
    val listStatuses: Set<String> = emptySet(),
    val lineStatuses: Set<String> = emptySet(),
)
