package pe.edu.hzg.tesoreria.features.treasury.data

import pe.edu.hzg.tesoreria.features.treasury.domain.model.*
import pe.edu.hzg.tesoreria.features.treasury.domain.repository.TreasuryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio online-first. Conserva en memoria el ultimo dato confirmado para que un fallo de red
 * no vacie las pantallas. Las escrituras monetarias nunca se reintentan automaticamente.
 */
class TreasuryRepositoryImpl(private val api: TreasuryApi) : TreasuryRepository {
    private val _dashboard = MutableStateFlow<TreasuryDashboard?>(null)
    override val dashboard: StateFlow<TreasuryDashboard?> = _dashboard.asStateFlow()

    private val _programs = MutableStateFlow<List<TreasuryProgram>>(emptyList())
    override val programs: StateFlow<List<TreasuryProgram>> = _programs.asStateFlow()
    private val _services = MutableStateFlow<List<TreasuryService>>(emptyList())
    override val services: StateFlow<List<TreasuryService>> = _services.asStateFlow()
    private val _shifts = MutableStateFlow<List<Shift>>(emptyList())
    override val shifts: StateFlow<List<Shift>> = _shifts.asStateFlow()
    private val _period = MutableStateFlow<ActivePeriod?>(null)
    override val period: StateFlow<ActivePeriod?> = _period.asStateFlow()
    private val _registers = MutableStateFlow<List<EnrollmentRegister>>(emptyList())
    override val registers: StateFlow<List<EnrollmentRegister>> = _registers.asStateFlow()
    private val studentCache = mutableMapOf<String, StudentLookup>()

    override suspend fun refreshDashboard(): TreasuryDashboard = api.dashboard().also { _dashboard.value = it }
    override suspend fun dashboardPeriod(): DashboardPeriod = api.dashboardPeriod()

    override suspend fun bootstrapCashier() = coroutineScope {
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

    override suspend fun lookupStudent(dni: String): StudentLookup =
        api.lookupStudent(dni).also { studentCache[dni] = it }

    override fun cachedStudent(dni: String): StudentLookup? = studentCache[dni]

    override suspend fun createStudent(request: CreateStudentCommand): StudentLookup {
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

    override suspend fun emitTicket(studentId: Int, lines: List<EmitTicketDetailCommand>): Ticket =
        api.emitTicket(EmitTicketCommand(studentId, lines))

    override suspend fun tickets(from: String?, to: String?, dni: String?) = api.tickets(from, to, dni)
    override suspend fun ticket(id: Int) = api.ticket(id)
    override suspend fun configuration() = api.configuration()
    override suspend fun incomeReport(from: String?, to: String?) = api.incomeReport(from, to)
    override suspend fun expenses(from: String?, to: String?, category: String?) = api.expenses(from, to, category)
    override suspend fun expense(id: Int) = api.expense(id)
}
