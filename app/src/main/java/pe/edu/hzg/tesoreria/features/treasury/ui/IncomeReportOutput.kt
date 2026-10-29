package pe.edu.hzg.tesoreria.features.treasury.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import pe.edu.hzg.tesoreria.features.treasury.data.IncomeReportDto
import pe.edu.hzg.tesoreria.features.treasury.data.ReportSummaryDto

/** Exportación iniciada expresamente por el usuario; los archivos permanecen en caché privada. */
object IncomeReportOutput {
    fun shareSummary(context: Context, report: IncomeReportDto) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de ingresos")
                    putExtra(Intent.EXTRA_TEXT, report.asSummaryText())
                },
                "Compartir reporte de ingresos",
            ),
        )
    }

    fun exportCsv(context: Context, report: IncomeReportDto) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        cleanup(directory)
        val range = "${report.desde.orEmpty()}_${report.hasta.orEmpty()}"
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .ifBlank { "consulta" }
        val file = File(directory, "reporte_ingresos_$range.csv")
        file.writeText("\uFEFF${report.asCsv()}", Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de ingresos")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Exportar reporte CSV",
            ),
        )
    }

    private fun IncomeReportDto.asSummaryText(): String = buildString {
        appendLine("REPORTE DE INGRESOS")
        appendLine("Desde: ${desde?.let(::shortDate) ?: "Todos"}")
        appendLine("Hasta: ${hasta?.let(::shortDate) ?: "Hoy"}")
        appendLine("Tickets: $cantidadTickets")
        appendLine("Servicios cobrados: $cantidadServicios")
        appendLine("Total: ${money(total)}")
        appendSummary("Resumen por servicio", resumenPorServicio)
        appendSummary("Resumen por programa", resumenPorPrograma)
    }

    private fun StringBuilder.appendSummary(title: String, values: List<ReportSummaryDto>) {
        if (values.isEmpty()) return
        appendLine()
        appendLine(title.uppercase())
        values.forEach { appendLine("• ${it.nombre}: ${it.cantidad} · ${money(it.monto)}") }
    }

    private fun IncomeReportDto.asCsv(): String = buildList {
        add(csvRow("Reporte de ingresos"))
        add(csvRow("Desde", desde.orEmpty()))
        add(csvRow("Hasta", hasta.orEmpty()))
        add(csvRow("Cantidad de tickets", cantidadTickets.toString()))
        add(csvRow("Cantidad de servicios", cantidadServicios.toString()))
        add(csvRow("Total", "%.2f".format(total)))
        add("")
        add(csvRow("DETALLE"))
        add(csvRow("Fecha", "Ticket", "Contador", "DNI", "Alumno", "Programa", "Servicio", "Importe", "Usuario"))
        items.forEach { item ->
            add(
                csvRow(
                    item.fecha,
                    item.numeroTicket,
                    item.contador,
                    item.dni,
                    item.alumno,
                    item.programa,
                    item.servicio,
                    "%.2f".format(item.importe),
                    item.usuario,
                ),
            )
        }
        add("")
        add(csvRow("RESUMEN POR SERVICIO"))
        add(csvRow("Nombre", "Cantidad", "Monto"))
        resumenPorServicio.forEach { add(csvRow(it.nombre, it.cantidad.toString(), "%.2f".format(it.monto))) }
        add("")
        add(csvRow("RESUMEN POR PROGRAMA"))
        add(csvRow("Nombre", "Cantidad", "Monto"))
        resumenPorPrograma.forEach { add(csvRow(it.nombre, it.cantidad.toString(), "%.2f".format(it.monto))) }
    }.joinToString("\r\n")

    private fun csvRow(vararg cells: String): String = cells.joinToString(",") { cell ->
        "\"${cell.replace("\"", "\"\"")}\""
    }

    private fun cleanup(directory: File) {
        val expiry = System.currentTimeMillis() - 24L * 60L * 60L * 1_000L
        directory.listFiles()?.filter { it.isFile && it.lastModified() < expiry }?.forEach { it.delete() }
    }
}
