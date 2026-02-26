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
import com.remitos.app.data.db.entity.OutboundLineWithRemito
import com.remitos.app.data.db.entity.OutboundListEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OutboundListPrinter(private val context: Context) {
    
    fun print(list: OutboundListEntity, lines: List<OutboundLineWithRemito>) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Lista ${list.listNumber}"
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, OutboundListPrintAdapter(context, list, lines), attributes)
    }
    
    fun saveToPdf(list: OutboundListEntity, lines: List<OutboundLineWithRemito>): File? {
        return try {
            val pdfDoc = PrintedPdfDocument(context, PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build())
            
            val page = pdfDoc.startPage(0)
            drawPage(page.canvas, list, lines)
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
    private val lines: List<OutboundLineWithRemito>
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
        drawPage(page.canvas, list, lines)
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

private fun drawPage(canvas: Canvas, list: OutboundListEntity, lines: List<OutboundLineWithRemito>) {
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
    val rowHeight = 20f

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
    val columns = listOf(
        Column("Nro. Cliente", 76f),
        Column("Nro. Interno", 76f),
        Column("Destinatario", 118f),
        Column("Direccion", 150f),
        Column("Localidad", 80f),
        Column("Bultos", 54f),
        Column("Peso", 54f),
        Column("Volumen", 68f),
        Column("Observaciones", 110f),
    )
    val availableWidth = right - left
    val baseWidth = columns.sumOf { it.width.toDouble() }.toFloat()
    val scale = if (baseWidth > 0f) availableWidth / baseWidth else 1f
    val scaledColumns = columns.map { col ->
        col.copy(width = col.width * scale)
    }
    val tableRight = right

    canvas.drawRect(left, tableTop, tableRight, tableTop + rowHeight, linePaint)
    var x = left
    scaledColumns.forEach { col ->
        canvas.drawRect(x, tableTop, x + col.width, tableTop + rowHeight, linePaint)
        canvas.drawText(col.title, x + 4f, tableTop + 14f, boldPaint)
        x += col.width
    }

    var rowTop = tableTop + rowHeight
    val signatureBoxHeight = 28f
    val signatureGap = 18f
    val tableBottomLimit = 560f - signatureBoxHeight - signatureGap
    val maxRows = ((tableBottomLimit - rowTop) / rowHeight).toInt().coerceAtLeast(8)
    lines.take(maxRows).forEach { line ->
        x = left
        canvas.drawRect(left, rowTop, tableRight, rowTop + rowHeight, linePaint)
        val values = listOf(
            line.remitoNumCliente,
            line.remitoNumInterno,
            "${line.recipientApellido} ${line.recipientNombre}",
            line.recipientDireccion,
            "",
            line.packageQty.toString(),
            "",
            "",
            "",
        )
        values.forEachIndexed { index, value ->
            val col = scaledColumns[index]
            canvas.drawRect(x, rowTop, x + col.width, rowTop + rowHeight, linePaint)
            canvas.drawText(value, x + 3f, rowTop + 14f, paint)
            x += col.width
        }
        rowTop += rowHeight
    }

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

    val aclaracionTop = signatureTop + 26f
    canvas.drawText("Aclaración:", right - 200f, aclaracionTop, paint)
    canvas.drawLine(right - 130f, aclaracionTop + 6f, right, aclaracionTop + 6f, linePaint)

    val totalsTop = aclaracionTop + 22f
    canvas.drawRect(left, totalsTop - 14f, left + 70f, totalsTop + 6f, linePaint)
    canvas.drawText("Totales", left + 8f, totalsTop, boldPaint)

    val totalBultos = lines.sumOf { it.packageQty }
    val totalPedidos = lines.size
    canvas.drawText("Cantidad de Bultos: $totalBultos", left, totalsTop + 26f, paint)
    canvas.drawText("Cantidad de Pedidos: $totalPedidos", left + 180f, totalsTop + 26f, paint)
    canvas.drawText("Volument Total M³:", left + 380f, totalsTop + 26f, paint)
    canvas.drawText("Peso Total Kgs:", left + 560f, totalsTop + 26f, paint)
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
