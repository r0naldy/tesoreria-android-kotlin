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
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import pe.edu.hzg.tesoreria.features.treasury.domain.model.IncomeReport
import pe.edu.hzg.tesoreria.features.treasury.domain.dashboardCsv

/** Salidas iniciadas por el usuario. El CSV se guarda solo en la cache privada compartible. */
actual object DashboardOutput {
    actual fun share(context: Context, summary: String) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Resumen del dashboard")
                    putExtra(Intent.EXTRA_TEXT, summary)
                },
                "Compartir resumen",
            ),
        )
    }

    actual fun exportCsv(context: Context, report: IncomeReport) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        cleanup(directory)
        val range = "${report.desde.orEmpty()}_${report.hasta.orEmpty()}"
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .ifBlank { "consulta" }
        val file = File(directory, "dashboard_$range.csv")
        file.writeText("\uFEFF${dashboardCsv(report)}", Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte filtrado del dashboard")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Exportar dashboard CSV",
            ),
        )
    }

    actual fun print(context: Context, summary: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins(400, 400, 400, 400))
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        manager.print("Resumen del dashboard", DashboardSummaryPrintAdapter(context, summary), attributes)
    }

    private fun cleanup(directory: File) {
        val expiry = System.currentTimeMillis() - 24L * 60L * 60L * 1_000L
        directory.listFiles()?.filter { it.isFile && it.lastModified() < expiry }?.forEach { it.delete() }
    }
}

private class DashboardSummaryPrintAdapter(
    private val context: Context,
    private val summary: String,
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
            PrintDocumentInfo.Builder("resumen-dashboard.pdf")
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
            val rect = page.info.contentRect
            val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                textSize = 24f
            }
            val bold = Paint(normal).apply { typeface = Typeface.DEFAULT_BOLD }
            var y = rect.top + 30f
            summary.lineSequence().forEachIndexed { index, source ->
                val paint = if (index == 0 || source == source.uppercase(Locale.ROOT) && source.isNotBlank()) bold else normal
                wrap(source, paint, rect.width().toFloat()).forEach { line ->
                    page.canvas.drawText(line, rect.left.toFloat(), y, paint)
                    y += 34f
                }
                if (source.isBlank()) y += 8f
            }
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            callback.onWriteFailed(error.message ?: "No se pudo crear el resumen")
        } finally {
            document.close()
        }
    }

    private fun wrap(text: String, paint: Paint, width: Float): List<String> {
        if (text.isBlank() || paint.measureText(text) <= width) return listOf(text)
        val result = mutableListOf<String>()
        var current = ""
        text.split(' ').forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= width) current = candidate
            else {
                if (current.isNotBlank()) result += current
                current = word
            }
        }
        if (current.isNotBlank()) result += current
        return result
    }
}
