package pe.edu.hzg.tesoreria.features.academic.domain.model

data class AcademicProgram(
    val id: Int,
    val nombre: String,
    val estado: String = "Activo",
)

data class Shift(
    val id: Int,
    val nombre: String,
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

data class DniLookup(
    val dni: String,
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

data class Student(
    val id: Int,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val nombreCompleto: String,
    val programaId: Int,
    val programa: String,
    val turnoId: Int,
    val turno: String,
    val seccion: String,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val edad: Int? = null,
    val celular: String? = null,
    val estado: String,
    val fechaRegistro: String,
    val pagado: Double = 0.0,
    val ratificadoEl: String? = null,
    val semestreDeRegistro: String = "",
    val semestreRatificado: String? = null,
    val fechaRetiro: String? = null,
    val motivoRetiro: String? = null,
    val usuarioRetiro: String? = null,
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
    val condicion: String?,
)

data class CreatedId(val id: Int)

data class UpdateStudentCommand(
    val id: Int,
    val nombres: String,
    val apellidos: String,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val celular: String? = null,
)

data class Ratification(
    val alumnoId: Int,
    val semestre: String,
    val ratificadoEl: String,
)

data class WithdrawalCommand(
    val motivo: String,
    val detalle: String,
    val devolucion: Double? = null,
    val metodoDevolucion: String? = null,
)

data class WithdrawalResult(
    val alumnoId: Int,
    val pagado: Double,
    val devuelto: Double,
    val egresoId: Int? = null,
)

data class Semester(
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val cerrado: Boolean,
)

data class Period(
    val id: Int,
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val usuarioApertura: String? = null,
    val fechaApertura: String,
    val usuarioCierre: String? = null,
    val fechaCierre: String? = null,
    val observaciones: String? = null,
    val tickets: Int = 0,
    val ingresos: Double = 0.0,
    val diasRestantes: Int? = null,
    val semestres: List<Semester> = emptyList(),
    val semestreActivo: Semester? = null,
)

data class PeriodSummary(
    val id: Int,
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val tickets: Int,
    val estudiantes: Int,
    val ingresos: Double,
    val servicios: Int,
    val usuarioApertura: String? = null,
    val usuarioCierre: String? = null,
    val fechaCierre: String? = null,
)

data class OpenPeriodCommand(
    val nombre: String,
    val fechaInicio: String = "$nombre-01-01",
    val fechaFin: String = "$nombre-12-31",
    val observaciones: String? = null,
)

data class IncomeItem(
    val fecha: String,
    val numeroTicket: String,
    val contador: String = "",
    val dni: String,
    val alumno: String,
    val programa: String,
    val servicio: String,
    val importe: Double,
    val usuario: String,
)

data class SummaryGroup(
    val nombre: String,
    val cantidad: Int,
    val monto: Double,
)

data class PeriodDetail(
    val periodo: PeriodSummary,
    val items: List<IncomeItem> = emptyList(),
    val total: Double = 0.0,
    val resumenPorServicio: List<SummaryGroup> = emptyList(),
    val resumenPorPrograma: List<SummaryGroup> = emptyList(),
    val resumenPorUsuario: List<SummaryGroup> = emptyList(),
)

data class Record(
    val id: Int,
    val periodoId: Int,
    val periodo: String,
    val programaId: Int,
    val programa: String,
    val turnoId: Int,
    val turno: String,
    val seccion: String,
    val cantidad: Int,
    val fechaCreacion: String,
    val profesor: String? = null,
    val moduloFormativo: String? = null,
    val resolucionPrograma: String? = null,
)

data class UpdateRecordCommand(
    val id: Int,
    val profesor: String? = null,
    val moduloFormativo: String? = null,
    val resolucionPrograma: String? = null,
)

data class CreateRecordCommand(
    val programaId: Int,
    val turnoId: Int,
    val seccion: String = "U",
    val periodoId: Int? = null,
    val profesor: String? = null,
    val moduloFormativo: String? = null,
)

data class EnrollmentItem(
    val n: Int,
    val codigoMatricula: String,
    val apellidosNombres: String,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val edad: Int? = null,
    val celular: String? = null,
    val condicion: String = "G",
)

data class EnrollmentRoster(
    val nombreInstitucion: String = "",
    val nombreCetpro: String = "",
    val codigoModular: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val periodo: String = "",
    val periodoInicio: String? = null,
    val periodoFin: String? = null,
    val programa: String = "",
    val turno: String = "",
    val seccion: String = "",
    val semestre: String? = null,
    val dreGre: String = "",
    val ugel: String = "",
    val resolucionCreacion: String = "",
    val resolucionAutorizacion: String = "",
    val periodoLectivo: String = "",
    val modalidadServicio: String = "",
    val nivelFormativo: String = "",
    val tipoPlan: String = "",
    val profesor: String? = null,
    val moduloFormativo: String? = null,
    val cantidad: Int = 0,
    val estudiantes: List<EnrollmentItem> = emptyList(),
    val hombres: Int = 0,
    val mujeres: Int = 0,
    val pagantes: Int = 0,
    val becados: Int = 0,
)

data class ComparisonDay(
    val fecha: String,
    val ingresos: Double,
    val egresos: Double,
)

data class ComparisonGroup(
    val nombre: String,
    val cantidad: Int,
    val monto: Double,
)

data class Movement(
    val tipo: String,
    val fecha: String,
    val descripcion: String,
    val detalle: String,
    val monto: Double,
    val usuario: String,
)

data class Comparison(
    val desde: String? = null,
    val hasta: String? = null,
    val totalIngresos: Double = 0.0,
    val totalEgresos: Double = 0.0,
    val balance: Double = 0.0,
    val cantidadIngresos: Int = 0,
    val cantidadEgresos: Int = 0,
    val serie: List<ComparisonDay> = emptyList(),
    val egresosPorCategoria: List<ComparisonGroup> = emptyList(),
    val ingresosPorConcepto: List<ComparisonGroup> = emptyList(),
    val movimientos: List<Movement> = emptyList(),
)
