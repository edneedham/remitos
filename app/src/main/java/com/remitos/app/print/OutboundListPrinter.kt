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
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OutboundListPrinter(private val context: Context) {
    fun print(list: OutboundListEntity, lines: List<OutboundLineEntity>) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Lista ${list.listNumber}"
        printManager.print(jobName, OutboundListPrintAdapter(context, list, lines), null)
    }
}

private class OutboundListPrintAdapter(
    private val context: Context,
    private val list: OutboundListEntity,
    private val lines: List<OutboundLineEntity>
) : PrintDocumentAdapter() {
    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
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
        drawPage(page.canvas)
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

    private fun drawPage(canvas: Canvas) {
        val paint = Paint().apply { textSize = 14f }
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))
        val issueDate = formatter.format(Date(list.issueDate))
        var y = 40f
        canvas.drawText("Lista Nº ${list.listNumber}", 40f, y, paint)
        y += 20f
        canvas.drawText("Fecha de emisión: $issueDate", 40f, y, paint)
        y += 20f
        canvas.drawText("Chofer: ${list.driverNombre} ${list.driverApellido}", 40f, y, paint)
        y += 30f

        canvas.drawText("Nº Entrega | Destinatario | Dirección | Teléfono | Bultos", 40f, y, paint)
        y += 20f

        lines.forEach { line ->
            val row = "${line.deliveryNumber} | ${line.recipientNombre} ${line.recipientApellido} | ${line.recipientDireccion} | ${line.recipientTelefono} | ${line.packageQty}"
            canvas.drawText(row, 40f, y, paint)
            y += 18f
            if (y > 760f) return
        }
    }
}
