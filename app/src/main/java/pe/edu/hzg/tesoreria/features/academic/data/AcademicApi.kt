package pe.edu.hzg.tesoreria.features.academic.data

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

    suspend fun students(search: String? = null): List<StudentDto> =
        client.get("$root/alumnos") {
            search?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("buscar", it) }
        }.body()

    suspend fun lookupDni(dni: String): DniLookupDto =
        client.get("$root/alumnos/consulta-dni/$dni").body()

    suspend fun createStudent(request: CreateStudentRequest): Int =
        client.post("$root/alumnos") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CreatedIdDto>().id

    suspend fun withdrawalReasons(): List<String> =
        client.get("$root/alumnos/motivos-de-retiro").body()

    suspend fun updateStudent(request: UpdateStudentRequest) {
        client.put("$root/alumnos/${request.id}") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteStudent(id: Int) {
        client.delete("$root/alumnos/$id")
    }

    suspend fun ratifyStudent(id: Int): RatificationDto =
        client.post("$root/alumnos/$id/ratificacion").body()

    suspend fun withdrawStudent(id: Int, request: WithdrawalRequest): WithdrawalResultDto =
        client.post("$root/alumnos/$id/retiro") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun programs(): List<AcademicProgramDto> =
        client.get("$root/programas") {
            url.parameters.append("soloActivos", "true")
        }.body()

    suspend fun shifts(): List<ShiftDto> = client.get("$root/turnos").body()

    suspend fun periods(): List<PeriodDto> = client.get("$root/periodos").body()

    suspend fun activePeriod(): PeriodDto? = client.get("$root/periodos/activo").body()

    suspend fun periodSummary(id: Int): PeriodSummaryDto =
        client.get("$root/periodos/$id/resumen").body()

    suspend fun periodDetail(id: Int): PeriodDetailDto =
        client.get("$root/periodos/$id/detalle").body()

    suspend fun openPeriod(request: OpenPeriodRequest): Int =
        client.post("$root/periodos") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CreatedIdDto>().id

    suspend fun closePeriod(id: Int): PeriodSummaryDto =
        client.put("$root/periodos/$id/cerrar").body()

    suspend fun records(periodId: Int? = null): List<RecordDto> =
        client.get("$root/registros") {
            periodId?.let { url.parameters.append("periodoId", it.toString()) }
        }.body()

    suspend fun roster(id: Int, semester: String? = null): EnrollmentRosterDto =
        client.get("$root/registros/$id") {
            semester?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("semestre", it) }
        }.body()

    suspend fun recordExcel(id: Int, semester: String? = null): ByteArray =
        client.get("$root/registros/$id/excel") {
            semester?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("semestre", it) }
        }.body()

    suspend fun createRecord(request: CreateRecordRequest): Int =
        client.post("$root/registros") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CreatedIdDto>().id

    suspend fun deleteRecord(id: Int) {
        client.delete("$root/registros/$id")
    }

    suspend fun updateRecord(request: UpdateRecordRequest) {
        client.put("$root/registros/${request.id}") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun comparison(from: String? = null, to: String? = null): ComparisonDto =
        client.get("$root/comparativa") {
            from?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("desde", it) }
            to?.trim()?.takeIf(String::isNotEmpty)?.let { url.parameters.append("hasta", it) }
        }.body()
}
