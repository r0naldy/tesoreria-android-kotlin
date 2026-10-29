package pe.edu.hzg.tesoreria.features.treasury.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import pe.edu.hzg.tesoreria.features.treasury.data.InstitutionConfigDto
import pe.edu.hzg.tesoreria.features.treasury.data.TicketDto

object TicketOutput {
    fun share(context: Context, ticket: TicketDto) {
        val text = buildString {
            appendLine("Ticket ${ticket.numeroTicket}")
            appendLine("${ticket.alumnoNombre} · DNI ${ticket.dni}")
            appendLine("${ticket.programa} · ${ticket.turno}")
            appendLine()
            ticket.detalles.forEach { appendLine("${it.servicio}: ${money(it.importe)}") }
            appendLine("TOTAL: ${money(ticket.total)}")
            appendLine("SON: ${ticket.totalEnLetras}")
            appendLine("Emitido por ${ticket.usuario} · ${shortDate(ticket.fechaEmision)}")
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Ticket ${ticket.numeroTicket}")
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Compartir ticket",
            ),
        )
    }

    fun print(context: Context, ticket: TicketDto, config: InstitutionConfigDto? = null) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize("THERMAL_80", "Ticket 80 mm", 3150, 9000))
            .setMinMargins(PrintAttributes.Margins(80, 80, 80, 80))
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        manager.print("Ticket ${ticket.numeroTicket}", ThermalTicketAdapter(context, ticket, config), attributes)
    }
}

private class ThermalTicketAdapter(
    private val context: Context,
    private val ticket: TicketDto,
    private val config: InstitutionConfigDto?,
) : PrintDocumentAdapter() {
    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("ticket-${ticket.numeroTicket.replace('/', '-')}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        val document = PrintedPdfDocument(context, attributes ?: PrintAttributes.Builder().build())
        try {
            if (cancellationSignal.isCanceled) return callback.onWriteCancelled()
            val page = document.startPage(0)
            drawTicket(page.canvas, page.info.contentRect.left.toFloat(), page.info.contentRect.top.toFloat(), page.info.contentRect.width().toFloat())
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            callback.onWriteFailed(error.message ?: "No se pudo crear el ticket")
        } finally {
            document.close()
        }
    }

    private fun drawTicket(canvas: android.graphics.Canvas, left: Float, top: Float, width: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 25f }
        val bold = Paint(paint).apply { typeface = Typeface.DEFAULT_BOLD }
        var y = top + 30f
        fun center(text: String, p: Paint = paint) {
            canvas.drawText(text, left + (width - p.measureText(text)) / 2f, y, p); y += 34f
        }
        fun line(text: String, p: Paint = paint) {
            wrap(text, p, width).forEach { canvas.drawText(it, left, y, p); y += 31f }
        }
        fun rule() { canvas.drawLine(left, y, left + width, y, paint); y += 22f }

        center(config?.nombreInstitucion ?: "TESORERIA", bold)
        center(config?.ciudad ?: "JULIACA")
        rule()
        center(config?.tituloComprobante ?: "INGRESO POR RECURSOS PROPIOS", bold)
        rule()
        line("Ticket N°: ${ticket.numeroTicket}")
        line("Contador: ${ticket.contador}")
        line("Fecha: ${shortDate(ticket.fechaEmision)}")
        line("DNI: ${ticket.dni}")
        line("Alumno: ${ticket.alumnoNombre}")
        line("Programa: ${ticket.programa}")
        line("Turno: ${ticket.turno}")
        rule()
        ticket.detalles.forEach { detail ->
            line(detail.servicio, bold)
            line("  ${money(detail.importe)}")
        }
        rule()
        line("TOTAL: ${money(ticket.total)}", bold)
        line("SON: ${ticket.totalEnLetras}")
        rule()
        center("Usuario: ${ticket.usuario}")
        y += 70f
        center("________________________")
        center("Firma del Tesorero")
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val result = mutableListOf<String>()
        var current = ""
        text.split(' ').forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) current = candidate
            else { if (current.isNotBlank()) result += current; current = word }
        }
        if (current.isNotBlank()) result += current
        return result.ifEmpty { listOf(text.take(40)) }
    }
}
