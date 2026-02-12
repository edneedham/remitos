package com.remitos.app.data

internal fun calculateExcessLogs(total: Long, max: Int): Int {
    val excess = total - max
    return if (excess > 0) excess.toInt() else 0
}
