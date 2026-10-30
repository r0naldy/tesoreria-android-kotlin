package pe.edu.hzg.tesoreria.features.treasury.presentation

import pe.edu.hzg.tesoreria.domain.repository.ErrorMessages
import pe.edu.hzg.tesoreria.domain.util.decimal
import pe.edu.hzg.tesoreria.domain.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.features.treasury.domain.model.ActivePeriod
import pe.edu.hzg.tesoreria.features.treasury.domain.model.CreateStudentCommand
import pe.edu.hzg.tesoreria.features.treasury.domain.model.DashboardPeriod
import pe.edu.hzg.tesoreria.features.treasury.domain.DashboardReportFilter
import pe.edu.hzg.tesoreria.features.treasury.domain.model.EmitTicketDetailCommand
import pe.edu.hzg.tesoreria.features.treasury.domain.model.EnrollmentRegister
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Expense
import pe.edu.hzg.tesoreria.features.treasury.domain.model.ExpenseListItem
import pe.edu.hzg.tesoreria.features.treasury.domain.model.IncomeReport
import pe.edu.hzg.tesoreria.features.treasury.domain.model.InstitutionConfig
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Shift
import pe.edu.hzg.tesoreria.features.treasury.domain.model.StudentLookup
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Ticket
import pe.edu.hzg.tesoreria.features.treasury.domain.model.TicketListItem
import pe.edu.hzg.tesoreria.features.treasury.domain.model.TreasuryDashboard
import pe.edu.hzg.tesoreria.features.treasury.domain.model.TreasuryProgram
import pe.edu.hzg.tesoreria.features.treasury.domain.repository.TreasuryRepository
import pe.edu.hzg.tesoreria.features.treasury.domain.model.TreasuryService
import pe.edu.hzg.tesoreria.features.treasury.domain.filtered

data class DashboardUiState(
    val data: TreasuryDashboard? = null,
    val period: DashboardPeriod? = null,
    val report: IncomeReport? = null,
    val from: String = LocalDate.now().minusDays(29).toString(),
    val to: String = LocalDate.now().toString(),
    val program: String = "",
    val service: String = "",
    val user: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val filteredReport: IncomeReport?
        get() = report?.filtered(
            DashboardReportFilter(program = program, service = service, user = user),
        )
}

class TreasuryDashboardViewModel(private val repository: TreasuryRepository, private val errors: ErrorMessages) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState(data = repository.dashboard.value))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val current = _state.value
        runCatching {
            coroutineScope {
                val dashboard = async { repository.refreshDashboard() }
                val period = async { repository.dashboardPeriod() }
                val report = async { repository.incomeReport(current.from, current.to) }
                Triple(dashboard.await(), period.await(), report.await())
            }
        }
            .onSuccess { (dashboard, period, report) ->
                _state.value = _state.value.copy(
                    data = dashboard,
                    period = period,
                    report = report,
                    loading = false,
                    error = null,
                )
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = errors.treasury(it)) }
    }

    fun loadRange(from: String, to: String) {
        val parsedFrom = runCatching { LocalDate.parse(from) }.getOrNull()
        val parsedTo = runCatching { LocalDate.parse(to) }.getOrNull()
        if (parsedFrom == null || parsedTo == null) {
            _state.value = _state.value.copy(error = "Use fechas validas con formato AAAA-MM-DD.")
            return
        }
        if (parsedFrom > parsedTo) {
            _state.value = _state.value.copy(error = "La fecha Desde no puede ser posterior a Hasta.")
            return
        }
        _state.value = _state.value.copy(from = from, to = to, error = null)
        refresh()
    }

    fun setProgram(value: String) { _state.value = _state.value.copy(program = value) }
    fun setService(value: String) { _state.value = _state.value.copy(service = value) }
    fun setUser(value: String) { _state.value = _state.value.copy(user = value) }
    fun clearFilters() {
        _state.value = _state.value.copy(program = "", service = "", user = "")
    }
}

data class CashierUiState(
    val programs: List<TreasuryProgram> = emptyList(),
    val services: List<TreasuryService> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    val period: ActivePeriod? = null,
    val registers: List<EnrollmentRegister> = emptyList(),
    val configuration: InstitutionConfig? = null,
    val dni: String = "",
    val student: StudentLookup? = null,
    val selectedStudentId: Int? = null,
    val loadingCatalogs: Boolean = false,
    val searching: Boolean = false,
    val registering: Boolean = false,
    val emitting: Boolean = false,
    val emittedTicket: Ticket? = null,
    val message: String? = null,
    val error: String? = null,
)

class CashierViewModel(private val repository: TreasuryRepository, private val errors: ErrorMessages) : ViewModel() {
    private val _state = MutableStateFlow(CashierUiState())
    val state: StateFlow<CashierUiState> = _state.asStateFlow()
    private var lookupJob: Job? = null

    init { loadCatalogs() }

    fun loadCatalogs() = viewModelScope.launch {
        _state.value = _state.value.copy(loadingCatalogs = true, error = null)
        runCatching { repository.bootstrapCashier() }
            .onSuccess {
                val configuration = runCatching { repository.configuration() }.getOrNull()
                _state.value = _state.value.copy(
                    programs = repository.programs.value,
                    services = repository.services.value,
                    shifts = repository.shifts.value,
                    period = repository.period.value,
                    registers = repository.registers.value,
                    configuration = configuration,
                    loadingCatalogs = false,
                )
            }
            .onFailure { _state.value = _state.value.copy(loadingCatalogs = false, error = errors.treasury(it)) }
    }

    fun onDniChanged(raw: String) {
        val dni = raw.filter(Char::isDigit).take(8)
        lookupJob?.cancel()
        _state.value = _state.value.copy(
            dni = dni,
            student = null,
            selectedStudentId = null,
            searching = dni.length == 8,
            emittedTicket = null,
            message = null,
            error = null,
        )
        if (dni.length != 8) return
        repository.cachedStudent(dni)?.let { cached ->
            applyStudent(cached)
            return
        }
        lookupJob = viewModelScope.launch {
            delay(300)
            runCatching { repository.lookupStudent(dni) }
                .onSuccess(::applyStudent)
                .onFailure { _state.value = _state.value.copy(searching = false, error = errors.treasury(it)) }
        }
    }

    private fun applyStudent(student: StudentLookup) {
        _state.value = _state.value.copy(
            student = student,
            selectedStudentId = student.alumnoId ?: student.carreras.firstOrNull()?.alumnoId,
            searching = false,
            error = null,
        )
    }

    fun selectCareer(studentId: Int) {
        _state.value = _state.value.copy(selectedStudentId = studentId, emittedTicket = null)
    }

    fun registerStudent(request: CreateStudentCommand) = viewModelScope.launch {
        if (_state.value.registering) return@launch
        _state.value = _state.value.copy(registering = true, error = null, message = null)
        runCatching { repository.createStudent(request) }
            .onSuccess {
                applyStudent(it)
                _state.value = _state.value.copy(registering = false, message = "Estudiante registrado correctamente.")
            }
            .onFailure { _state.value = _state.value.copy(registering = false, error = errors.treasury(it)) }
    }

    fun emitTicket(lines: List<EmitTicketDetailCommand>) = viewModelScope.launch {
        val current = _state.value
        if (current.emitting) return@launch
        val studentId = current.selectedStudentId
        when {
            current.period == null -> _state.value = current.copy(error = "No existe un periodo academico abierto.")
            studentId == null -> _state.value = current.copy(error = "Seleccione o registre al estudiante.")
            lines.isEmpty() || lines.any { it.importe <= 0.0 } ->
                _state.value = current.copy(error = "Agregue al menos un servicio con importe mayor a cero.")
            else -> {
                _state.value = current.copy(emitting = true, error = null, message = null)
                runCatching { repository.emitTicket(studentId, lines) }
                    .onSuccess { _state.value = _state.value.copy(emitting = false, emittedTicket = it) }
                    .onFailure {
                        _state.value = _state.value.copy(
                            emitting = false,
                            error = "${errors.treasury(it)} Si la conexion se corto al emitir, revise Historial antes de intentarlo otra vez.",
                        )
                    }
            }
        }
    }

    fun newCharge() {
        lookupJob?.cancel()
        _state.value = _state.value.copy(
            dni = "", student = null, selectedStudentId = null, searching = false,
            emittedTicket = null, message = null, error = null,
        )
    }
    fun clearNotice() { _state.value = _state.value.copy(message = null, error = null) }
}

data class TicketHistoryUiState(
    val items: List<TicketListItem> = emptyList(),
    val detail: Ticket? = null,
    val configuration: InstitutionConfig? = null,
    val loading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null,
)

class TicketHistoryViewModel(private val repository: TreasuryRepository, private val errors: ErrorMessages) : ViewModel() {
    private val _state = MutableStateFlow(TicketHistoryUiState())
    val state = _state.asStateFlow()
    init { load(null, null, null) }

    fun load(from: String?, to: String?, dni: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, detail = null)
        runCatching { repository.tickets(from, to, dni) }
            .onSuccess { _state.value = _state.value.copy(items = it, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = errors.treasury(it)) }
    }

    fun open(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(detailLoading = true, error = null)
        runCatching {
            val ticket = repository.ticket(id)
            val config = runCatching { repository.configuration() }.getOrNull()
            ticket to config
        }.onSuccess { (ticket, config) ->
            _state.value = _state.value.copy(detail = ticket, configuration = config, detailLoading = false)
        }.onFailure { _state.value = _state.value.copy(detailLoading = false, error = errors.treasury(it)) }
    }

    fun closeDetail() { _state.value = _state.value.copy(detail = null) }
}

data class IncomeReportUiState(
    val report: IncomeReport? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class IncomeReportViewModel(private val repository: TreasuryRepository, private val errors: ErrorMessages) : ViewModel() {
    private val _state = MutableStateFlow(IncomeReportUiState())
    val state = _state.asStateFlow()
    fun load(from: String?, to: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.incomeReport(from, to) }
            .onSuccess { _state.value = IncomeReportUiState(report = it) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = errors.treasury(it)) }
    }
}

data class ExpenseBrowserUiState(
    val items: List<ExpenseListItem> = emptyList(),
    val detail: Expense? = null,
    val loading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null,
)

class ExpenseBrowserViewModel(private val repository: TreasuryRepository, private val errors: ErrorMessages) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseBrowserUiState())
    val state = _state.asStateFlow()

    fun load(from: String?, to: String?, category: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.expenses(from, to, category) }
            .onSuccess { _state.value = _state.value.copy(items = it, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = errors.treasury(it)) }
    }

    fun open(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(detailLoading = true, error = null)
        runCatching { repository.expense(id) }
            .onSuccess { _state.value = _state.value.copy(detail = it, detailLoading = false) }
            .onFailure { _state.value = _state.value.copy(detailLoading = false, error = errors.treasury(it)) }
    }
    fun close() { _state.value = _state.value.copy(detail = null) }
}
