package pe.edu.hzg.tesoreria.features.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardReportToolsTest {
    private val report = IncomeReportDto(
        desde = "2026-10-01",
        hasta = "2026-10-31",
        items = listOf(
            item("001", "Administracion", "Constancia", 10.0, "ana"),
            item("001", "Administracion", "Certificado", 15.0, "ana"),
            item("002", "Gastronomia", "Constancia", 20.0, "luis"),
        ),
    )

    @Test
    fun filtersLocallyAndRecalculatesAllTotals() {
        val filtered = report.filtered(
            DashboardReportFilter(program = "Administracion", user = "ana"),
        )

        assertEquals(1, filtered.cantidadTickets)
        assertEquals(2, filtered.cantidadServicios)
        assertEquals(25.0, filtered.total, 0.001)
        assertEquals(2, filtered.resumenPorServicio.size)
        assertEquals(1, filtered.resumenPorPrograma.size)
    }

    @Test
    fun csvEscapesQuotesAndExportsOnlyFilteredRows() {
        val quoted = report.copy(
            items = report.items + item("003", "Diseño", "Copia \"A4\"", 2.5, "ana"),
        ).filtered(DashboardReportFilter(service = "Copia \"A4\""))

        val csv = dashboardCsv(quoted)

        assertTrue(csv.startsWith("\"REPORTE DEL DASHBOARD\""))
        assertTrue(csv.contains("\"Copia \"\"A4\"\"\""))
        assertTrue(csv.contains("\"2.50\""))
        assertFalse(csv.contains("\"Constancia\""))
    }

    private fun item(
        ticket: String,
        program: String,
        service: String,
        amount: Double,
        user: String,
    ) = IncomeReportItemDto(
        fecha = "2026-10-20T10:30:00",
        numeroTicket = ticket,
        contador = ticket,
        dni = "12345678",
        alumno = "Estudiante",
        programa = program,
        servicio = service,
        importe = amount,
        usuario = user,
    )
}
