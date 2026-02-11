package com.remitos.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val text: String,
    val fields: Map<String, String>,
    val confidence: Map<String, Float>
)

class OcrProcessor {
    private val minTargetEdgePx = 1200
    private val maxTargetEdgePx = 1800
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processImage(context: Context, uri: Uri): OcrResult {
        val image = withContext(Dispatchers.IO) {
            val grayscale = toGrayscaleBitmap(context, uri)
            InputImage.fromBitmap(grayscale, 0)
        }
        val text = recognizeText(image)
        val fields = extractFields(text)
        return OcrResult(text.text, fields.first, fields.second)
    }

    private suspend fun recognizeText(image: InputImage): Text {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    private fun extractFields(text: Text): Pair<Map<String, String>, Map<String, Float>> {
        return parseFields(text.text)
    }

    private fun toGrayscaleBitmap(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val bitmap = if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, true)
        }
        val normalized = normalizeBitmap(bitmap)
        val grayscale = Bitmap.createBitmap(normalized.width, normalized.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = android.graphics.Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(normalized, 0f, 0f, paint)
        return reduceNoise(grayscale)
    }

    private fun normalizeBitmap(bitmap: Bitmap): Bitmap {
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        val targetMaxEdge = when {
            maxEdge > maxTargetEdgePx -> maxTargetEdgePx
            maxEdge < minTargetEdgePx -> minTargetEdgePx
            else -> return bitmap
        }
        val scale = targetMaxEdge.toFloat() / maxEdge.toFloat()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun reduceNoise(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap

        val input = IntArray(width * height)
        val output = IntArray(width * height)
        bitmap.getPixels(input, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                var sum = 0
                for (dy in -1..1) {
                    val offset = (y + dy) * width
                    for (dx in -1..1) {
                        val color = input[offset + x + dx]
                        sum += color and 0xFF
                    }
                }
                val avg = (sum / 9).coerceIn(0, 255)
                output[rowOffset + x] = (0xFF shl 24) or (avg shl 16) or (avg shl 8) or avg
            }
        }

        for (x in 0 until width) {
            output[x] = input[x]
            output[(height - 1) * width + x] = input[(height - 1) * width + x]
        }
        for (y in 1 until height - 1) {
            output[y * width] = input[y * width]
            output[y * width + (width - 1)] = input[y * width + (width - 1)]
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    companion object {
        internal fun parseFields(raw: String): Pair<Map<String, String>, Map<String, Float>> {
            val fields = mutableMapOf<String, String>()
            val confidence = mutableMapOf<String, Float>()

            val cuitMatch = Regex("\\b\\d{2}-\\d{8}-\\d{1}\\b").find(raw)
            if (cuitMatch != null) {
                fields["sender_cuit"] = cuitMatch.value
                confidence["sender_cuit"] = 0.8f
            }

            val bultosMatch = Regex("(?i)cantidad\\s+de\\s+bultos\\s*[:\\-]?\\s*(\\d+)").find(raw)
            if (bultosMatch != null) {
                fields["cant_bultos_total"] = bultosMatch.groupValues[1]
                confidence["cant_bultos_total"] = 0.7f
            }

            val remitoClienteMatch = Regex(
                "(?i)remito\\s*(?:n\\s*[°o])?\\s*cliente\\s*[:\\-]?\\s*([\\w-]+)"
            ).find(raw)
            if (remitoClienteMatch != null) {
                fields["remito_num_cliente"] = remitoClienteMatch.groupValues[1]
                confidence["remito_num_cliente"] = 0.6f
            }

            val remitoInternoMatch = Regex(
                "(?i)remito\\s*(?:n\\s*[°o])?\\s*interno\\s*[:\\-]?\\s*([\\w-]+)"
            ).find(raw)
            if (remitoInternoMatch != null) {
                fields["remito_num_interno"] = remitoInternoMatch.groupValues[1]
                confidence["remito_num_interno"] = 0.6f
            }

            val remitenteMatch = Regex("(?i)remitente\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ]+)\\s+([A-Za-zÁÉÍÓÚÑáéíóúñ]+)")
                .find(raw)
            if (remitenteMatch != null) {
                fields["sender_nombre"] = remitenteMatch.groupValues[1]
                fields["sender_apellido"] = remitenteMatch.groupValues[2]
                confidence["sender_nombre"] = 0.6f
                confidence["sender_apellido"] = 0.6f
            }

            val destinatarioMatch = Regex("(?i)destinatario\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ]+)\\s+([A-Za-zÁÉÍÓÚÑáéíóúñ]+)")
                .find(raw)
            if (destinatarioMatch != null) {
                fields["dest_nombre"] = destinatarioMatch.groupValues[1]
                fields["dest_apellido"] = destinatarioMatch.groupValues[2]
                confidence["dest_nombre"] = 0.6f
                confidence["dest_apellido"] = 0.6f
            }

            val direccionMatch = Regex("(?i)direcci[oó]n\\s*destinatario\\s*[:\\-]?\\s*(.+)")
                .find(raw)
            if (direccionMatch != null) {
                fields["dest_direccion"] = direccionMatch.groupValues[1].trim()
                confidence["dest_direccion"] = 0.6f
            }

            val telefonoMatch = Regex("(?i)tel[eé]fono\\s*destinatario\\s*[:\\-]?\\s*([+\\d][\\d\\s-]+)")
                .find(raw)
            if (telefonoMatch != null) {
                fields["dest_telefono"] = telefonoMatch.groupValues[1].replace(" ", "").trim()
                confidence["dest_telefono"] = 0.6f
            }

            return fields to confidence
        }
    }
}
