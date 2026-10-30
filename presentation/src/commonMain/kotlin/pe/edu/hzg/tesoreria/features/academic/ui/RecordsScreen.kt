package pe.edu.hzg.tesoreria.features.academic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.features.academic.AcademicDocumentOutput
import pe.edu.hzg.tesoreria.features.academic.domain.model.AcademicProgram
import pe.edu.hzg.tesoreria.features.academic.domain.model.CreateRecordCommand
import pe.edu.hzg.tesoreria.features.academic.domain.model.EnrollmentRoster
import pe.edu.hzg.tesoreria.features.academic.domain.model.Period
import pe.edu.hzg.tesoreria.features.academic.domain.model.Record
import pe.edu.hzg.tesoreria.features.academic.domain.model.Shift
import pe.edu.hzg.tesoreria.features.academic.domain.model.UpdateRecordCommand
import pe.edu.hzg.tesoreria.features.academic.domain.recordPersonalDataError
import pe.edu.hzg.tesoreria.features.academic.presentation.RecordsViewModel

@Composable
fun RecordsScreen(isAdmin: Boolean, viewModel: RecordsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = platformContext()
    var query by rememberSaveable { mutableStateOf("") }
    var shiftFilter by rememberSaveable { mutableStateOf("") }
    var sectionFilter by rememberSaveable { mutableStateOf("") }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Record?>(null) }
    var editing by remember { mutableStateOf<Record?>(null) }
    var submittedRevision by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.mutationRevision) {
        if (submittedRevision?.let { state.mutationRevision > it } == true) {
            showCreate = false
            deleting = null
            editing = null
            submittedRevision = null
        }
    }

    CollectAcademicShares(viewModel.shares)

    if (state.roster != null && state.selectedRecord != null) {
        val roster = requireNotNull(state.roster)
        val printableRoster = roster.copy(
            semestre = roster.semestre?.takeIf(String::isNotBlank) ?: state.selectedSemester,
        )
        RosterView(
            roster = roster,
            record = requireNotNull(state.selectedRecord),
            periods = state.periods,
            semester = state.selectedSemester,
            loading = state.loading || state.actionLoading,
            error = state.error,
            onSemester = viewModel::selectSemester,
            onExportExcel = viewModel::exportExcel,
            onExportPdf = viewModel::exportPdf,
            onPrint = { AcademicDocumentOutput.printRoster(context, printableRoster) },
            onBack = viewModel::closeRoster,
        )
    } else {
        val filtered = remember(state.records, query, shiftFilter, sectionFilter) {
            state.records.filter { record ->
                (query.isBlank() || record.programa.contains(query, true) || record.profesor.orEmpty().contains(query, true)) &&
                    (shiftFilter.isBlank() || record.turnoId.toString() == shiftFilter) &&
                    (sectionFilter.isBlank() || record.seccion == sectionFilter)
            }
        }
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { if (!state.loading && !state.actionLoading) { viewModel.clearFeedback(); showCreate = true } },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Crear registro") },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AcademicHeader(
                        title = "Registros de matrícula",
                        subtitle = "Aulas, secciones y padrones oficiales por semestre",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        onRefresh = { viewModel.refresh() },
                    )
                }
                item {
                    ChoiceField(
                        label = "Período",
                        value = state.selectedPeriodId?.toString().orEmpty(),
                        choices = state.periods.map { it.id.toString() to "${it.nombre}${if (it.estado == "Abierto") " · abierto" else ""}" },
                        onSelect = { viewModel.selectPeriod(it.toIntOrNull()) },
                        enabled = !state.loading && !state.actionLoading,
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar programa o docente") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceField(
                            label = "Turno",
                            value = shiftFilter,
                            choices = listOf("" to "Todos") + state.shifts.map { it.id.toString() to it.nombre },
                            onSelect = { shiftFilter = it },
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceField(
                            label = "Sección",
                            value = sectionFilter,
                            choices = listOf("" to "Todas") + listOf("U", "A", "B", "C", "D").map { it to it },
                            onSelect = { sectionFilter = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                state.error?.let { item { FeedbackCard(it, error = true) } }
                state.message?.let { item { FeedbackCard(it, error = false) } }
                items(filtered, key = { it.id }) { record ->
                    RecordRow(
                        record = record,
                        enabled = !state.loading && !state.actionLoading,
                        isAdmin = isAdmin,
                        canEdit = state.periods.any { it.id == record.periodoId && it.estado.equals("Abierto", true) },
                        onOpen = { viewModel.openRoster(record) },
                        onEdit = { viewModel.clearFeedback(); editing = record },
                        onDelete = { viewModel.clearFeedback(); deleting = record },
                    )
                }
                if (!state.loading && filtered.isEmpty()) {
                    item {
                        Text(
                            "No hay aulas que coincidan con los filtros.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateRecordDialog(
            periods = state.periods,
            programs = state.programs,
            shifts = state.shifts,
            defaultPeriod = state.selectedPeriodId,
            busy = state.actionLoading,
            error = state.mutationError,
            onConfirm = {
                submittedRevision = state.mutationRevision
                viewModel.create(it)
            },
            onDismiss = { showCreate = false; viewModel.clearFeedback() },
        )
    }

    editing?.let { record ->
        EditRecordDialog(
            record = record,
            busy = state.actionLoading,
            error = state.mutationError,
            onConfirm = { request ->
                submittedRevision = state.mutationRevision
                viewModel.update(request, isAdmin)
            },
            onDismiss = { editing = null; viewModel.clearFeedback() },
        )
    }

    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!state.actionLoading) { deleting = null; viewModel.clearFeedback() } },
            properties = DialogProperties(dismissOnBackPress = !state.actionLoading, dismissOnClickOutside = !state.actionLoading),
            title = { Text("Eliminar registro vacío") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Eliminar ${record.programa} · ${record.turno} · sección ${record.seccion}? El servidor rechazará la operación si conserva alumnos históricos.")
                    state.mutationError?.let { FeedbackCard(it, error = true) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        submittedRevision = state.mutationRevision
                        viewModel.delete(record)
                    },
                    enabled = !state.actionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(if (state.actionLoading) "Eliminando…" else "Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null; viewModel.clearFeedback() }, enabled = !state.actionLoading) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun RecordRow(record: Record, enabled: Boolean, isAdmin: Boolean, canEdit: Boolean,
    onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(record.programa, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${record.turno} · Sección ${record.seccion} · ${record.cantidad} estudiante(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    record.profesor ?: "Sin docente asignado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                if (isAdmin) {
                    IconButton(onClick = onEdit, enabled = enabled && canEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = if (canEdit) "Editar datos de ${record.programa}" else "No editable: período cerrado")
                    }
                }
                if (record.cantidad == 0) {
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRecordDialog(
    record: Record,
    busy: Boolean,
    error: String?,
    onConfirm: (UpdateRecordCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    var teacher by rememberSaveable(record.id) { mutableStateOf(record.profesor.orEmpty()) }
    var module by rememberSaveable(record.id) { mutableStateOf(record.moduloFormativo.orEmpty()) }
    var resolution by rememberSaveable(record.id) { mutableStateOf(record.resolucionPrograma.orEmpty()) }
    val request = UpdateRecordCommand(record.id, teacher, module, resolution)
    val localError = recordPersonalDataError(request)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        icon = { Icon(Icons.Rounded.Edit, null) },
        title = { Text("Editar datos del registro") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(record.programa, style = MaterialTheme.typography.titleMedium)
                Text("${record.periodo} · ${record.turno} · Sección ${record.seccion}", style = MaterialTheme.typography.bodySmall)
                Text("Edita los datos base del aula. El período, programa, turno y sección no cambian. Los datos particulares de cada semestre se gestionan en ASISTO y se conservan.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it }, enabled = !busy,
                    label = { Text("Docente base") }, modifier = Modifier.fillMaxWidth(),
                    isError = teacher.trim().length > 150,
                    supportingText = { Text("${teacher.trim().length}/150 caracteres") },
                )
                OutlinedTextField(
                    value = module, onValueChange = { module = it }, enabled = !busy,
                    label = { Text("Módulo formativo base") }, modifier = Modifier.fillMaxWidth(),
                    isError = module.trim().length > 200,
                    supportingText = { Text("${module.trim().length}/200 caracteres") },
                )
                OutlinedTextField(
                    value = resolution, onValueChange = { resolution = it }, enabled = !busy,
                    label = { Text("Resolución del programa") }, modifier = Modifier.fillMaxWidth(),
                    isError = resolution.trim().length > 150,
                    supportingText = { Text("${resolution.trim().length}/150 caracteres") },
                )
                Text("Dejar un campo vacío elimina ese dato base.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (localError ?: error)?.let { FeedbackCard(it, error = true) }
            }
        },
        confirmButton = {
            Button(onClick = { focusManager.clearFocus(); keyboard?.hide(); onConfirm(request) }, enabled = !busy && localError == null) {
                Text(if (busy) "Guardando…" else "Guardar cambios")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun RosterView(
    roster: EnrollmentRoster,
    record: Record,
    periods: List<Period>,
    semester: String?,
    loading: Boolean,
    error: String?,
    onSemester: (String?) -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onBack: () -> Unit,
) {
    val period = periods.firstOrNull { it.id == record.periodoId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(roster.programa, style = MaterialTheme.typography.headlineSmall)
                    Text("${roster.turno} · Sección ${roster.seccion} · ${roster.periodo}")
                }
            }
        }
        if (period?.semestres?.isNotEmpty() == true) {
            item {
                ChoiceField(
                    label = "Semestre del padrón",
                    value = semester.orEmpty(),
                    choices = period.semestres.map { it.nombre to it.nombre },
                    onSelect = { onSemester(it) },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onExportPdf, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Text(" Compartir PDF")
                }
                OutlinedButton(onClick = onPrint, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Text(" Imprimir")
                }
            }
        }
        item {
            OutlinedButton(onClick = onExportExcel, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Text(" Conservar / compartir XLSX oficial")
            }
        }
        if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        error?.let { item { FeedbackCard(it, error = true) } }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(roster.nombreCetpro.ifBlank { roster.nombreInstitucion }, style = MaterialTheme.typography.titleMedium)
                    Text("Docente: ${roster.profesor ?: "Sin asignar"}")
                    Text("Módulo: ${roster.moduloFormativo ?: "Sin asignar"}")
                    Text("${roster.cantidad} estudiantes · H: ${roster.hombres} · M: ${roster.mujeres}")
                }
            }
        }
        items(roster.estudiantes, key = { it.codigoMatricula }) { student ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(student.n.toString(), modifier = Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.apellidosNombres, style = MaterialTheme.typography.titleSmall)
                        Text("DNI ${student.codigoMatricula} · ${student.sexo ?: "—"} · ${student.edad ?: "—"} años")
                    }
                    StatusPill(student.condicion, positive = student.condicion != "P")
                }
            }
        }
    }
}

@Composable
private fun CreateRecordDialog(
    periods: List<Period>,
    programs: List<AcademicProgram>,
    shifts: List<Shift>,
    defaultPeriod: Int?,
    busy: Boolean,
    error: String?,
    onConfirm: (CreateRecordCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    var periodId by rememberSaveable { mutableStateOf(defaultPeriod?.toString().orEmpty()) }
    var programId by rememberSaveable { mutableStateOf("") }
    var shiftId by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("U") }
    var teacher by rememberSaveable { mutableStateOf("") }
    var module by rememberSaveable { mutableStateOf("") }
    val valid = programs.any { it.id.toString() == programId } && shifts.any { it.id.toString() == shiftId } &&
        periods.any { it.id.toString() == periodId }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text("Nuevo registro de matrícula") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { item { FeedbackCard(it, error = true) } }
                if (periods.isEmpty() || programs.isEmpty() || shifts.isEmpty()) {
                    item { FeedbackCard("Faltan períodos, programas o turnos. Actualiza los catálogos antes de crear el registro.", error = true) }
                }
                item {
                    ChoiceField(
                        label = "Período",
                        value = periodId,
                        choices = periods.map { it.id.toString() to it.nombre },
                        onSelect = { periodId = it },
                        enabled = !busy,
                    )
                }
                item {
                    ChoiceField(
                        label = "Programa",
                        value = programId,
                        choices = programs.map { it.id.toString() to it.nombre },
                        onSelect = { programId = it },
                        enabled = !busy,
                    )
                }
                item {
                    ChoiceField(
                        label = "Turno",
                        value = shiftId,
                        choices = shifts.map { it.id.toString() to it.nombre },
                        onSelect = { shiftId = it },
                        enabled = !busy,
                    )
                }
                item {
                    ChoiceField(
                        label = "Sección",
                        value = section,
                        choices = listOf("U", "A", "B", "C", "D").map { it to it },
                        onSelect = { section = it },
                        enabled = !busy,
                    )
                }
                item {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Docente inicial (opcional)") },
                        enabled = !busy,
                    )
                }
                item {
                    OutlinedTextField(
                        value = module,
                        onValueChange = { module = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Módulo formativo (opcional)") },
                        enabled = !busy,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CreateRecordCommand(
                            programaId = programId.toInt(),
                            turnoId = shiftId.toInt(),
                            seccion = section,
                            periodoId = periodId.toIntOrNull(),
                            profesor = teacher.trim().takeIf(String::isNotEmpty),
                            moduloFormativo = module.trim().takeIf(String::isNotEmpty),
                        ),
                    )
                },
                enabled = valid && !busy,
            ) { Text(if (busy) "Creando…" else "Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}
