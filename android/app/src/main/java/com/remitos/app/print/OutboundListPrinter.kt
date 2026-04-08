package com.remitos.app.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.remitos.app.data.TemplateConfig
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OutboundListPrinter(private val context: Context) {
    
    fun print(list: OutboundListEntity, lines: List<OutboundLineWithRemito>, config: TemplateConfig = TemplateConfig()) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Lista ${list.listNumber}"
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, OutboundListPrintAdapter(context, list, lines, config), attributes)
    }
    
    fun saveToPdf(list: OutboundListEntity, lines: List<OutboundLineWithRemito>, config: TemplateConfig = TemplateConfig()): File? {
        return try {
            val pdfDoc = PrintedPdfDocument(context, PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build())
            
            val page = pdfDoc.startPage(0)
            drawPage(context, page.canvas, list, lines, config)
            pdfDoc.finishPage(page)
            
            val pdfDir = File(context.getExternalFilesDir(null), "remitos")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "lista_${list.listNumber}_$timestamp.pdf"
            val pdfFile = File(pdfDir, fileName)
            
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
            
            pdfDoc.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private class OutboundListPrintAdapter(
    private val context: Context,
    private val list: OutboundListEntity,
    private val lines: List<OutboundLineWithRemito>,
    private val config: TemplateConfig
) : PrintDocumentAdapter() {
    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        pdfDocument = PrintedPdfDocument(context, attributes)
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("lista_${list.listNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = pdfDocument ?: return callback.onWriteFailed("Documento no disponible")
        val page = document.startPage(0)
        drawPage(context, page.canvas, list, lines, config)
        document.finishPage(page)

        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }

        FileOutputStream(destination.fileDescriptor).use { out ->
            document.writeTo(out)
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
    }
}

private data class Column(
    val title: String,
    val width: Float,
)

/**
 * Wraps text into multiple lines based on available width
 * Returns list of lines (max 4 lines by default)
 */
private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 4): List<String> {
    if (text.isEmpty()) return listOf("")
    
    // If single line fits, return early
    if (paint.measureText(text) <= maxWidth) {
        return listOf(text)
    }
    
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""
    
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(testLine) <= maxWidth) {
            currentLine = testLine
        } else {
            // Word doesn't fit, save current line and start new one
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
                // Check if we've reached max lines
                if (lines.size >= maxLines) {
                    // Truncate the last line with ellipsis if needed
                    if (paint.measureText(lines.last()) > maxWidth - 10f) {
                        lines[lines.size - 1] = truncateWithEllipsis(lines.last(), paint, maxWidth)
                    }
                    return lines
                }
            }
            currentLine = word
        }
    }
    
    // Add remaining text
    if (currentLine.isNotEmpty() && lines.size < maxLines) {
        lines.add(currentLine)
    }
    
    return lines
}

/**
 * Truncates text with ellipsis to fit within maxWidth
 */
private fun truncateWithEllipsis(text: String, paint: Paint, maxWidth: Float): String {
    val ellipsis = "..."
    if (paint.measureText(ellipsis) > maxWidth) return ellipsis
    
    var low = 0
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        val test = text.substring(0, mid) + ellipsis
        if (paint.measureText(test) <= maxWidth) {
            low = mid
        } else {
            high = mid - 1
        }
    }
    return if (low > 0) text.substring(0, low) + ellipsis else ellipsis
}

/**
 * Calculates the required height for a cell based on wrapped text
 */
private fun calculateCellHeight(
    text: String, 
    paint: Paint, 
    maxWidth: Float, 
    lineHeight: Float,
    maxLines: Int = 4
): Float {
    val lines = wrapText(text, paint, maxWidth, maxLines)
    return lines.size * lineHeight
}

private fun drawPage(context: Context, canvas: Canvas, list: OutboundListEntity, lines: List<OutboundLineWithRemito>, config: TemplateConfig) {
    val paint = Paint().apply { textSize = 12f; color = android.graphics.Color.BLACK }
    val boldPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
    val linePaint = Paint().apply {
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
        color = android.graphics.Color.BLACK
    }
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))
    val issueDate = formatter.format(Date(list.issueDate))
    var y = 26f
    val left = 24f
    val right = 818f

    // Draw Logo if available
    config.logoUri?.let { uriString ->
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                // Scale logo to fit in a 120x60 box at the top left
                val targetWidth = 120f
                val targetHeight = 60f
                val scale = minOf(targetWidth / bitmap.width, targetHeight / bitmap.height)
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val rect = RectF(left, y, left + scaledWidth, y + scaledHeight)
                canvas.drawBitmap(bitmap, null, rect, paint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val titleLeft = left + 160f
    val titleRight = right
    val titleTop = y - 10f
    val titleHeight = 26f
    canvas.drawRect(left, titleTop, titleLeft, titleTop + titleHeight * 2, linePaint)
    canvas.drawRect(titleLeft, titleTop, titleRight, titleTop + titleHeight * 2, linePaint)
    val titleCenter = (left + right) / 2f
    canvas.drawText("Reparto", titleCenter - 26f, titleTop + 17f, boldPaint)
    canvas.drawText("Nro. de Documento:", titleRight - 190f, titleTop + 17f, paint)
    canvas.drawText(list.listNumber.toString(), titleRight - 70f, titleTop + 17f, paint)

    val issueTop = titleTop + titleHeight
    canvas.drawRect(titleLeft, issueTop, titleRight, issueTop + titleHeight, linePaint)
    canvas.drawText("Fecha De Emision:", titleLeft + 10f, issueTop + 17f, paint)
    canvas.drawText(issueDate, titleLeft + 120f, issueTop + 17f, paint)

    val headerRowTop = issueTop + titleHeight
    val fullHeaderWidth = right - left
    val choferLabelWidth = 64f
    val patenteLabelWidth = 74f
    val patenteValueWidth = 180f
    val choferValueWidth = fullHeaderWidth - choferLabelWidth - patenteLabelWidth - patenteValueWidth
    drawHeaderField(
        canvas,
        "Chofer",
        "${list.driverApellido} ${list.driverNombre}",
        left,
        headerRowTop,
        choferLabelWidth,
        choferValueWidth,
        titleHeight,
        linePaint,
        paint,
        boldPaint,
    )
    drawHeaderField(
        canvas,
        "Patente",
        "",
        left + choferLabelWidth + choferValueWidth,
        headerRowTop,
        patenteLabelWidth,
        patenteValueWidth,
        titleHeight,
        linePaint,
        paint,
        boldPaint,
    )

    val tableTop = headerRowTop + titleHeight + 8f
    
    // Column configuration with wrapping flags
    val wrappingConfig = mapOf(
        "Destinatario" to true,
        "Direccion" to true, 
        "Observaciones" to true
    )
    
    // Wrapping configuration
    val lineHeightForWrapping = 14f
    val maxLinesPerCell = 4
    val minRowHeight = 20f
    
    val allColumns = mutableListOf(
        Column("Nro. Cliente", 98f),
        Column("Nro. Interno", 79f),
        Column("Destinatario", 118f),
        Column("Direccion", 150f),
        Column("Localidad", 80f),
        Column("Bultos", 47f)
    )
    if (config.showPeso) allColumns.add(Column("Peso", 47f))
    if (config.showVolumen) allColumns.add(Column("Volumen", 68f))
    if (config.showObservaciones) allColumns.add(Column("Observaciones", 110f))
    
    val columns = allColumns.toList()
    val availableWidth = right - left
    val baseWidth = columns.sumOf { it.width.toDouble() }.toFloat()
    val scale = if (baseWidth > 0f) availableWidth / baseWidth else 1f
    val scaledColumns = columns.map { col ->
        col.copy(width = col.width * scale)
    }
    val tableRight = right

    // Draw header row
    canvas.drawRect(left, tableTop, tableRight, tableTop + minRowHeight, linePaint)
    var x = left
    scaledColumns.forEach { col ->
        canvas.drawRect(x, tableTop, x + col.width, tableTop + minRowHeight, linePaint)
        canvas.drawText(col.title, x + 4f, tableTop + 14f, boldPaint)
        x += col.width
    }

    // Calculate row heights before drawing
    val rowHeights = mutableListOf<Float>()
    
    lines.forEach { line ->
        // Prepare values for this row
        val values = mutableListOf(
            line.remitoNumCliente,
            line.remitoNumInterno ?: "",
            "${line.recipientApellido} ${line.recipientNombre}",
            line.recipientDireccion,
            "",
            line.packageQty.toString()
        )
        if (config.showPeso) values.add("")
        if (config.showVolumen) values.add("")
        if (config.showObservaciones) values.add("")
        
        // Calculate max height needed for this row
        var maxCellHeight = minRowHeight
        
        values.forEachIndexed { index, value ->
            val columnTitle = scaledColumns[index].title
            if (wrappingConfig[columnTitle] == true) {
                // This column wraps - calculate height
                val colWidth = scaledColumns[index].width
                val textWidth = colWidth - 6f // 3px padding each side
                val cellHeight = calculateCellHeight(
                    value, 
                    paint, 
                    textWidth, 
                    lineHeightForWrapping, 
                    maxLinesPerCell
                )
                maxCellHeight = maxOf(maxCellHeight, cellHeight + 6f) // Add padding
            }
        }
        
        rowHeights.add(maxCellHeight)
    }
    
    // Calculate how many rows fit on the page
    val signatureBoxHeight = 28f
    val signatureGap = 28f
    val tableBottomLimit = 560f - signatureBoxHeight - signatureGap
    val footerSpace = 120f // Space needed for footer elements
    
    // Find how many rows fit
    var rowTop = tableTop + minRowHeight
    var rowsDrawn = 0
    var cumulativeHeight = 0f
    for (i in rowHeights.indices) {
        if (rowTop + cumulativeHeight + rowHeights[i] > tableBottomLimit - footerSpace) {
            break
        }
        cumulativeHeight += rowHeights[i]
        rowsDrawn++
    }
    
    // Draw rows with variable heights
    rowTop = tableTop + minRowHeight
    lines.take(rowsDrawn).forEachIndexed { rowIndex, line ->
        val currentRowHeight = rowHeights[rowIndex]
        
        var x = left
        canvas.drawRect(left, rowTop, tableRight, rowTop + currentRowHeight, linePaint)
        
        val values = mutableListOf(
            line.remitoNumCliente,
            line.remitoNumInterno ?: "",
            "${line.recipientApellido} ${line.recipientNombre}",
            line.recipientDireccion,
            "",
            line.packageQty.toString()
        )
        if (config.showPeso) values.add("")
        if (config.showVolumen) values.add("")
        if (config.showObservaciones) values.add("")

        values.forEachIndexed { index, value ->
            val col = scaledColumns[index]
            val columnTitle = col.title
            val shouldWrap = wrappingConfig[columnTitle] == true
            
            canvas.drawRect(x, rowTop, x + col.width, rowTop + currentRowHeight, linePaint)
            
            if (shouldWrap) {
                // Draw wrapped text
                val textWidth = col.width - 6f
                val wrappedLines = wrapText(value, paint, textWidth, maxLinesPerCell)
                
                wrappedLines.forEachIndexed { lineIndex, lineText ->
                    val yPosition = rowTop + 6f + (lineIndex * lineHeightForWrapping) + (lineHeightForWrapping / 2)
                    canvas.drawText(lineText, x + 3f, yPosition, paint)
                }
            } else {
                // Draw single line (centered vertically)
                val yPosition = rowTop + (currentRowHeight / 2) + 6f
                canvas.drawText(value, x + 3f, yPosition, paint)
            }
            
            x += col.width
        }
        rowTop += currentRowHeight
    }

    // Position signature box based on actual table end
    val signatureTop = rowTop + signatureGap
    val signatureBoxWidth = 160f
    canvas.drawText("Firma:", right - signatureBoxWidth - 60f, signatureTop, paint)
    canvas.drawRect(
        right - signatureBoxWidth,
        signatureTop - 14f,
        right,
        signatureTop - 14f + signatureBoxHeight,
        linePaint
    )

    val aclaracionTop = signatureTop + 36f
    canvas.drawText("Aclaración:", right - 200f, aclaracionTop, paint)
    canvas.drawLine(right - 130f, aclaracionTop + 6f, right, aclaracionTop + 6f, linePaint)

    val totalsTop = aclaracionTop + 22f
    canvas.drawRect(left, totalsTop - 14f, left + 70f, totalsTop + 6f, linePaint)
    canvas.drawText("Totales", left + 8f, totalsTop, boldPaint)

    val totalBultos = lines.take(rowsDrawn).sumOf { it.packageQty }
    val totalPedidos = rowsDrawn
    canvas.drawText("Cantidad de Bultos: $totalBultos", left, totalsTop + 26f, paint)
    canvas.drawText("Cantidad de Pedidos: $totalPedidos", left + 180f, totalsTop + 26f, paint)
    if (config.showVolumen) canvas.drawText("Volumen Total M³:", left + 380f, totalsTop + 26f, paint)
    if (config.showPeso) canvas.drawText("Peso Total Kgs:", left + 560f, totalsTop + 26f, paint)
    
    // Draw continuation indicator if list was truncated
    if (rowsDrawn < lines.size) {
        val remainingCount = lines.size - rowsDrawn
        val continuationY = totalsTop + 50f
        val warningPaint = Paint(paint).apply { 
            textSize = 10f
            isFakeBoldText = true
            color = android.graphics.Color.RED
        }
        canvas.drawText("... y $remainingCount pedido${if (remainingCount > 1) "s" else ""} más", left, continuationY, warningPaint)
    }
    
    // Draw legal text at the bottom
    if (config.legalText.isNotBlank()) {
        val legalTop = totalsTop + (if (rowsDrawn < lines.size) 60f else 40f)
        val legalPaint = Paint(paint).apply { 
            textSize = 9f
            color = android.graphics.Color.DKGRAY 
        }
        
        // Simple word wrap
        val words = config.legalText.split(" ")
        var currentLine = ""
        var lineY = legalTop
        val maxWidth = right - left
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = legalPaint.measureText(testLine)
            if (width > maxWidth) {
                canvas.drawText(currentLine, left, lineY, legalPaint)
                currentLine = word
                lineY += 12f
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, left, lineY, legalPaint)
        }
    }
}

private fun drawHeaderField(
    canvas: Canvas,
    label: String,
    value: String,
    left: Float,
    top: Float,
    labelWidth: Float,
    valueWidth: Float,
    height: Float,
    linePaint: Paint,
    paint: Paint,
    boldPaint: Paint,
) {
    canvas.drawRect(left, top, left + labelWidth, top + height, linePaint)
    canvas.drawRect(left + labelWidth, top, left + labelWidth + valueWidth, top + height, linePaint)
    canvas.drawText(label, left + 4f, top + 15f, boldPaint)
    if (value.isNotBlank()) {
        canvas.drawText(value, left + labelWidth + 4f, top + 15f, paint)
    }
}
