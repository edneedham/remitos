package com.remitos.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
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
    val confidence: Map<String, Float>,
    val preprocessTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val parsingErrorSummary: String?
)

data class OcrDebugInfo(
    val preprocessTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

class OcrProcessingException(
    val debugInfo: OcrDebugInfo,
    cause: Throwable,
) : Exception(cause)

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
        val preprocessStart = SystemClock.elapsedRealtime()
        val preprocessResult = withContext(Dispatchers.IO) {
            val grayscale = toGrayscaleBitmap(context, uri)
            val enhanced = if (enableCorrection) applyClaheIfNeeded(grayscale) else grayscale
            val corrected = if (enableCorrection) correctPerspectiveOrDeskew(enhanced) else enhanced
            val inputImage = InputImage.fromBitmap(corrected, 0)
            Triple(inputImage, corrected.width, corrected.height)
        }
        val preprocessTimeMs = SystemClock.elapsedRealtime() - preprocessStart
        val image = preprocessResult.first
        val width = preprocessResult.second
        val height = preprocessResult.third
        val text = try {
            recognizeText(image)
        } catch (error: Exception) {
            throw OcrProcessingException(
                debugInfo = OcrDebugInfo(preprocessTimeMs, width, height),
                cause = error
            )
        }
        val fields = extractFields(text)
        val parsingSummary = buildParsingErrorSummary(fields.first)
        return OcrResult(
            text = text.text,
            fields = fields.first,
            confidence = fields.second,
            preprocessTimeMs = preprocessTimeMs,
            imageWidth = width,
            imageHeight = height,
            parsingErrorSummary = parsingSummary
        )
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
            val normalized = raw.replace("\r", "\n")
            val lines = normalized.lines()

            val senderLabels = listOf(
                "remitente",
                "proveedor",
                "emisor",
                "vendedor",
            )
            val destLabels = listOf(
                "destinatario",
                "cliente",
                "receptor",
                "comprador",
                "consignatario",
            )
            val addressLabels = listOf(
                "dirección",
                "direccion",
                "domicilio",
                "dirección de entrega",
                "direccion de entrega",
                "domicilio de entrega",
                "dirección destinatario",
                "direccion destinatario",
                "domicilio destinatario",
                "dirección cliente",
                "direccion cliente",
                "domicilio cliente",
            )
            val phoneLabels = listOf(
                "teléfono",
                "telefono",
                "tel",
                "tel.",
                "celular",
                "contacto",
            )
            val bultosLabels = listOf(
                "cantidad de bultos",
                "cant bultos",
                "cant. bultos",
                "bultos",
            )
            val documentNumberLabels = listOf(
                "remito cliente",
                "remito",
                "nota de entrega",
                "guía de despacho",
                "guia de despacho",
                "orden de entrega",
                "factura",
                "comprobante",
                "documento",
            )
            val cuitLabels = listOf(
                "cuit",
                "cuil",
                "cuit/cuil",
                "cuit remitente",
                "cuit proveedor",
                "cuit emisor",
            )

            val knownLabels = senderLabels + destLabels + addressLabels + phoneLabels + bultosLabels + documentNumberLabels + cuitLabels

            val senderValue = findLabeledValue(lines, senderLabels, knownLabels)
            if (senderValue != null) {
                val (nombre, apellido) = splitPersonName(senderValue.value)
                if (nombre.isNotBlank()) {
                    fields[OcrFieldKeys.SenderNombre] = nombre
                    confidence[OcrFieldKeys.SenderNombre] = senderValue.confidence
                }
                if (apellido.isNotBlank()) {
                    fields[OcrFieldKeys.SenderApellido] = apellido
                    confidence[OcrFieldKeys.SenderApellido] = senderValue.confidence
                }
            }

            val destValue = findLabeledValue(lines, destLabels, knownLabels)
            if (destValue != null) {
                val (nombre, apellido) = splitPersonName(destValue.value)
                if (nombre.isNotBlank()) {
                    fields[OcrFieldKeys.DestNombre] = nombre
                    confidence[OcrFieldKeys.DestNombre] = destValue.confidence
                }
                if (apellido.isNotBlank()) {
                    fields[OcrFieldKeys.DestApellido] = apellido
                    confidence[OcrFieldKeys.DestApellido] = destValue.confidence
                }
            }

            val addressValue = findLabeledValue(lines, addressLabels, knownLabels)
            if (addressValue != null && addressValue.value.isNotBlank()) {
                fields[OcrFieldKeys.DestDireccion] = addressValue.value
                confidence[OcrFieldKeys.DestDireccion] = addressValue.confidence
            }

            val phoneValue = findLabeledValue(lines, phoneLabels, knownLabels)
            if (phoneValue != null && phoneValue.value.isNotBlank()) {
                val normalizedPhone = phoneValue.value.replace(" ", "").trim()
                if (normalizedPhone.isNotBlank()) {
                    fields[OcrFieldKeys.DestTelefono] = normalizedPhone
                    confidence[OcrFieldKeys.DestTelefono] = phoneValue.confidence
                }
            }

            val bultosValue = findLabeledValue(lines, bultosLabels, knownLabels)
            if (bultosValue != null) {
                val quantity = Regex("\\d+").find(bultosValue.value)?.value
                if (!quantity.isNullOrBlank()) {
                    fields[OcrFieldKeys.CantBultosTotal] = quantity
                    confidence[OcrFieldKeys.CantBultosTotal] = bultosValue.confidence
                }
            }

            val documentValue = findLabeledValue(
                lines,
                documentNumberLabels,
                knownLabels,
            ) { value -> value.any { it.isDigit() } }
            if (documentValue != null && documentValue.value.isNotBlank()) {
                fields[OcrFieldKeys.RemitoNumCliente] = documentValue.value
                confidence[OcrFieldKeys.RemitoNumCliente] = documentValue.confidence
            }

            val cuitValue = findLabeledValue(lines, cuitLabels, knownLabels)
            if (cuitValue != null) {
                val match = Regex("\\b\\d{2}-\\d{8}-\\d{1}\\b").find(cuitValue.value)
                if (match != null) {
                    fields[OcrFieldKeys.SenderCuit] = match.value
                    confidence[OcrFieldKeys.SenderCuit] = cuitValue.confidence
                }
            }

            val cuitMatch = Regex("\\b\\d{2}-\\d{8}-\\d{1}\\b").find(raw)
            if (cuitMatch != null && !fields.containsKey(OcrFieldKeys.SenderCuit)) {
                fields[OcrFieldKeys.SenderCuit] = cuitMatch.value
                confidence[OcrFieldKeys.SenderCuit] = 0.8f
            }

            val bultosMatch = Regex("(?i)(?:cantidad\\s*de\\s*)?bultos\\s*[:\\-]?\\s*(\\d+)").find(raw)
            if (bultosMatch != null && !fields.containsKey(OcrFieldKeys.CantBultosTotal)) {
                fields[OcrFieldKeys.CantBultosTotal] = bultosMatch.groupValues[1]
                confidence[OcrFieldKeys.CantBultosTotal] = 0.7f
            }

            val remitoClienteMatch = Regex(
                "(?i)(?:remito\\s*(?:n\\s*[°o])?\\s*cliente|remito|factura|nota\\s*de\\s*entrega|gu[ií]a\\s*de\\s*despacho|orden\\s*de\\s*entrega)\\s*[:\\-]?\\s*([\\w-]+)"
            ).find(raw)
            if (remitoClienteMatch != null && !fields.containsKey(OcrFieldKeys.RemitoNumCliente)) {
                fields[OcrFieldKeys.RemitoNumCliente] = remitoClienteMatch.groupValues[1]
                confidence[OcrFieldKeys.RemitoNumCliente] = 0.6f
            }

            if (!fields.containsKey(OcrFieldKeys.SenderNombre) || !fields.containsKey(OcrFieldKeys.SenderApellido)) {
                val remitenteMatch = Regex("(?i)remitente\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ]+(?:\\s+[A-Za-zÁÉÍÓÚÑáéíóúñ]+)*)")
                    .find(raw)
                if (remitenteMatch != null) {
                    val (nombre, apellido) = splitPersonName(remitenteMatch.groupValues[1])
                    if (!fields.containsKey(OcrFieldKeys.SenderNombre) && nombre.isNotBlank()) {
                        fields[OcrFieldKeys.SenderNombre] = nombre
                        confidence[OcrFieldKeys.SenderNombre] = 0.6f
                    }
                    if (!fields.containsKey(OcrFieldKeys.SenderApellido) && apellido.isNotBlank()) {
                        fields[OcrFieldKeys.SenderApellido] = apellido
                        confidence[OcrFieldKeys.SenderApellido] = 0.6f
                    }
                }
            }

            if (!fields.containsKey(OcrFieldKeys.DestNombre) || !fields.containsKey(OcrFieldKeys.DestApellido)) {
                val destinatarioMatch = Regex("(?i)destinatario\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÑáéíóúñ]+(?:\\s+[A-Za-zÁÉÍÓÚÑáéíóúñ]+)*)")
                    .find(raw)
                if (destinatarioMatch != null) {
                    val (nombre, apellido) = splitPersonName(destinatarioMatch.groupValues[1])
                    if (!fields.containsKey(OcrFieldKeys.DestNombre) && nombre.isNotBlank()) {
                        fields[OcrFieldKeys.DestNombre] = nombre
                        confidence[OcrFieldKeys.DestNombre] = 0.6f
                    }
                    if (!fields.containsKey(OcrFieldKeys.DestApellido) && apellido.isNotBlank()) {
                        fields[OcrFieldKeys.DestApellido] = apellido
                        confidence[OcrFieldKeys.DestApellido] = 0.6f
                    }
                }
            }

            if (!fields.containsKey(OcrFieldKeys.DestDireccion)) {
                val direccionMatch = Regex("(?i)direcci[oó]n\\s*(?:destinatario|cliente)?\\s*[:\\-]?\\s*(.+)")
                    .find(raw)
                if (direccionMatch != null) {
                    fields[OcrFieldKeys.DestDireccion] = direccionMatch.groupValues[1].trim()
                    confidence[OcrFieldKeys.DestDireccion] = 0.6f
                }
            }

            if (!fields.containsKey(OcrFieldKeys.DestTelefono)) {
                val telefonoMatch = Regex("(?i)tel[eé]fono\\s*(?:destinatario|cliente)?\\s*[:\\-]?\\s*([+\\d][\\d\\s-]+)")
                    .find(raw)
                if (telefonoMatch != null) {
                    fields[OcrFieldKeys.DestTelefono] = telefonoMatch.groupValues[1].replace(" ", "").trim()
                    confidence[OcrFieldKeys.DestTelefono] = 0.6f
                }
            }

            return fields to confidence
        }

        private data class LabeledValue(
            val value: String,
            val confidence: Float,
        )

        private fun findLabeledValue(
            lines: List<String>,
            labels: List<String>,
            knownLabels: List<String>,
            valueValidator: (String) -> Boolean = { true },
        ): LabeledValue? {
            if (labels.isEmpty()) return null
            val labelRegex = labels
                .sortedByDescending { it.length }
                .joinToString("|") { labelPattern(it) }
            val pattern = Regex(
                "^\\s*(?:$labelRegex)\\b(?:\\s*(?:n\\s*[°o]|nro\\.?|no\\.?|num\\.?|n°))?\\s*[:\\-]?\\s*(.*)$",
                RegexOption.IGNORE_CASE
            )
            val knownLabelRegex = if (knownLabels.isEmpty()) {
                null
            } else {
                val knownPattern = knownLabels
                    .sortedByDescending { it.length }
                    .joinToString("|") { labelPattern(it) }
                Regex("^\\s*(?:$knownPattern)\\b.*$", RegexOption.IGNORE_CASE)
            }
            for (index in lines.indices) {
                val line = lines[index].trim()
                if (line.isBlank()) continue
                val match = pattern.find(line) ?: continue
                val inlineValue = match.groupValues[1].trim()
                if (inlineValue.isNotBlank()) {
                    val cleanedValue = cleanValue(inlineValue)
                    if (!valueValidator(cleanedValue)) continue
                    return LabeledValue(cleanedValue, 0.75f)
                }
                val nextValue = nextNonEmptyLine(lines, index + 1, knownLabelRegex)
                if (!nextValue.isNullOrBlank()) {
                    val cleanedValue = cleanValue(nextValue)
                    if (!valueValidator(cleanedValue)) continue
                    return LabeledValue(cleanedValue, 0.65f)
                }
            }
            return null
        }

        private fun labelPattern(label: String): String {
            return label.split(" ")
                .filter { it.isNotBlank() }
                .joinToString("\\s+") { Regex.escape(it) }
        }

        private fun nextNonEmptyLine(
            lines: List<String>,
            startIndex: Int,
            knownLabelRegex: Regex?,
        ): String? {
            val end = (startIndex + 2).coerceAtMost(lines.size)
            for (i in startIndex until end) {
                val candidate = lines[i].trim()
                if (candidate.isBlank()) continue
                if (candidate.endsWith(":")) continue
                if (knownLabelRegex?.containsMatchIn(candidate) == true) continue
                return candidate
            }
            return null
        }

        private fun splitPersonName(value: String): Pair<String, String> {
            val cleaned = value.replace("[,:;]".toRegex(), " ").trim()
            val parts = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (parts.isEmpty()) return "" to ""
            if (parts.size == 1) return parts.first() to ""
            val nombre = parts.first()
            val apellido = parts.drop(1).joinToString(" ")
            return nombre to apellido
        }

        private fun cleanValue(value: String): String {
            return value.replace("\t", " ").trim()
        }

        internal fun buildParsingErrorSummary(fields: Map<String, String>): String? {
            val missing = mutableListOf<String>()
            if (!fields.containsKey(OcrFieldKeys.SenderCuit)) missing.add("CUIT")
            if (!fields.containsKey(OcrFieldKeys.SenderNombre) || !fields.containsKey(OcrFieldKeys.SenderApellido)) {
                missing.add("Remitente")
            }
            if (!fields.containsKey(OcrFieldKeys.DestNombre) || !fields.containsKey(OcrFieldKeys.DestApellido)) {
                missing.add("Destinatario")
            }
            if (!fields.containsKey(OcrFieldKeys.DestDireccion)) missing.add("Dirección")
            if (!fields.containsKey(OcrFieldKeys.DestTelefono)) missing.add("Teléfono")
            if (!fields.containsKey(OcrFieldKeys.CantBultosTotal)) missing.add("Cantidad de bultos")
            if (!fields.containsKey(OcrFieldKeys.RemitoNumCliente)) missing.add("Número de remito de cliente")

            if (missing.isEmpty()) return null
            return "Faltan: ${missing.joinToString(", ")}"
        }
    }
}
