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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pe.edu.hzg.tesoreria.features.academic.data.AcademicProgramDto
import pe.edu.hzg.tesoreria.features.academic.data.CreateStudentRequest
import pe.edu.hzg.tesoreria.features.academic.data.DniLookupDto
import pe.edu.hzg.tesoreria.features.academic.data.ShiftDto
import pe.edu.hzg.tesoreria.features.academic.data.StudentDto
import pe.edu.hzg.tesoreria.features.academic.data.WithdrawalRequest
import pe.edu.hzg.tesoreria.features.academic.data.UpdateStudentRequest
import pe.edu.hzg.tesoreria.features.academic.data.studentPersonalDataError
import pe.edu.hzg.tesoreria.features.academic.presentation.StudentsViewModel

@Composable
fun StudentsScreen(
    isAdmin: Boolean,
    viewModel: StudentsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDni by remember { mutableStateOf(false) }
    var withdrawing by remember { mutableStateOf<StudentDto?>(null) }
    var editing by remember { mutableStateOf<StudentDto?>(null) }
    var deleting by remember { mutableStateOf<StudentDto?>(null) }
    var observedRevision by remember { mutableStateOf(state.mutationRevision) }
    LaunchedEffect(state.mutationRevision) {
        if (state.mutationRevision != observedRevision) {
            showDni = false
            withdrawing = null
            editing = null
            deleting = null
            viewModel.select(null)
            observedRevision = state.mutationRevision
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!state.actionLoading) { viewModel.clearFeedback(); showDni = true } },
                icon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null) },
                text = { Text("Consultar DNI") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AcademicHeader(
                title = "Estudiantes",
                subtitle = "Padrón académico, matrícula, ratificación y retiros",
                icon = Icons.Rounded.School,
                onRefresh = viewModel::refresh,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null) },
                label = { Text("Buscar por DNI, nombre o apellido") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            state.error?.let { FeedbackCard(it, error = true) }
            state.message?.let { FeedbackCard(it, error = false) }
            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.students, key = { it.id }) { student ->
                    StudentRow(student = student, onClick = { viewModel.select(student) })
                }
                if (!state.loading && state.students.isEmpty()) {
                    item {
                        Text(
                            "No se encontraron estudiantes.",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showDni) {
        DniRegistrationDialog(
            dni = state.dni,
            lookup = state.lookup,
            lookupLoading = state.lookupLoading,
            actionLoading = state.actionLoading,
            error = state.error,
            programs = state.programs,
            shifts = state.shifts,
            onDniChanged = viewModel::onDniChanged,
            onRegister = viewModel::register,
            onDismiss = { if (!state.actionLoading) showDni = false },
        )
    }

    state.selected?.let { student ->
        StudentDetailDialog(
            student = student,
            activeSemester = state.activeSemester,
            isAdmin = isAdmin,
            actionLoading = state.actionLoading,
            error = state.error,
            onRatify = { viewModel.ratify(student) },
            onWithdraw = {
                viewModel.clearFeedback()
                viewModel.select(null)
                withdrawing = student
            },
            onEdit = { viewModel.clearFeedback(); viewModel.select(null); editing = student },
            onDelete = { viewModel.clearFeedback(); viewModel.select(null); deleting = student },
            onDismiss = { viewModel.select(null) },
        )
    }

    withdrawing?.let { student ->
        WithdrawalDialog(
            student = student,
            reasons = state.withdrawalReasons,
            busy = state.actionLoading,
            error = state.error,
            onConfirm = { viewModel.withdraw(student, it) },
            onDismiss = { if (!state.actionLoading) withdrawing = null },
        )
    }
    editing?.let { student ->
        EditStudentDialog(student, state.actionLoading, state.error, viewModel::updateStudent) {
            if (!state.actionLoading) editing = null
        }
    }
    deleting?.let { student ->
        AlertDialog(
            onDismissRequest = { if (!state.actionLoading) deleting = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar estudiante sin historial") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${student.nombreCompleto} · DNI ${student.dni}")
                Text("Solo se puede eliminar un alta sin matrícula ni movimientos. Si ya tiene historial, utiliza Retirar matrícula para conservar los comprobantes y el padrón.")
                state.error?.let { FeedbackCard(it, error = true) }
                if (state.actionLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            } },
            confirmButton = { TextButton(onClick = { viewModel.deleteStudent(student) }, enabled = !state.actionLoading) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }, enabled = !state.actionLoading) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StudentRow(student: StudentDto, onClick: () -> Unit) {
    val active = student.fechaRetiro == null
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(student.nombreCompleto, style = MaterialTheme.typography.titleSmall)
                Text(
                    "DNI ${student.dni} · ${student.programa}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${student.turno} · Sección ${student.seccion} · ${money(student.pagado)} pagado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(if (active) "Activo" else "Retirado", positive = active)
        }
    }
}

@Composable
private fun StudentDetailDialog(
    student: StudentDto,
    activeSemester: String?,
    isAdmin: Boolean,
    actionLoading: Boolean,
    error: String?,
    onRatify: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val withdrawn = student.fechaRetiro != null
    val countsInSemester = activeSemester != null &&
        (student.semestreDeRegistro == activeSemester || student.semestreRatificado == activeSemester)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.nombreCompleto) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { FeedbackCard(it, error = true) }
                Text("DNI ${student.dni}")
                Text("${student.programa} · ${student.turno} · Sección ${student.seccion}")
                Text("Nacimiento: ${shortDate(student.fechaNacimiento)} · Edad: ${student.edad ?: "—"}")
                Text("Celular: ${student.celular ?: "—"} · Pagado: ${money(student.pagado)}")
                Text("Semestre de registro: ${student.semestreDeRegistro.ifBlank { "—" }}")
                student.semestreRatificado?.let { Text("Ratificado para: $it") }
                student.fechaRetiro?.let {
                    FeedbackCard(
                        "Retirado el ${shortDate(it)}. ${student.motivoRetiro.orEmpty()}",
                        error = true,
                    )
                }
                if (!withdrawn && activeSemester != null && !countsInSemester) {
                    Button(onClick = onRatify, enabled = !actionLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Ratificar para $activeSemester")
                    }
                }
                if (isAdmin && !withdrawn) {
                    OutlinedButton(onClick = onWithdraw, enabled = !actionLoading, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PersonRemove, contentDescription = null)
                        Text(" Retirar matrícula")
                    }
                }
                if (isAdmin) {
                    OutlinedButton(onClick = onEdit, enabled = !actionLoading, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text(" Editar datos personales")
                    }
                    TextButton(onClick = onDelete, enabled = !actionLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Eliminar alta sin historial", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun DniRegistrationDialog(
    dni: String,
    lookup: DniLookupDto?,
    lookupLoading: Boolean,
    actionLoading: Boolean,
    error: String?,
    programs: List<AcademicProgramDto>,
    shifts: List<ShiftDto>,
    onDniChanged: (String) -> Unit,
    onRegister: (CreateStudentRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var newCareer by remember(lookup?.dni) { mutableStateOf(lookup?.existe == false) }
    var names by remember { mutableStateOf("") }
    var surnames by remember { mutableStateOf("") }
    var programId by remember { mutableStateOf("") }
    var shiftId by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("U") }
    var sex by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("G") }

    LaunchedEffect(lookup) {
        lookup?.let {
            names = it.nombres
            surnames = it.apellidos
            sex = it.sexo.orEmpty()
            birthDate = it.fechaNacimiento?.substringBefore('T').orEmpty()
            phone = it.celular.orEmpty()
            newCareer = !it.existe
        }
    }
    val availablePrograms = programs.filter { program ->
        lookup?.carreras.orEmpty().none { it.programaId == program.id }
    }
    val personalError = studentPersonalDataError(names, surnames, birthDate, phone)
    val valid = dni.length == 8 && lookup?.dni == dni && !lookupLoading && personalError == null &&
        availablePrograms.any { it.id.toString() == programId } && shifts.any { it.id.toString() == shiftId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consulta y matrícula por DNI") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { item { FeedbackCard(it, error = true) } }
                if (actionLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        value = dni,
                        onValueChange = onDniChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("DNI (8 dígitos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !actionLoading,
                        singleLine = true,
                    )
                }
                if (lookupLoading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                lookup?.let { result ->
                    item {
                        FeedbackCard(
                            message = when {
                                result.existe -> "Estudiante encontrado: ${result.nombreCompleto}."
                                result.encontradoReniec -> "Datos encontrados en RENIEC. Completa la matrícula."
                                else -> "No fue encontrado. Ingresa sus datos manualmente."
                            },
                            error = false,
                        )
                    }
                    if (result.existe && !newCareer) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                result.carreras.forEach {
                                    Text("• ${it.programa} · ${it.turno} · Sección ${it.seccion}")
                                }
                                if (availablePrograms.isNotEmpty()) {
                                    OutlinedButton(onClick = { newCareer = true }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Text(" Matricular en otra carrera")
                                    }
                                }
                            }
                        }
                    }
                    if (newCareer) {
                        item {
                            OutlinedTextField(
                                value = names,
                                onValueChange = { names = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nombres") },
                                singleLine = true,
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = surnames,
                                onValueChange = { surnames = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Apellidos") },
                                singleLine = true,
                            )
                        }
                        item {
                            ChoiceField(
                                label = "Programa",
                                value = programId,
                                choices = availablePrograms.map { it.id.toString() to it.nombre },
                                onSelect = { programId = it },
                            )
                        }
                        item {
                            ChoiceField(
                                label = "Turno",
                                value = shiftId,
                                choices = shifts.map { it.id.toString() to it.nombre },
                                onSelect = { shiftId = it },
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChoiceField(
                                    label = "Sección",
                                    value = section,
                                    choices = listOf("U", "A", "B", "C", "D").map { it to it },
                                    onSelect = { section = it },
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceField(
                                    label = "Sexo",
                                    value = sex,
                                    choices = listOf("" to "Sin registrar", "H" to "Masculino", "M" to "Femenino"),
                                    onSelect = { sex = it },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = birthDate,
                                onValueChange = { birthDate = it.take(10) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Fecha de nacimiento (AAAA-MM-DD)") },
                                singleLine = true,
                                isError = birthDate.isNotBlank() && personalError?.contains("fecha", ignoreCase = true) == true,
                                supportingText = { if (birthDate.isNotBlank() && personalError?.contains("fecha", ignoreCase = true) == true) Text(personalError) },
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it.filter(Char::isDigit).take(9) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Celular") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                            )
                        }
                        item {
                            ChoiceField(
                                label = "Condición",
                                value = condition,
                                choices = listOf("G" to "Gratuito", "P" to "Pagante", "B" to "Becado"),
                                onSelect = { condition = it },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (lookup != null && newCareer) {
                Button(
                    onClick = {
                        onRegister(
                            CreateStudentRequest(
                                dni = dni,
                                nombres = names.trim(),
                                apellidos = surnames.trim(),
                                programaId = programId.toInt(),
                                turnoId = shiftId.toInt(),
                                seccion = section,
                                sexo = sex.takeIf(String::isNotBlank),
                                fechaNacimiento = birthDate.takeIf(String::isNotBlank),
                                celular = phone.takeIf(String::isNotBlank),
                                condicion = condition,
                            ),
                        )
                    },
                    enabled = valid && !actionLoading,
                ) { Text("Registrar") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !actionLoading) { Text("Cerrar") } },
    )
}

@Composable
private fun WithdrawalDialog(
    student: StudentDto,
    reasons: List<String>,
    busy: Boolean,
    error: String?,
    onConfirm: (WithdrawalRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var refund by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Efectivo") }
    val parsed = amount.replace(',', '.').toDoubleOrNull()
    val validRefund = !refund || (parsed != null && parsed > 0 && parsed <= student.pagado)
    val valid = reason.isNotBlank() && detail.trim().length >= 10 && validRefund

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirar matrícula") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { FeedbackCard(it, error = true) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("${student.nombreCompleto} · ${student.programa}")
                ChoiceField(
                    label = "Motivo",
                    value = reason,
                    choices = reasons.map { it to it },
                    onSelect = { reason = it },
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it.take(400) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Explique qué ocurrió") },
                    minLines = 3,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Registrar devolución", modifier = Modifier.weight(1f))
                    Switch(checked = refund, onCheckedChange = { refund = it }, enabled = student.pagado > 0)
                }
                if (refund) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Monto (máximo ${money(student.pagado)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !validRefund,
                        singleLine = true,
                    )
                    ChoiceField(
                        label = "Método",
                        value = method,
                        choices = listOf("Efectivo", "Yape", "Plin", "Transferencia").map { it to it },
                        onSelect = { method = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        WithdrawalRequest(
                            motivo = reason,
                            detalle = detail.trim(),
                            devolucion = if (refund) parsed else null,
                            metodoDevolucion = if (refund) method else null,
                        ),
                    )
                },
                enabled = valid && !busy,
            ) { Text("Retirar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}
