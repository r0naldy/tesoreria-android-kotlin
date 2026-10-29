package pe.edu.hzg.tesoreria.features.treasury.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pe.edu.hzg.tesoreria.data.remote.ApiClient

class TreasuryApi(apiClient: ApiClient, baseUrl: String) {
    private val client = apiClient.http
    private val root = "${baseUrl.trimEnd('/')}/api"

    suspend fun dashboard(): TreasuryDashboardDto = client.get("$root/dashboard").body()
    suspend fun dashboardPeriod(): DashboardPeriodDto = client.get("$root/periodos/dashboard").body()
    suspend fun programs(): List<TreasuryProgramDto> = client.get("$root/programas") { parameter("soloActivos", true) }.body()
    suspend fun services(): List<TreasuryServiceDto> = client.get("$root/servicios") { parameter("soloActivos", true) }.body()
    suspend fun shifts(): List<ShiftDto> = client.get("$root/turnos").body()
    suspend fun activePeriod(): ActivePeriodDto? = client.get("$root/periodos/activo").body()
    suspend fun registers(periodId: Int): List<EnrollmentRegisterDto> =
        client.get("$root/registros") { parameter("periodoId", periodId) }.body()

    suspend fun lookupStudent(dni: String): StudentLookupDto =
        client.get("$root/alumnos/consulta-dni/$dni").body()

    suspend fun createStudent(request: CreateStudentRequest): CreatedStudentDto =
        client.post("$root/alumnos") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * Deliberadamente sin reintento. El API aun no recibe una clave de idempotencia y repetir
     * un POST podria emitir dos correlativos si se pierde la respuesta despues del commit.
     */
    suspend fun emitTicket(request: EmitTicketRequest): TicketDto =
        client.post("$root/tickets") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun tickets(from: String?, to: String?, dni: String?): List<TicketListItemDto> =
        client.get("$root/tickets") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
            dni?.takeIf(String::isNotBlank)?.let { parameter("dni", it) }
        }.body()

    suspend fun ticket(id: Int): TicketDto = client.get("$root/tickets/$id").body()
    suspend fun configuration(): InstitutionConfigDto = client.get("$root/configuracion").body()

    suspend fun incomeReport(from: String?, to: String?): IncomeReportDto =
        client.get("$root/reportes/ingresos") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
        }.body()

    suspend fun expenses(from: String?, to: String?, category: String?): List<ExpenseListItemDto> =
        client.get("$root/comprobantes") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
            category?.takeIf(String::isNotBlank)?.let { parameter("categoria", it) }
        }.body()

    suspend fun expense(id: Int): ExpenseDto = client.get("$root/comprobantes/$id").body()
}
