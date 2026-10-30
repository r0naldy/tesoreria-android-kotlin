package pe.edu.hzg.tesoreria.features.academic.data

import pe.edu.hzg.tesoreria.features.academic.domain.model.*
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pe.edu.hzg.tesoreria.data.remote.ApiClient

/**
 * Contrato móvil de los módulos académicos. Todas las rutas usan JWT; de forma
 * deliberada no existe ninguna llamada a /api/integracion ni una clave ASISTO.
 */
class AcademicApi(
    apiClient: ApiClient,
    baseUrl: String,
) {
    private val client = apiClient.http
    private val root = "${baseUrl.trimEnd('/')}/api"

    suspend fun students(search: String? = null): List<Student> =
        client.get("$root/alumnos") {
            search?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("buscar", it) }
        }.body<List<StudentDto>>().map { it.toDomain() }

    suspend fun lookupDni(dni: String): DniLookup =
        client.get("$root/alumnos/consulta-dni/$dni").body<DniLookupDto>().toDomain()

    suspend fun createStudent(request: CreateStudentCommand): Int =
        client.post("$root/alumnos") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<CreatedIdDto>().id

    suspend fun withdrawalReasons(): List<String> =
        client.get("$root/alumnos/motivos-de-retiro").body()

    suspend fun updateStudent(request: UpdateStudentCommand) {
        client.put("$root/alumnos/${request.id}") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }
    }

    suspend fun deleteStudent(id: Int) {
        client.delete("$root/alumnos/$id")
    }

    suspend fun ratifyStudent(id: Int): Ratification =
        client.post("$root/alumnos/$id/ratificacion").body<RatificationDto>().toDomain()

    suspend fun withdrawStudent(id: Int, request: WithdrawalCommand): WithdrawalResult =
        client.post("$root/alumnos/$id/retiro") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<WithdrawalResultDto>().toDomain()

    suspend fun programs(): List<AcademicProgram> =
        client.get("$root/programas") {
            url.parameters.append("soloActivos", "true")
        }.body<List<AcademicProgramDto>>().map { it.toDomain() }

    suspend fun shifts(): List<Shift> = client.get("$root/turnos").body<List<ShiftDto>>().map { it.toDomain() }

    suspend fun periods(): List<Period> = client.get("$root/periodos").body<List<PeriodDto>>().map { it.toDomain() }

    suspend fun activePeriod(): Period? = client.get("$root/periodos/activo").body<PeriodDto?>()?.toDomain()

    suspend fun periodSummary(id: Int): PeriodSummary =
        client.get("$root/periodos/$id/resumen").body<PeriodSummaryDto>().toDomain()

    suspend fun periodDetail(id: Int): PeriodDetail =
        client.get("$root/periodos/$id/detalle").body<PeriodDetailDto>().toDomain()

    suspend fun openPeriod(request: OpenPeriodCommand): Int =
        client.post("$root/periodos") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<CreatedIdDto>().id

    suspend fun closePeriod(id: Int): PeriodSummary =
        client.put("$root/periodos/$id/cerrar").body<PeriodSummaryDto>().toDomain()

    suspend fun records(periodId: Int? = null): List<Record> =
        client.get("$root/registros") {
            periodId?.let { url.parameters.append("periodoId", it.toString()) }
        }.body<List<RecordDto>>().map { it.toDomain() }

    suspend fun roster(id: Int, semester: String? = null): EnrollmentRoster =
        client.get("$root/registros/$id") {
            semester?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("semestre", it) }
        }.body<EnrollmentRosterDto>().toDomain()

    suspend fun recordExcel(id: Int, semester: String? = null): ByteArray =
        client.get("$root/registros/$id/excel") {
            semester?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("semestre", it) }
        }.body()

    suspend fun createRecord(request: CreateRecordCommand): Int =
        client.post("$root/registros") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<CreatedIdDto>().id

    suspend fun deleteRecord(id: Int) {
        client.delete("$root/registros/$id")
    }

    suspend fun updateRecord(request: UpdateRecordCommand) {
        client.put("$root/registros/${request.id}") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }
    }

    suspend fun comparison(from: String? = null, to: String? = null): Comparison =
        client.get("$root/comparativa") {
            from?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("desde", it) }
            to?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("hasta", it) }
        }.body<ComparisonDto>().toDomain()
}
