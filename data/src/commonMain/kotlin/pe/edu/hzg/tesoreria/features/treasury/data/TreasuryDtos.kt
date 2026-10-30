package pe.edu.hzg.tesoreria.features.treasury.data

import kotlinx.serialization.Serializable

@Serializable
data class TreasuryDashboardDto(
    val totalRecaudadoHoy: Double = 0.0,
    val totalRecaudadoMes: Double = 0.0,
    val ticketsHoy: Int = 0,
    val ticketsMes: Int = 0,
    val totalRecaudadoAyer: Double = 0.0,
    val totalRecaudadoMesPasado: Double = 0.0,
    val ticketsAyer: Int = 0,
    val ticketsMesPasado: Int = 0,
    val programasActivos: Int = 0,
    val serviciosMasCobrados: List<CountItemDto> = emptyList(),
    val programasConMasPagos: List<CountItemDto> = emptyList(),
    val ultimosTickets: List<TicketListItemDto> = emptyList(),
    val ingresosDiarios: List<DailyIncomeDto> = emptyList(),
)

/**
 * Resumen financiero del periodo abierto. Es distinto de [ActivePeriodDto]: este contrato
 * corresponde exactamente a GET /api/periodos/dashboard e incluye los acumulados que el
 * dashboard web muestra en su bloque de gestion del periodo.
 */
@Serializable
data class DashboardPeriodDto(
    val hayPeriodoActivo: Boolean = false,
    val nombre: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val ingresosAcumulados: Double = 0.0,
    val tickets: Int = 0,
    val estudiantes: Int = 0,
    val servicios: Int = 0,
    val diasRestantes: Int = 0,
)

@Serializable data class CountItemDto(val nombre: String = "", val cantidad: Int = 0, val monto: Double = 0.0)
@Serializable data class DailyIncomeDto(val fecha: String = "", val monto: Double = 0.0)

@Serializable
data class StudentLookupDto(
    val dni: String = "",
    val existe: Boolean = false,
    val encontradoReniec: Boolean = false,
    val alumnoId: Int? = null,
    val nombres: String = "",
    val apellidos: String = "",
    val nombreCompleto: String = "",
    val programaId: Int? = null,
    val programa: String? = null,
    val turnoId: Int? = null,
    val turno: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val edad: Int? = null,
    val celular: String? = null,
    val seccion: String? = null,
    val carreras: List<StudentCareerDto> = emptyList(),
)

@Serializable
data class StudentCareerDto(
    val alumnoId: Int,
    val programaId: Int,
    val programa: String,
    val turnoId: Int,
    val turno: String,
    val seccion: String,
    val periodo: String? = null,
    val fechaRegistro: String,
)

@Serializable
data class CreateStudentRequest(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val programaId: Int,
    val turnoId: Int,
    val seccion: String = "U",
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val celular: String? = null,
    val condicion: String? = "G",
)

@Serializable data class CreatedStudentDto(val id: Int)

@Serializable
data class TreasuryProgramDto(
    val id: Int,
    val nombre: String,
    val estado: String = "Activo",
    val fechaCreacion: String = "",
)

@Serializable data class ShiftDto(val id: Int, val nombre: String)

@Serializable
data class TreasuryServiceDto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val estado: String = "Activo",
)

@Serializable
data class ActivePeriodDto(
    val id: Int,
    val nombre: String,
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val estado: String = "",
    val diasRestantes: Int? = null,
)

@Serializable
data class EnrollmentRegisterDto(
    val id: Int,
    val periodoId: Int,
    val periodo: String,
    val programaId: Int,
    val programa: String,
    val turnoId: Int,
    val turno: String,
    val seccion: String,
    val cantidad: Int = 0,
)

@Serializable data class EmitTicketDetailRequest(val servicioId: Int, val importe: Double)
@Serializable data class EmitTicketRequest(val alumnoId: Int, val detalles: List<EmitTicketDetailRequest>)

@Serializable
data class TicketDetailDto(
    val servicioId: Int,
    val servicio: String,
    val importe: Double,
)

@Serializable
data class TicketDto(
    val id: Int,
    val numeroTicket: String,
    val contador: String,
    val fechaEmision: String,
    val dni: String,
    val alumnoNombre: String,
    val programa: String,
    val turno: String,
    val total: Double,
    val totalEnLetras: String,
    val usuario: String,
    val estado: String,
    val detalles: List<TicketDetailDto> = emptyList(),
)

@Serializable
data class TicketListItemDto(
    val id: Int,
    val numeroTicket: String,
    val contador: String,
    val fechaEmision: String,
    val dni: String,
    val alumnoNombre: String,
    val total: Double,
    val usuario: String,
    val estado: String,
)

@Serializable
data class InstitutionConfigDto(
    val nombreInstitucion: String = "INSTITUTO DE EDUCACION SUPERIOR TECNOLOGICO PRODUCTIVO HORACIO ZEBALLOS GAMEZ",
    val ciudad: String = "JULIACA",
    val baseLegal: String = "",
    val tituloComprobante: String = "Ingreso por Recursos Propios y Actividades Productivas",
    val tipoComprobante: String = "Ticket",
    val logoBase64: String? = null,
)

@Serializable
data class IncomeReportDto(
    val desde: String? = null,
    val hasta: String? = null,
    val cantidadTickets: Int = 0,
    val cantidadServicios: Int = 0,
    val total: Double = 0.0,
    val items: List<IncomeReportItemDto> = emptyList(),
    val resumenPorServicio: List<ReportSummaryDto> = emptyList(),
    val resumenPorPrograma: List<ReportSummaryDto> = emptyList(),
)

@Serializable
data class IncomeReportItemDto(
    val fecha: String,
    val numeroTicket: String,
    val contador: String,
    val dni: String,
    val alumno: String,
    val programa: String,
    val servicio: String,
    val importe: Double,
    val usuario: String,
)

@Serializable data class ReportSummaryDto(val nombre: String, val cantidad: Int, val monto: Double)

@Serializable
data class ExpenseListItemDto(
    val id: Int,
    val proveedor: String? = null,
    val ruc: String? = null,
    val tipoDocumento: String? = null,
    val numeroComprobante: String? = null,
    val fechaEmision: String? = null,
    val fechaRegistro: String,
    val total: Double,
    val moneda: String = "PEN",
    val categoria: String? = null,
    val concepto: String? = null,
    val confianza: Int = 0,
    val esDuplicadoProbable: Boolean = false,
    val usuario: String = "",
    val estado: String = "",
)

@Serializable
data class ExpenseProductDto(
    val orden: Int = 0,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val precioUnitario: Double? = null,
    val importe: Double? = null,
)

@Serializable
data class ExpenseDto(
    val id: Int,
    val proveedor: String? = null,
    val ruc: String? = null,
    val tipoDocumento: String? = null,
    val numeroComprobante: String? = null,
    val fechaEmision: String? = null,
    val horaEmision: String? = null,
    val moneda: String = "PEN",
    val subtotal: Double? = null,
    val igv: Double? = null,
    val total: Double,
    val categoria: String? = null,
    val concepto: String? = null,
    val metodoPago: String? = null,
    val confianza: Int = 0,
    val esDuplicadoProbable: Boolean = false,
    val observaciones: String? = null,
    val imagenRuta: String? = null,
    val imagenUrl: String? = null,
    val imagenBase64: String? = null,
    val fechaRegistro: String,
    val usuario: String = "",
    val estado: String = "",
    val cantidadConceptos: Int = 0,
    val productos: List<ExpenseProductDto> = emptyList(),
)
