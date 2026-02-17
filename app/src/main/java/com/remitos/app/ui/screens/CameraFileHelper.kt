package com.remitos.app.ui.screens

import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createImageFile(baseDir: File): File {
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File(baseDir, "remito_$timestamp.jpg")
}

internal fun saveBitmapToFile(bitmap: Bitmap, baseDir: File): File {
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    val file = createImageFile(baseDir)
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
    }
    return file
}
