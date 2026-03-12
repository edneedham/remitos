package com.remitos.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.InputStream
import kotlin.math.max

object BitmapUtils {
    fun decodeBitmap(context: Context, uri: Uri, maxEdge: Int = -1, isMutableRequired: Boolean = false): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.isMutableRequired = isMutableRequired
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    
                    if (maxEdge > 0) {
                        val size = info.size
                        val currentMax = max(size.width, size.height).coerceAtLeast(1)
                        if (currentMax > maxEdge) {
                            val scale = maxEdge.toFloat() / currentMax.toFloat()
                            val targetWidth = (size.width * scale).toInt().coerceAtLeast(1)
                            val targetHeight = (size.height * scale).toInt().coerceAtLeast(1)
                            decoder.setTargetSize(targetWidth, targetHeight)
                        }
                    }
                }
            } else {
                decodeWithBitmapFactory(context, uri, maxEdge, isMutableRequired)
            }
        }.getOrNull()
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxEdge: Int, isMutableRequired: Boolean): Bitmap? {
        val options = BitmapFactory.Options()
        if (maxEdge > 0) {
            options.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            var inSampleSize = 1
            val height = options.outHeight
            val width = options.outWidth
            val currentMax = max(height, width)
            
            if (currentMax > maxEdge) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= maxEdge && halfWidth / inSampleSize >= maxEdge) {
                    inSampleSize *= 2
                }
            }
            options.inSampleSize = inSampleSize
        }
        
        options.inJustDecodeBounds = false
        options.inMutable = isMutableRequired
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null
        
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            rotateBitmapIfNeeded(bitmap, stream)
        } ?: bitmap
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, inputStream: InputStream): Bitmap {
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        
        if (degrees == 0f) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}
