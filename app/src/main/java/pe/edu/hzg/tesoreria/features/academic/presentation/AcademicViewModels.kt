package pe.edu.hzg.tesoreria.features.academic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.hzg.tesoreria.features.academic.AcademicExportManager
import pe.edu.hzg.tesoreria.features.academic.AcademicPdfDocuments
import pe.edu.hzg.tesoreria.features.academic.AcademicShareFile
import pe.edu.hzg.tesoreria.features.academic.academicMessage
import pe.edu.hzg.tesoreria.features.academic.data.AcademicProgramDto
import pe.edu.hzg.tesoreria.features.academic.data.AcademicRepository
import pe.edu.hzg.tesoreria.features.academic.data.ComparisonDto
import pe.edu.hzg.tesoreria.features.academic.data.CreateRecordRequest
import pe.edu.hzg.tesoreria.features.academic.data.CreateStudentRequest
import pe.edu.hzg.tesoreria.features.academic.data.DniLookupDto
import pe.edu.hzg.tesoreria.features.academic.data.EnrollmentRosterDto
import pe.edu.hzg.tesoreria.features.academic.data.OpenPeriodRequest
import pe.edu.hzg.tesoreria.features.academic.data.PeriodDetailDto
import pe.edu.hzg.tesoreria.features.academic.data.PeriodDto
import pe.edu.hzg.tesoreria.features.academic.data.PeriodSummaryDto
import pe.edu.hzg.tesoreria.features.academic.data.RecordDto
import pe.edu.hzg.tesoreria.features.academic.data.ShiftDto
import pe.edu.hzg.tesoreria.features.academic.data.StudentDto
import pe.edu.hzg.tesoreria.features.academic.data.WithdrawalRequest
import pe.edu.hzg.tesoreria.features.academic.data.UpdateStudentRequest
import pe.edu.hzg.tesoreria.features.academic.data.UpdateRecordRequest
import pe.edu.hzg.tesoreria.features.academic.data.recordPersonalDataError
import pe.edu.hzg.tesoreria.features.academic.data.studentPersonalDataError

data class StudentsUiState(
    val students: List<StudentDto> = emptyList(),
    val programs: List<AcademicProgramDto> = emptyList(),
    val shifts: List<ShiftDto> = emptyList(),
    val withdrawalReasons: List<String> = emptyList(),
    val activeSemester: String? = null,
    val query: String = "",
    val dni: String = "",
    val lookup: DniLookupDto? = null,
    val selected: StudentDto? = null,
    val loading: Boolean = false,
    val lookupLoading: Boolean = false,
    val actionLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val mutationRevision: Long = 0,
)

class StudentsViewModel(private val repository: AcademicRepository) : ViewModel() {
    private val _state = MutableStateFlow(StudentsUiState())
    val state: StateFlow<StudentsUiState> = _state.asStateFlow()
    private var queryJob: Job? = null
    private var dniJob: Job? = null
    private var listJob: Job? = null

    init {
        refresh()
        loadReferences()
    }

    fun refresh() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val students = repository.students(_state.value.query.takeIf(String::isNotBlank))
                _state.update { it.copy(students = students, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(loading = false, error = error.academicMessage()) }
            }
        }
    }

    fun onQueryChanged(value: String) {
        _state.update { it.copy(query = value) }
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(350)
            refresh()
        }
    }

    fun onDniChanged(value: String) {
        val clean = value.filter(Char::isDigit).take(8)
        _state.update { it.copy(dni = clean, lookup = null, error = null, lookupLoading = clean.length == 8) }
        dniJob?.cancel()
        if (clean.length == 8) {
            dniJob = viewModelScope.launch {
                delay(250)
                performDniLookup(clean)
            }
        }
    }

    fun lookupDni(dni: String = _state.value.dni) {
        if (dni.length != 8) return
        dniJob?.cancel()
        dniJob = viewModelScope.launch { performDniLookup(dni) }
    }

    private suspend fun performDniLookup(dni: String) {
            _state.update { it.copy(lookupLoading = true, error = null) }
            try {
                val result = repository.lookupDni(dni)
                _state.update { if (it.dni == dni) it.copy(lookup = result, lookupLoading = false) else it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val message = error.academicMessage()
                _state.update { if (it.dni == dni) it.copy(lookupLoading = false, error = message) else it }
            }
    }

    fun select(student: StudentDto?) = _state.update { it.copy(selected = student) }

    fun updateStudent(request: UpdateStudentRequest) {
        val error = studentPersonalDataError(request.nombres, request.apellidos, request.fechaNacimiento.orEmpty(), request.celular.orEmpty())
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        criticalAction(success = { "Datos personales actualizados. La matrícula y su historial se conservan." }) {
            repository.updateStudent(request)
            _state.update { state ->
                state.copy(
                    selected = null,
                    students = state.students.map { student ->
                        if (student.dni == state.students.firstOrNull { it.id == request.id }?.dni) {
                            student.copy(nombres = request.nombres, apellidos = request.apellidos,
                                nombreCompleto = "${request.apellidos} ${request.nombres}", sexo = request.sexo,
                                fechaNacimiento = request.fechaNacimiento, celular = request.celular)
                        } else student
                    },
                )
            }
            val students = runCatching { repository.students(_state.value.query.takeIf(String::isNotBlank)) }.getOrNull()
            if (students != null) _state.update { it.copy(students = students) }
        }
    }

    fun deleteStudent(student: StudentDto) = criticalAction(success = { "Estudiante sin historial eliminado." }) {
        repository.deleteStudent(student.id)
        _state.update { it.copy(students = it.students.filterNot { item -> item.id == student.id }, selected = null) }
    }

    fun register(request: CreateStudentRequest) = criticalAction(
        success = { "Matrícula registrada correctamente." },
    ) {
        repository.createStudent(request)
        // El alta ya fue confirmada. Una falla posterior al refrescar nunca se
        // informa como falla del alta, pues podría inducir a repetir el POST.
        val refreshedLookup = runCatching { repository.lookupDni(request.dni) }.getOrNull()
        val students = runCatching {
            repository.students(_state.value.query.takeIf(String::isNotBlank))
        }.getOrElse { _state.value.students }
        _state.update { it.copy(lookup = refreshedLookup ?: it.lookup, students = students) }
    }

    fun ratify(student: StudentDto) = criticalAction(
        success = { semester -> "${student.nombreCompleto} quedó ratificado para $semester." },
    ) {
        val result = repository.ratifyStudent(student.id)
        val students = runCatching {
            repository.students(_state.value.query.takeIf(String::isNotBlank))
        }.getOrElse { _state.value.students }
        _state.update { it.copy(students = students) }
        result.semestre
    }

    fun withdraw(student: StudentDto, request: WithdrawalRequest) = criticalAction(
        success = { returned ->
            if (returned > 0) {
                "Matrícula retirada; se registró una devolución de S/ %.2f.".format(returned)
            } else {
                "Matrícula retirada. Su historial se conserva."
            }
        },
    ) {
        val result = repository.withdrawStudent(student.id, request)
        val students = runCatching {
            repository.students(_state.value.query.takeIf(String::isNotBlank))
        }.getOrElse { _state.value.students }
        _state.update {
            it.copy(
                selected = null,
                students = students,
            )
        }
        result.devuelto
    }

    fun clearFeedback() = _state.update { it.copy(error = null, message = null) }

    private fun loadReferences() {
        viewModelScope.launch {
            try {
                val programs = repository.programs()
                val shifts = repository.shifts()
                val reasons = repository.withdrawalReasons()
                val semester = repository.activePeriod()?.semestreActivo?.nombre
                _state.update {
                    it.copy(
                        programs = programs,
                        shifts = shifts,
                        withdrawalReasons = reasons,
                        activeSemester = semester,
                    )
                }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.academicMessage()) }
            }
        }
    }

    private fun <T> criticalAction(success: (T) -> String, block: suspend () -> T) {
        if (_state.value.actionLoading) return
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val value = block()
                _state.update { it.copy(actionLoading = false, message = success(value), mutationRevision = it.mutationRevision + 1) }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(actionLoading = false) }
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(actionLoading = false, error = error.academicMessage()) }
            }
        }
    }
}

data class PeriodsUiState(
    val periods: List<PeriodDto> = emptyList(),
    val active: PeriodDto? = null,
    val detail: PeriodDetailDto? = null,
    val closingSummary: PeriodSummaryDto? = null,
    val lastClosedSummary: PeriodSummaryDto? = null,
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val mutationRevision: Int = 0,
    val mutationError: String? = null,
)

class PeriodsViewModel(
    private val repository: AcademicRepository,
    private val exports: AcademicExportManager,
) : ViewModel() {
    private val _state = MutableStateFlow(PeriodsUiState())
    val state: StateFlow<PeriodsUiState> = _state.asStateFlow()
    private val _shares = MutableSharedFlow<AcademicShareFile>(extraBufferCapacity = 1)
    val shares: SharedFlow<AcademicShareFile> = _shares.asSharedFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        if (_state.value.loading || _state.value.actionLoading) return@launch
        _state.update { it.copy(loading = true, error = null) }
        try {
            val periods = repository.periods()
            _state.update {
                it.copy(
                    periods = periods,
                    active = periods.firstOrNull { item -> item.estado.equals("Abierto", true) },
                    loading = false,
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.academicMessage()) }
        }
    }

    fun loadDetail(id: Int) = viewModelScope.launch {
        if (_state.value.loading || _state.value.actionLoading) return@launch
        _state.update { it.copy(loading = true, error = null) }
        try {
            val detail = repository.periodDetail(id)
            _state.update { it.copy(detail = detail, loading = false) }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.academicMessage()) }
        }
    }

    fun clearDetail() = _state.update { it.copy(detail = null) }

    fun open(year: String, observations: String?) = criticalAction {
        require(year.length == 4 && year.all(Char::isDigit) && year.toInt() in 1..9999) { "Escribe un año válido de cuatro dígitos." }
        repository.openPeriod(OpenPeriodRequest(year, observaciones = observations?.takeIf(String::isNotBlank)))
        "Año $year abierto correctamente."
    }

    fun prepareClose(id: Int) = viewModelScope.launch {
        if (_state.value.actionLoading || _state.value.loading) return@launch
        _state.update { it.copy(actionLoading = true, error = null, mutationError = null) }
        try {
            val summary = repository.periodSummary(id)
            _state.update { it.copy(closingSummary = summary, actionLoading = false) }
        } catch (error: Throwable) {
            _state.update { it.copy(actionLoading = false, error = error.academicMessage()) }
        }
    }

    fun cancelClose() {
        if (!_state.value.actionLoading) _state.update { it.copy(closingSummary = null, mutationError = null) }
    }

    fun dismissClosedSummary() = _state.update { it.copy(lastClosedSummary = null) }

    fun shareClosingAct(summary: PeriodSummaryDto) = viewModelScope.launch {
        if (_state.value.actionLoading) return@launch
        _state.update { it.copy(actionLoading = true, error = null) }
        try {
            val share = withContext(Dispatchers.IO) {
                exports.writeBytes(
                    fileName = AcademicPdfDocuments.closingActFileName(summary),
                    bytes = AcademicPdfDocuments.closingActBytes(summary),
                    mimeType = "application/pdf",
                    title = "Compartir acta de cierre ${summary.nombre}",
                )
            }
            _shares.emit(share)
            _state.update { it.copy(actionLoading = false) }
        } catch (error: Throwable) {
            _state.update { it.copy(actionLoading = false, error = error.academicMessage()) }
        }
    }

    fun confirmClose() {
        val id = _state.value.closingSummary?.id ?: return
        criticalAction {
            val summary = repository.closePeriod(id)
            _state.update {
                it.copy(
                    closingSummary = null, lastClosedSummary = summary, active = null,
                    periods = it.periods.map { period -> if (period.id == id) period.copy(estado = "Cerrado") else period },
                )
            }
            "Año ${summary.nombre} cerrado definitivamente."
        }
    }

    fun clearFeedback() = _state.update { it.copy(error = null, message = null, mutationError = null) }

    private fun criticalAction(block: suspend () -> String) = viewModelScope.launch {
        if (_state.value.actionLoading || _state.value.loading) return@launch
        _state.update { it.copy(actionLoading = true, error = null, message = null, mutationError = null) }
        try {
            val message = block()
            var refreshError: String? = null
            val periods = try { repository.periods() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Throwable) {
                    refreshError = "La operación se confirmó, pero no se pudo actualizar la lista. Pulsa Actualizar; no repitas la operación."
                    _state.value.periods
                }
            _state.update {
                it.copy(
                    periods = periods,
                    active = periods.firstOrNull { item -> item.estado.equals("Abierto", true) },
                    actionLoading = false,
                    message = message,
                    mutationRevision = it.mutationRevision + 1,
                    error = refreshError,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update { it.copy(actionLoading = false, error = error.academicMessage(), mutationError = error.academicMessage()) }
        }
    }
}

data class RecordsUiState(
    val records: List<RecordDto> = emptyList(),
    val periods: List<PeriodDto> = emptyList(),
    val programs: List<AcademicProgramDto> = emptyList(),
    val shifts: List<ShiftDto> = emptyList(),
    val selectedPeriodId: Int? = null,
    val selectedRecord: RecordDto? = null,
    val roster: EnrollmentRosterDto? = null,
    val selectedSemester: String? = null,
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val mutationRevision: Int = 0,
    val mutationError: String? = null,
)

class RecordsViewModel(
    private val repository: AcademicRepository,
    private val exports: AcademicExportManager,
) : ViewModel() {
    private val _state = MutableStateFlow(RecordsUiState())
    val state: StateFlow<RecordsUiState> = _state.asStateFlow()
    private val _shares = MutableSharedFlow<AcademicShareFile>(extraBufferCapacity = 1)
    val shares: SharedFlow<AcademicShareFile> = _shares.asSharedFlow()
    private var rosterJob: Job? = null

    init { loadInitial() }

    fun selectPeriod(id: Int?) {
        if (_state.value.loading || _state.value.actionLoading) return
        _state.update { it.copy(selectedPeriodId = id, selectedRecord = null, roster = null) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        if (_state.value.loading || _state.value.actionLoading) return@launch
        if (_state.value.periods.isEmpty() || _state.value.programs.isEmpty() || _state.value.shifts.isEmpty()) {
            loadInitial()
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val records = repository.records(_state.value.selectedPeriodId)
            _state.update {
                it.copy(
                    records = records,
                    loading = false,
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.academicMessage()) }
        }
    }

    fun openRoster(record: RecordDto, semester: String? = defaultSemester(record.periodoId)) {
        if (_state.value.loading || _state.value.actionLoading) return
        rosterJob = viewModelScope.launch {
            if (_state.value.loading || _state.value.actionLoading) return@launch
            _state.update { it.copy(loading = true, error = null) }
            try {
                val roster = repository.roster(record.id, semester)
                _state.update { it.copy(selectedRecord = record, selectedSemester = semester, roster = roster, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(loading = false, error = error.academicMessage()) }
            }
        }
    }

    fun selectSemester(value: String?) {
        val record = _state.value.selectedRecord ?: return
        openRoster(record, value)
    }

    fun closeRoster() {
        rosterJob?.cancel()
        _state.update { it.copy(selectedRecord = null, roster = null, selectedSemester = null, loading = false) }
    }

    fun create(request: CreateRecordRequest) = criticalAction("Registro de matrícula creado.") {
        require(request.programaId > 0 && request.turnoId > 0) { "Selecciona un programa y un turno válidos." }
        repository.createRecord(request)
        request.periodoId?.let { periodId -> _state.update { it.copy(selectedPeriodId = periodId) } }
    }

    fun delete(record: RecordDto) = criticalAction("Registro vacío eliminado.") {
        repository.deleteRecord(record.id)
        _state.update { it.copy(records = it.records.filterNot { item -> item.id == record.id }) }
    }

    fun update(request: UpdateRecordRequest, isAdmin: Boolean) = criticalAction("Datos base del registro actualizados.") {
        require(isAdmin) { "Solo un administrador puede editar los datos del registro." }
        recordPersonalDataError(request)?.let { throw IllegalArgumentException(it) }
        val current = _state.value.records.firstOrNull { it.id == request.id }
            ?: throw IllegalStateException("Actualiza la lista antes de editar este registro.")
        require(_state.value.periods.any { it.id == current.periodoId && it.estado.equals("Abierto", true) }) {
            "No se pueden editar registros de un período cerrado."
        }
        val normalized = request.copy(
            profesor = request.profesor?.trim(), moduloFormativo = request.moduloFormativo?.trim(),
            resolucionPrograma = request.resolucionPrograma?.trim(),
        )
        repository.updateRecord(normalized)
        _state.update {
            it.copy(records = it.records.map { record ->
                if (record.id == request.id) record.copy(
                    profesor = normalized.profesor?.takeIf(String::isNotBlank),
                    moduloFormativo = normalized.moduloFormativo?.takeIf(String::isNotBlank),
                    resolucionPrograma = normalized.resolucionPrograma?.takeIf(String::isNotBlank),
                ) else record
            })
        }
    }

    fun exportExcel() {
        if (_state.value.actionLoading || _state.value.loading) return
        val record = _state.value.selectedRecord ?: return
        val semester = _state.value.selectedSemester
        viewModelScope.launch {
            if (_state.value.actionLoading) return@launch
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                val bytes = repository.recordExcel(record.id, semester)
                val suffix = semester?.let { "_$it" }.orEmpty()
                val share = exports.writeBytes(
                    fileName = "registro_${record.programa}_${record.turno}_${record.seccion}$suffix.xlsx",
                    bytes = bytes,
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    title = "Compartir registro de matrícula",
                )
                _shares.emit(share)
                _state.update { it.copy(actionLoading = false) }
            } catch (error: Throwable) {
                _state.update { it.copy(actionLoading = false, error = error.academicMessage()) }
            }
        }
    }

    fun exportPdf() {
        if (_state.value.actionLoading || _state.value.loading) return
        val roster = _state.value.roster ?: return
        val printableRoster = roster.copy(
            semestre = roster.semestre?.takeIf(String::isNotBlank) ?: _state.value.selectedSemester,
        )
        viewModelScope.launch {
            if (_state.value.actionLoading) return@launch
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                val share = withContext(Dispatchers.IO) {
                    exports.writeBytes(
                        fileName = AcademicPdfDocuments.rosterFileName(printableRoster),
                        bytes = AcademicPdfDocuments.rosterBytes(printableRoster),
                        mimeType = "application/pdf",
                        title = "Compartir padrón de matrícula en PDF",
                    )
                }
                _shares.emit(share)
                _state.update { it.copy(actionLoading = false) }
            } catch (error: Throwable) {
                _state.update { it.copy(actionLoading = false, error = error.academicMessage()) }
            }
        }
    }

    fun clearFeedback() = _state.update { it.copy(error = null, message = null, mutationError = null) }

    private fun loadInitial() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        try {
            val periods = repository.periods()
            val selected = periods.firstOrNull { it.estado.equals("Abierto", true) }?.id
                ?: periods.firstOrNull()?.id
            val programs = repository.programs()
            val shifts = repository.shifts()
            val records = repository.records(selected)
            _state.update {
                it.copy(
                    periods = periods,
                    programs = programs,
                    shifts = shifts,
                    selectedPeriodId = selected,
                    records = records,
                    loading = false,
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.academicMessage()) }
        }
    }

    private fun defaultSemester(periodId: Int): String? = _state.value.periods
        .firstOrNull { it.id == periodId }
        ?.let { it.semestreActivo?.nombre ?: it.semestres.lastOrNull()?.nombre }

    private fun criticalAction(message: String, block: suspend () -> Unit) = viewModelScope.launch {
        if (_state.value.actionLoading || _state.value.loading) return@launch
        _state.update { it.copy(actionLoading = true, error = null, message = null, mutationError = null) }
        try {
            block()
            var refreshError: String? = null
            val records = try { repository.records(_state.value.selectedPeriodId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Throwable) {
                    refreshError = "La operación se confirmó, pero no se pudo actualizar la lista. Pulsa Actualizar; no repitas la operación."
                    _state.value.records
                }
            _state.update {
                it.copy(
                    records = records,
                    actionLoading = false,
                    message = message,
                    mutationRevision = it.mutationRevision + 1,
                    error = refreshError,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update { it.copy(actionLoading = false, error = error.academicMessage(), mutationError = error.academicMessage()) }
        }
    }
}

data class ComparisonUiState(
    val from: String = LocalDate.now().withDayOfMonth(1).toString(),
    val to: String = LocalDate.now().toString(),
    val data: ComparisonDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class ComparisonViewModel(
    private val repository: AcademicRepository,
    private val exports: AcademicExportManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ComparisonUiState())
    val state: StateFlow<ComparisonUiState> = _state.asStateFlow()
    private val _shares = MutableSharedFlow<AcademicShareFile>(extraBufferCapacity = 1)
    val shares: SharedFlow<AcademicShareFile> = _shares.asSharedFlow()

    init { load() }

    fun setFrom(value: String) = _state.update { it.copy(from = value.take(10)) }
    fun setTo(value: String) = _state.update { it.copy(to = value.take(10)) }

    fun load() = viewModelScope.launch {
        val current = _state.value
        if (!validDate(current.from) || !validDate(current.to)) {
            _state.update { it.copy(error = "Escribe las fechas como AAAA-MM-DD.") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            _state.update {
                it.copy(
                    data = repository.comparison(current.from, current.to),
                    loading = false,
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.academicMessage()) }
        }
    }

    fun exportCsv() {
        val current = _state.value
        val data = current.data ?: return
        viewModelScope.launch {
            val rows = buildList {
                add(listOf("Tipo", "Fecha", "Descripción", "Detalle", "Monto", "Usuario"))
                data.movimientos.forEach { movement ->
                    add(
                        listOf(
                            movement.tipo,
                            movement.fecha,
                            movement.descripcion,
                            movement.detalle,
                            "%.2f".format(movement.monto),
                            movement.usuario,
                        ),
                    )
                }
            }
            val csv = "\uFEFF" + rows.joinToString("\n") { row ->
                row.joinToString(",") { cell -> "\"${cell.replace("\"", "\"\"")}\"" }
            }
            _shares.emit(
                exports.writeText(
                    fileName = "comparativa_${current.from}_a_${current.to}.csv",
                    text = csv,
                    mimeType = "text/csv",
                    title = "Compartir comparativa",
                ),
            )
        }
    }

    private fun validDate(value: String): Boolean = runCatching { LocalDate.parse(value) }.isSuccess
}
