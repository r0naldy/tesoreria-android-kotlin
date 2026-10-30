package pe.edu.hzg.tesoreria.features.treasury.domain.model

data class TreasuryDashboard(
    val totalRecaudadoHoy: Double = 0.0,
    val totalRecaudadoMes: Double = 0.0,
    val ticketsHoy: Int = 0,
    val ticketsMes: Int = 0,
    val totalRecaudadoAyer: Double = 0.0,
    val totalRecaudadoMesPasado: Double = 0.0,
    val ticketsAyer: Int = 0,
    val ticketsMesPasado: Int = 0,
    val programasActivos: Int = 0,
    val serviciosMasCobrados: List<CountItem> = emptyList(),
    val programasConMasPagos: List<CountItem> = emptyList(),
    val ultimosTickets: List<TicketListItem> = emptyList(),
    val ingresosDiarios: List<DailyIncome> = emptyList(),
)

data class DashboardPeriod(
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

data class CountItem(val nombre: String = "", val cantidad: Int = 0, val monto: Double = 0.0)

data class DailyIncome(val fecha: String = "", val monto: Double = 0.0)

data class StudentLookup(
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
    val carreras: List<StudentCareer> = emptyList(),
)

data class StudentCareer(
    val alumnoId: Int,
    val programaId: Int,
    val programa: String,
    val turnoId: Int,
    val turno: String,
    val seccion: String,
    val periodo: String? = null,
    val fechaRegistro: String,
)

data class CreateStudentCommand(
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

data class CreatedStudent(val id: Int)

data class TreasuryProgram(
    val id: Int,
    val nombre: String,
    val estado: String = "Activo",
    val fechaCreacion: String = "",
)

data class Shift(val id: Int, val nombre: String)

data class TreasuryService(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val estado: String = "Activo",
)

data class ActivePeriod(
    val id: Int,
    val nombre: String,
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val estado: String = "",
    val diasRestantes: Int? = null,
)

data class EnrollmentRegister(
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

data class EmitTicketDetailCommand(val servicioId: Int, val importe: Double)

data class EmitTicketCommand(val alumnoId: Int, val detalles: List<EmitTicketDetailCommand>)

data class TicketDetail(
    val servicioId: Int,
    val servicio: String,
    val importe: Double,
)

data class Ticket(
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
    val detalles: List<TicketDetail> = emptyList(),
)

data class TicketListItem(
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

data class InstitutionConfig(
    val nombreInstitucion: String = "INSTITUTO DE EDUCACION SUPERIOR TECNOLOGICO PRODUCTIVO HORACIO ZEBALLOS GAMEZ",
    val ciudad: String = "JULIACA",
    val baseLegal: String = "",
    val tituloComprobante: String = "Ingreso por Recursos Propios y Actividades Productivas",
    val tipoComprobante: String = "Ticket",
    val logoBase64: String? = null,
)

data class IncomeReport(
    val desde: String? = null,
    val hasta: String? = null,
    val cantidadTickets: Int = 0,
    val cantidadServicios: Int = 0,
    val total: Double = 0.0,
    val items: List<IncomeReportItem> = emptyList(),
    val resumenPorServicio: List<ReportSummary> = emptyList(),
    val resumenPorPrograma: List<ReportSummary> = emptyList(),
)

data class IncomeReportItem(
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

data class ReportSummary(val nombre: String, val cantidad: Int, val monto: Double)

data class ExpenseListItem(
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

data class ExpenseProduct(
    val orden: Int = 0,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val precioUnitario: Double? = null,
    val importe: Double? = null,
)

data class Expense(
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
    val productos: List<ExpenseProduct> = emptyList(),
)
