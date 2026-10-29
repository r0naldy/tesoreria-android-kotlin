package pe.edu.hzg.tesoreria.features.academic.data

import kotlinx.serialization.Serializable

@Serializable
data class AcademicProgramDto(
    val id: Int,
    val nombre: String,
    val estado: String = "Activo",
)

@Serializable
data class ShiftDto(
    val id: Int,
    val nombre: String,
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
data class DniLookupDto(
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
    val carreras: List<StudentCareerDto> = emptyList(),
)

@Serializable
data class StudentDto(
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
    val condicion: String?,
)

@Serializable
data class CreatedIdDto(val id: Int)

@Serializable
data class UpdateStudentRequest(
    val id: Int,
    val nombres: String,
    val apellidos: String,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val celular: String? = null,
)

@Serializable
data class RatificationDto(
    val alumnoId: Int,
    val semestre: String,
    val ratificadoEl: String,
)

@Serializable
data class WithdrawalRequest(
    val motivo: String,
    val detalle: String,
    val devolucion: Double? = null,
    val metodoDevolucion: String? = null,
)

@Serializable
data class WithdrawalResultDto(
    val alumnoId: Int,
    val pagado: Double,
    val devuelto: Double,
    val egresoId: Int? = null,
)

@Serializable
data class SemesterDto(
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val cerrado: Boolean,
)

@Serializable
data class PeriodDto(
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
    val semestres: List<SemesterDto> = emptyList(),
    val semestreActivo: SemesterDto? = null,
)

@Serializable
data class PeriodSummaryDto(
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

@Serializable
data class OpenPeriodRequest(
    val nombre: String,
    val fechaInicio: String = "$nombre-01-01",
    val fechaFin: String = "$nombre-12-31",
    val observaciones: String? = null,
)

@Serializable
data class IncomeItemDto(
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

@Serializable
data class SummaryGroupDto(
    val nombre: String,
    val cantidad: Int,
    val monto: Double,
)

@Serializable
data class PeriodDetailDto(
    val periodo: PeriodSummaryDto,
    val items: List<IncomeItemDto> = emptyList(),
    val total: Double = 0.0,
    val resumenPorServicio: List<SummaryGroupDto> = emptyList(),
    val resumenPorPrograma: List<SummaryGroupDto> = emptyList(),
    val resumenPorUsuario: List<SummaryGroupDto> = emptyList(),
)

@Serializable
data class RecordDto(
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

@Serializable
data class UpdateRecordRequest(
    val id: Int,
    val profesor: String? = null,
    val moduloFormativo: String? = null,
    val resolucionPrograma: String? = null,
)

internal fun recordPersonalDataError(request: UpdateRecordRequest): String? = when {
    request.id <= 0 -> "El registro no tiene un identificador válido."
    request.profesor.orEmpty().trim().length > 150 -> "El docente admite hasta 150 caracteres."
    request.moduloFormativo.orEmpty().trim().length > 200 -> "El módulo formativo admite hasta 200 caracteres."
    request.resolucionPrograma.orEmpty().trim().length > 150 -> "La resolución admite hasta 150 caracteres."
    else -> null
}

@Serializable
data class CreateRecordRequest(
    val programaId: Int,
    val turnoId: Int,
    val seccion: String = "U",
    val periodoId: Int? = null,
    val profesor: String? = null,
    val moduloFormativo: String? = null,
)

@Serializable
data class EnrollmentItemDto(
    val n: Int,
    val codigoMatricula: String,
    val apellidosNombres: String,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val edad: Int? = null,
    val celular: String? = null,
    val condicion: String = "G",
)

@Serializable
data class EnrollmentRosterDto(
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
    val estudiantes: List<EnrollmentItemDto> = emptyList(),
    val hombres: Int = 0,
    val mujeres: Int = 0,
    val pagantes: Int = 0,
    val becados: Int = 0,
)

@Serializable
data class ComparisonDayDto(
    val fecha: String,
    val ingresos: Double,
    val egresos: Double,
)

@Serializable
data class ComparisonGroupDto(
    val nombre: String,
    val cantidad: Int,
    val monto: Double,
)

@Serializable
data class MovementDto(
    val tipo: String,
    val fecha: String,
    val descripcion: String,
    val detalle: String,
    val monto: Double,
    val usuario: String,
)

@Serializable
data class ComparisonDto(
    val desde: String? = null,
    val hasta: String? = null,
    val totalIngresos: Double = 0.0,
    val totalEgresos: Double = 0.0,
    val balance: Double = 0.0,
    val cantidadIngresos: Int = 0,
    val cantidadEgresos: Int = 0,
    val serie: List<ComparisonDayDto> = emptyList(),
    val egresosPorCategoria: List<ComparisonGroupDto> = emptyList(),
    val ingresosPorConcepto: List<ComparisonGroupDto> = emptyList(),
    val movimientos: List<MovementDto> = emptyList(),
)
