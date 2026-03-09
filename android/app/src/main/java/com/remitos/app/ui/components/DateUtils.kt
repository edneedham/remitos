package com.remitos.app.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val dateTimeFullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "AR"))

    fun formatDate(timestamp: Long): String {
        val date = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(dateFormatter)
    }

    fun formatDateTime(timestamp: Long): String {
        val dateTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return dateTime.format(dateTimeFormatter)
    }

    fun formatDateTimeFull(timestamp: Long): String {
        val dateTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return dateTime.format(dateTimeFullFormatter)
    }

    fun formatTime(timestamp: Long): String {
        val dateTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return dateTime.format(timeFormatter)
    }
}
