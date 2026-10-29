package pe.edu.hzg.tesoreria.features.treasury.data

import java.util.Locale

/** Filtros que el dashboard web aplica sobre el reporte real obtenido por rango de fechas. */
data class DashboardReportFilter(
    val program: String = "",
    val service: String = "",
    val user: String = "",
)

/**
 * El API de reportes solo admite desde/hasta. Programa, servicio y usuario se filtran en el
 * dispositivo sobre las lineas confirmadas por el servidor; no se inventan query parameters.
 */
fun IncomeReportDto.filtered(filter: DashboardReportFilter): IncomeReportDto {
    val filteredItems = items.filter { item ->
        (filter.program.isBlank() || item.programa == filter.program) &&
            (filter.service.isBlank() || item.servicio == filter.service) &&
            (filter.user.isBlank() || item.usuario == filter.user)
    }
    return copy(
        cantidadTickets = filteredItems.distinctBy { it.numeroTicket }.size,
        cantidadServicios = filteredItems.size,
        total = filteredItems.sumOf { it.importe },
        items = filteredItems,
        resumenPorServicio = filteredItems.summaryBy { it.servicio },
        resumenPorPrograma = filteredItems.summaryBy { it.programa },
    )
}

fun dashboardCsv(report: IncomeReportDto): String = buildList {
    add(csvRow("REPORTE DEL DASHBOARD"))
    add(csvRow("Desde", report.desde.orEmpty()))
    add(csvRow("Hasta", report.hasta.orEmpty()))
    add(csvRow("Tickets", report.cantidadTickets.toString()))
    add(csvRow("Servicios cobrados", report.cantidadServicios.toString()))
    add(csvRow("Total", report.total.fixedAmount()))
    add("")
    add(csvRow("Fecha", "Ticket", "Contador", "DNI", "Estudiante", "Programa", "Concepto", "Importe", "Usuario"))
    report.items.forEach { item ->
        add(
            csvRow(
                item.fecha,
                item.numeroTicket,
                item.contador,
                item.dni,
                item.alumno,
                item.programa,
                item.servicio,
                item.importe.fixedAmount(),
                item.usuario,
            ),
        )
    }
}.joinToString("\r\n")

fun dashboardSummary(
    dashboard: TreasuryDashboardDto,
    period: DashboardPeriodDto?,
    report: IncomeReportDto,
): String = buildString {
    appendLine("RESUMEN DEL DASHBOARD")
    appendLine("Rango: ${report.desde.orEmpty()} al ${report.hasta.orEmpty()}")
    appendLine("Total filtrado: S/ ${report.total.fixedAmount()}")
    appendLine("Tickets filtrados: ${report.cantidadTickets}")
    appendLine("Servicios cobrados: ${report.cantidadServicios}")
    appendLine()
    appendLine("Hoy: S/ ${dashboard.totalRecaudadoHoy.fixedAmount()} · ${dashboard.ticketsHoy} ticket(s)")
    appendLine("Este mes: S/ ${dashboard.totalRecaudadoMes.fixedAmount()} · ${dashboard.ticketsMes} ticket(s)")
    if (period?.hayPeriodoActivo == true) {
        appendLine()
        appendLine("PERIODO ${period.nombre.orEmpty()}")
        appendLine("Ingresos acumulados: S/ ${period.ingresosAcumulados.fixedAmount()}")
        appendLine("Tickets: ${period.tickets}")
        appendLine("Estudiantes: ${period.estudiantes}")
        appendLine("Servicios: ${period.servicios}")
        appendLine("Dias restantes: ${period.diasRestantes}")
    }
    if (report.resumenPorServicio.isNotEmpty()) {
        appendLine()
        appendLine("SERVICIOS")
        report.resumenPorServicio.take(10).forEach {
            appendLine("- ${it.nombre}: ${it.cantidad} · S/ ${it.monto.fixedAmount()}")
        }
    }
}

private fun List<IncomeReportItemDto>.summaryBy(name: (IncomeReportItemDto) -> String): List<ReportSummaryDto> =
    groupBy(name)
        .map { (groupName, rows) ->
            ReportSummaryDto(
                nombre = groupName,
                cantidad = rows.size,
                monto = rows.sumOf { it.importe },
            )
        }
        .sortedByDescending { it.monto }

private fun Double.fixedAmount(): String = String.format(Locale.US, "%.2f", this)

private fun csvRow(vararg cells: String): String = cells.joinToString(",") { cell ->
    "\"${cell.replace("\"", "\"\"")}\""
}
