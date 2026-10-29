package pe.edu.hzg.tesoreria.features.treasury.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.features.treasury.data.ActivePeriodDto
import pe.edu.hzg.tesoreria.features.treasury.data.CreateStudentRequest
import pe.edu.hzg.tesoreria.features.treasury.data.DashboardPeriodDto
import pe.edu.hzg.tesoreria.features.treasury.data.DashboardReportFilter
import pe.edu.hzg.tesoreria.features.treasury.data.EmitTicketDetailRequest
import pe.edu.hzg.tesoreria.features.treasury.data.EnrollmentRegisterDto
import pe.edu.hzg.tesoreria.features.treasury.data.ExpenseDto
import pe.edu.hzg.tesoreria.features.treasury.data.ExpenseListItemDto
import pe.edu.hzg.tesoreria.features.treasury.data.IncomeReportDto
import pe.edu.hzg.tesoreria.features.treasury.data.InstitutionConfigDto
import pe.edu.hzg.tesoreria.features.treasury.data.ShiftDto
import pe.edu.hzg.tesoreria.features.treasury.data.StudentLookupDto
import pe.edu.hzg.tesoreria.features.treasury.data.TicketDto
import pe.edu.hzg.tesoreria.features.treasury.data.TicketListItemDto
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryDashboardDto
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryProgramDto
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryRepository
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryServiceDto
import pe.edu.hzg.tesoreria.features.treasury.data.filtered

data class DashboardUiState(
    val data: TreasuryDashboardDto? = null,
    val period: DashboardPeriodDto? = null,
    val report: IncomeReportDto? = null,
    val from: String = LocalDate.now().minusDays(29).toString(),
    val to: String = LocalDate.now().toString(),
    val program: String = "",
    val service: String = "",
    val user: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val filteredReport: IncomeReportDto?
        get() = report?.filtered(
            DashboardReportFilter(program = program, service = service, user = user),
        )
}

class TreasuryDashboardViewModel(private val repository: TreasuryRepository) : ViewModel() {
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
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
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
    val programs: List<TreasuryProgramDto> = emptyList(),
    val services: List<TreasuryServiceDto> = emptyList(),
    val shifts: List<ShiftDto> = emptyList(),
    val period: ActivePeriodDto? = null,
    val registers: List<EnrollmentRegisterDto> = emptyList(),
    val configuration: InstitutionConfigDto? = null,
    val dni: String = "",
    val student: StudentLookupDto? = null,
    val selectedStudentId: Int? = null,
    val loadingCatalogs: Boolean = false,
    val searching: Boolean = false,
    val registering: Boolean = false,
    val emitting: Boolean = false,
    val emittedTicket: TicketDto? = null,
    val message: String? = null,
    val error: String? = null,
)

class CashierViewModel(private val repository: TreasuryRepository) : ViewModel() {
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
            .onFailure { _state.value = _state.value.copy(loadingCatalogs = false, error = it.userMessage()) }
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
                .onFailure { _state.value = _state.value.copy(searching = false, error = it.userMessage()) }
        }
    }

    private fun applyStudent(student: StudentLookupDto) {
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

    fun registerStudent(request: CreateStudentRequest) = viewModelScope.launch {
        if (_state.value.registering) return@launch
        _state.value = _state.value.copy(registering = true, error = null, message = null)
        runCatching { repository.createStudent(request) }
            .onSuccess {
                applyStudent(it)
                _state.value = _state.value.copy(registering = false, message = "Estudiante registrado correctamente.")
            }
            .onFailure { _state.value = _state.value.copy(registering = false, error = it.userMessage()) }
    }

    fun emitTicket(lines: List<EmitTicketDetailRequest>) = viewModelScope.launch {
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
                            error = "${it.userMessage()} Si la conexion se corto al emitir, revise Historial antes de intentarlo otra vez.",
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
    val items: List<TicketListItemDto> = emptyList(),
    val detail: TicketDto? = null,
    val configuration: InstitutionConfigDto? = null,
    val loading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null,
)

class TicketHistoryViewModel(private val repository: TreasuryRepository) : ViewModel() {
    private val _state = MutableStateFlow(TicketHistoryUiState())
    val state = _state.asStateFlow()
    init { load(null, null, null) }

    fun load(from: String?, to: String?, dni: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, detail = null)
        runCatching { repository.tickets(from, to, dni) }
            .onSuccess { _state.value = _state.value.copy(items = it, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
    }

    fun open(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(detailLoading = true, error = null)
        runCatching {
            val ticket = repository.ticket(id)
            val config = runCatching { repository.configuration() }.getOrNull()
            ticket to config
        }.onSuccess { (ticket, config) ->
            _state.value = _state.value.copy(detail = ticket, configuration = config, detailLoading = false)
        }.onFailure { _state.value = _state.value.copy(detailLoading = false, error = it.userMessage()) }
    }

    fun closeDetail() { _state.value = _state.value.copy(detail = null) }
}

data class IncomeReportUiState(
    val report: IncomeReportDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class IncomeReportViewModel(private val repository: TreasuryRepository) : ViewModel() {
    private val _state = MutableStateFlow(IncomeReportUiState())
    val state = _state.asStateFlow()
    fun load(from: String?, to: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.incomeReport(from, to) }
            .onSuccess { _state.value = IncomeReportUiState(report = it) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
    }
}

data class ExpenseBrowserUiState(
    val items: List<ExpenseListItemDto> = emptyList(),
    val detail: ExpenseDto? = null,
    val loading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null,
)

class ExpenseBrowserViewModel(private val repository: TreasuryRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseBrowserUiState())
    val state = _state.asStateFlow()

    fun load(from: String?, to: String?, category: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.expenses(from, to, category) }
            .onSuccess { _state.value = _state.value.copy(items = it, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
    }

    fun open(id: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(detailLoading = true, error = null)
        runCatching { repository.expense(id) }
            .onSuccess { _state.value = _state.value.copy(detail = it, detailLoading = false) }
            .onFailure { _state.value = _state.value.copy(detailLoading = false, error = it.userMessage()) }
    }
    fun close() { _state.value = _state.value.copy(detail = null) }
}

private fun Throwable.userMessage(): String = when (this) {
    is ClientRequestException -> when (response.status.value) {
        400 -> "El servidor rechazo los datos. Revise los campos e intentelo de nuevo."
        401 -> "La sesion vencio. Ingrese nuevamente."
        403 -> "Su usuario no tiene permiso para esta operacion."
        404 -> "El registro solicitado ya no existe."
        409 -> "La operacion entra en conflicto con un registro existente."
        else -> "Solicitud rechazada (${response.status.value})."
    }
    is ServerResponseException -> "El servidor no pudo completar la operacion (${response.status.value})."
    is HttpRequestTimeoutException -> "La consulta tardo demasiado. Revise su conexion."
    is IOException -> "No se pudo conectar con el sistema empresarial."
    else -> message?.takeIf { it.isNotBlank() } ?: "Ocurrio un error inesperado."
}
