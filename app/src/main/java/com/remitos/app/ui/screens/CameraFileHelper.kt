package com.remitos.app.ui.screens

import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_IMAGE_EDGE_PX = 1600

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
    val scaled = scaleBitmapIfNeeded(bitmap, MAX_IMAGE_EDGE_PX)
    val file = createImageFile(baseDir)
    FileOutputStream(file).use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)
    }
    return file
}

private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val maxCurrentEdge = maxOf(width, height)
    if (maxCurrentEdge <= maxEdge) return bitmap

    val scale = maxEdge.toFloat() / maxCurrentEdge.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
