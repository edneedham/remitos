package com.remitos.app.ui.screens

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createImageFile(baseDir: File): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File(baseDir, "remito_$timestamp.jpg")
}
