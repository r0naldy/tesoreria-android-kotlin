package pe.edu.hzg.tesoreria.features.treasury.data

import pe.edu.hzg.tesoreria.features.treasury.domain.model.*
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

    suspend fun dashboard(): TreasuryDashboard = client.get("$root/dashboard").body<TreasuryDashboardDto>().toDomain()
    suspend fun dashboardPeriod(): DashboardPeriod = client.get("$root/periodos/dashboard").body<DashboardPeriodDto>().toDomain()
    suspend fun programs(): List<TreasuryProgram> = client.get("$root/programas") { parameter("soloActivos", true) }.body<List<TreasuryProgramDto>>().map { it.toDomain() }
    suspend fun services(): List<TreasuryService> = client.get("$root/servicios") { parameter("soloActivos", true) }.body<List<TreasuryServiceDto>>().map { it.toDomain() }
    suspend fun shifts(): List<Shift> = client.get("$root/turnos").body<List<ShiftDto>>().map { it.toDomain() }
    suspend fun activePeriod(): ActivePeriod? = client.get("$root/periodos/activo").body<ActivePeriodDto?>()?.toDomain()
    suspend fun registers(periodId: Int): List<EnrollmentRegister> =
        client.get("$root/registros") { parameter("periodoId", periodId) }.body<List<EnrollmentRegisterDto>>().map { it.toDomain() }

    suspend fun lookupStudent(dni: String): StudentLookup =
        client.get("$root/alumnos/consulta-dni/$dni").body<StudentLookupDto>().toDomain()

    suspend fun createStudent(request: CreateStudentCommand): CreatedStudent =
        client.post("$root/alumnos") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<CreatedStudentDto>().toDomain()

    /**
     * Deliberadamente sin reintento. El API aun no recibe una clave de idempotencia y repetir
     * un POST podria emitir dos correlativos si se pierde la respuesta despues del commit.
     */
    suspend fun emitTicket(request: EmitTicketCommand): Ticket =
        client.post("$root/tickets") {
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }.body<TicketDto>().toDomain()

    suspend fun tickets(from: String?, to: String?, dni: String?): List<TicketListItem> =
        client.get("$root/tickets") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
            dni?.takeIf(String::isNotBlank)?.let { parameter("dni", it) }
        }.body<List<TicketListItemDto>>().map { it.toDomain() }

    suspend fun ticket(id: Int): Ticket = client.get("$root/tickets/$id").body<TicketDto>().toDomain()
    suspend fun configuration(): InstitutionConfig = client.get("$root/configuracion").body<InstitutionConfigDto>().toDomain()

    suspend fun incomeReport(from: String?, to: String?): IncomeReport =
        client.get("$root/reportes/ingresos") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
        }.body<IncomeReportDto>().toDomain()

    suspend fun expenses(from: String?, to: String?, category: String?): List<ExpenseListItem> =
        client.get("$root/comprobantes") {
            from?.takeIf(String::isNotBlank)?.let { parameter("desde", it) }
            to?.takeIf(String::isNotBlank)?.let { parameter("hasta", it) }
            category?.takeIf(String::isNotBlank)?.let { parameter("categoria", it) }
        }.body<List<ExpenseListItemDto>>().map { it.toDomain() }

    suspend fun expense(id: Int): Expense = client.get("$root/comprobantes/$id").body<ExpenseDto>().toDomain()
}
