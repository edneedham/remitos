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
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
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
    private val detectionEdgePx = 800
    @Volatile
    private var isOpenCvReady = false
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processImage(
        context: Context,
        uri: Uri,
        enableCorrection: Boolean = true,
    ): OcrResult {
        val image = withContext(Dispatchers.IO) {
            val grayscale = toGrayscaleBitmap(context, uri)
            val enhanced = if (enableCorrection) applyClaheIfNeeded(grayscale) else grayscale
            val corrected = if (enableCorrection) correctPerspectiveOrDeskew(enhanced) else enhanced
            InputImage.fromBitmap(corrected, 0)
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

    private fun correctPerspectiveOrDeskew(bitmap: Bitmap): Bitmap {
        if (!ensureOpenCvReady()) return bitmap

        val rgba = Mat()
        val gray = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            val quad = detectDocumentQuad(gray)
            if (quad != null) {
                val warped = warpToQuad(gray, quad)
                return matToBitmap(warped)
            }

            val deskewed = deskew(gray)
            return matToBitmap(deskewed)
        } finally {
            rgba.release()
            gray.release()
        }
    }

    private fun detectDocumentQuad(gray: Mat): MatOfPoint2f? {
        val detection = Mat()
        val edges = Mat()
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        try {
            val maxEdge = maxOf(gray.width(), gray.height()).toDouble()
            val scale = if (maxEdge > detectionEdgePx) detectionEdgePx / maxEdge else 1.0
            if (scale != 1.0) {
                Imgproc.resize(gray, detection, Size(gray.width() * scale, gray.height() * scale))
            } else {
                gray.copyTo(detection)
            }

            Imgproc.GaussianBlur(detection, detection, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(detection, edges, 75.0, 200.0)
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            var bestArea = 0.0
            var bestQuad: MatOfPoint2f? = null

            for (contour in contours) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.total() == 4L) {
                    val area = Imgproc.contourArea(approx)
                    if (area > bestArea) {
                        bestArea = area
                        bestQuad?.release()
                        bestQuad = approx
                    } else {
                        approx.release()
                    }
                } else {
                    approx.release()
                }
                contour2f.release()
            }

            if (bestQuad != null) {
                val factor = if (scale != 1.0) 1.0 / scale else 1.0
                val scaled = MatOfPoint2f(*bestQuad.toArray().map { point ->
                    Point(point.x * factor, point.y * factor)
                }.toTypedArray())
                bestQuad.release()
                return scaled
            }

            return null
        } finally {
            detection.release()
            edges.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private fun warpToQuad(gray: Mat, quad: MatOfPoint2f): Mat {
        val ordered = orderPoints(quad.toArray())
        val tl = ordered[0]
        val tr = ordered[1]
        val br = ordered[2]
        val bl = ordered[3]

        val widthTop = distance(tl, tr)
        val widthBottom = distance(bl, br)
        val maxWidth = maxOf(widthTop, widthBottom)

        val heightLeft = distance(tl, bl)
        val heightRight = distance(tr, br)
        val maxHeight = maxOf(heightLeft, heightRight)

        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )
        val src = MatOfPoint2f(tl, tr, br, bl)

        val transform = Imgproc.getPerspectiveTransform(src, dst)
        val warped = Mat()
        Imgproc.warpPerspective(gray, warped, transform, Size(maxWidth, maxHeight))

        transform.release()
        src.release()
        dst.release()
        quad.release()

        return warped
    }

    private fun deskew(gray: Mat): Mat {
        val binary = Mat()
        val nonZero = MatOfPoint()
        val deskewed = Mat()
        try {
            Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
            Core.findNonZero(binary, nonZero)
            if (nonZero.empty()) return gray.clone()

            val points = MatOfPoint2f(*nonZero.toArray())
            val rect = Imgproc.minAreaRect(points)
            points.release()

            var angle = rect.angle
            if (rect.size.width < rect.size.height) {
                angle += 90.0
            }
            if (kotlin.math.abs(angle) < 1.0) return gray.clone()

            val center = Point(gray.width() / 2.0, gray.height() / 2.0)
            val rotation = Imgproc.getRotationMatrix2D(center, angle, 1.0)
            Imgproc.warpAffine(gray, deskewed, rotation, gray.size())
            rotation.release()

            return deskewed
        } finally {
            binary.release()
            nonZero.release()
        }
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val result = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        val rgba = Mat()
        try {
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA)
            Utils.matToBitmap(rgba, result)
        } finally {
            rgba.release()
            mat.release()
        }
        return result
    }

    private fun orderPoints(points: Array<Point>): Array<Point> {
        val sumComparator = points.sortedBy { it.x + it.y }
        val diffComparator = points.sortedBy { it.y - it.x }
        val tl = sumComparator.first()
        val br = sumComparator.last()
        val tr = diffComparator.first()
        val bl = diffComparator.last()
        return arrayOf(tl, tr, br, bl)
    }

    private fun distance(a: Point, b: Point): Double {
        return kotlin.math.hypot(a.x - b.x, a.y - b.y)
    }

    private fun applyClaheIfNeeded(bitmap: Bitmap): Bitmap {
        if (!ensureOpenCvReady()) return bitmap

        val rgba = Mat()
        val gray = Mat()
        val small = Mat()
        val claheOutput = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            val maxEdge = maxOf(gray.width(), gray.height()).toDouble()
            val scale = if (maxEdge > detectionEdgePx) detectionEdgePx / maxEdge else 1.0
            if (scale != 1.0) {
                Imgproc.resize(gray, small, Size(gray.width() * scale, gray.height() * scale))
            } else {
                gray.copyTo(small)
            }

            val mean = MatOfDouble()
            val stddev = MatOfDouble()
            try {
                Core.meanStdDev(small, mean, stddev)
                val contrast = stddev.toArray().firstOrNull() ?: 0.0
                if (!shouldApplyClahe(contrast)) {
                    return bitmap
                }
            } finally {
                mean.release()
                stddev.release()
            }

            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            clahe.apply(gray, claheOutput)
            return matToBitmap(claheOutput)
        } finally {
            rgba.release()
            gray.release()
            small.release()
        }
    }

    internal fun shouldApplyClahe(contrastStdDev: Double): Boolean {
        return contrastStdDev < 35.0
    }

    private fun ensureOpenCvReady(): Boolean {
        if (isOpenCvReady) return true
        synchronized(this) {
            if (!isOpenCvReady) {
                isOpenCvReady = try {
                    System.loadLibrary("opencv_java4")
                    true
                } catch (_: UnsatisfiedLinkError) {
                    false
                }
            }
        }
        return isOpenCvReady
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
