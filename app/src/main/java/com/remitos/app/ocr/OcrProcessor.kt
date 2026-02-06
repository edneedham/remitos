package com.remitos.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val text: String,
    val fields: Map<String, String>,
    val confidence: Map<String, Float>
)

class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(context: Context, uri: Uri): OcrResult {
        val image = InputImage.fromFilePath(context, uri)
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

            return fields to confidence
        }
    }
}
