package pe.edu.hzg.tesoreria.features.treasury.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio online-first. Conserva en memoria el ultimo dato confirmado para que un fallo de red
 * no vacie las pantallas. Las escrituras monetarias nunca se reintentan automaticamente.
 */
class TreasuryRepository(private val api: TreasuryApi) {
    private val _dashboard = MutableStateFlow<TreasuryDashboardDto?>(null)
    val dashboard: StateFlow<TreasuryDashboardDto?> = _dashboard.asStateFlow()

    private val _programs = MutableStateFlow<List<TreasuryProgramDto>>(emptyList())
    val programs: StateFlow<List<TreasuryProgramDto>> = _programs.asStateFlow()
    private val _services = MutableStateFlow<List<TreasuryServiceDto>>(emptyList())
    val services: StateFlow<List<TreasuryServiceDto>> = _services.asStateFlow()
    private val _shifts = MutableStateFlow<List<ShiftDto>>(emptyList())
    val shifts: StateFlow<List<ShiftDto>> = _shifts.asStateFlow()
    private val _period = MutableStateFlow<ActivePeriodDto?>(null)
    val period: StateFlow<ActivePeriodDto?> = _period.asStateFlow()
    private val _registers = MutableStateFlow<List<EnrollmentRegisterDto>>(emptyList())
    val registers: StateFlow<List<EnrollmentRegisterDto>> = _registers.asStateFlow()
    private val studentCache = mutableMapOf<String, StudentLookupDto>()

    suspend fun refreshDashboard(): TreasuryDashboardDto = api.dashboard().also { _dashboard.value = it }
    suspend fun dashboardPeriod(): DashboardPeriodDto = api.dashboardPeriod()

    suspend fun bootstrapCashier() = coroutineScope {
        val p = async { api.programs() }
        val s = async { api.services() }
        val t = async { api.shifts() }
        val active = async { api.activePeriod() }
        _programs.value = p.await()
        _services.value = s.await()
        _shifts.value = t.await()
        _period.value = active.await()
        _registers.value = _period.value?.let { api.registers(it.id) }.orEmpty()
    }

    suspend fun lookupStudent(dni: String): StudentLookupDto =
        api.lookupStudent(dni).also { studentCache[dni] = it }

    fun cachedStudent(dni: String): StudentLookupDto? = studentCache[dni]

    suspend fun createStudent(request: CreateStudentRequest): StudentLookupDto {
        val created = api.createStudent(request)
        return api.lookupStudent(request.dni).let { fresh ->
            val selected = fresh.carreras.firstOrNull { it.alumnoId == created.id }
            if (selected == null) fresh else fresh.copy(
                alumnoId = selected.alumnoId,
                programaId = selected.programaId,
                programa = selected.programa,
                turnoId = selected.turnoId,
                turno = selected.turno,
                seccion = selected.seccion,
            )
        }.also { studentCache[request.dni] = it }
    }

    suspend fun emitTicket(studentId: Int, lines: List<EmitTicketDetailRequest>): TicketDto =
        api.emitTicket(EmitTicketRequest(studentId, lines))

    suspend fun tickets(from: String?, to: String?, dni: String?) = api.tickets(from, to, dni)
    suspend fun ticket(id: Int) = api.ticket(id)
    suspend fun configuration() = api.configuration()
    suspend fun incomeReport(from: String?, to: String?) = api.incomeReport(from, to)
    suspend fun expenses(from: String?, to: String?, category: String?) = api.expenses(from, to, category)
    suspend fun expense(id: Int) = api.expense(id)
}
