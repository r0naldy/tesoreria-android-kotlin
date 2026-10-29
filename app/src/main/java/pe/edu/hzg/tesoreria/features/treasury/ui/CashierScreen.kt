package pe.edu.hzg.tesoreria.features.treasury.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pe.edu.hzg.tesoreria.features.treasury.data.CreateStudentRequest
import pe.edu.hzg.tesoreria.features.treasury.data.EmitTicketDetailRequest
import pe.edu.hzg.tesoreria.features.treasury.data.StudentCareerDto
import pe.edu.hzg.tesoreria.features.treasury.data.TicketDto
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryProgramDto
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryServiceDto
import pe.edu.hzg.tesoreria.features.treasury.presentation.CashierViewModel
import pe.edu.hzg.tesoreria.ui.components.GradientHero

@Composable
fun CashierScreen(viewModel: CashierViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ticket = state.emittedTicket
    if (ticket != null) {
        EmittedTicketScreen(ticket = ticket, configuration = state.configuration, onNew = viewModel::newCharge)
        return
    }

    var names by remember { mutableStateOf("") }
    var surnames by remember { mutableStateOf("") }
    var programId by remember { mutableStateOf<Int?>(null) }
    var shiftId by remember { mutableStateOf<Int?>(null) }
    var section by remember { mutableStateOf("U") }
    var sex by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("G") }
    var anotherCareer by remember { mutableStateOf(false) }
    val amounts = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(state.student) {
        state.student?.let {
            names = it.nombres
            surnames = it.apellidos
            programId = it.programaId
            shiftId = it.turnoId
            section = it.seccion ?: "U"
            sex = it.sexo.orEmpty()
            birthDate = it.fechaNacimiento?.take(10).orEmpty()
            phone = it.celular.orEmpty()
            anotherCareer = false
        }
        amounts.clear()
    }

    val selectedLines = amounts.mapNotNull { (id, amount) ->
        amount.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let { EmitTicketDetailRequest(id, it) }
    }
    val total = selectedLines.sumOf { it.importe }
    val student = state.student
    val showRegistration = student != null && (!student.existe || anotherCareer)
    val existingPrograms = student?.carreras.orEmpty().map { it.programaId }.toSet()
    val availablePrograms = if (anotherCareer) state.programs.filterNot { it.id in existingPrograms } else state.programs
    val sections = (state.registers.filter { it.programaId == programId && it.turnoId == shiftId }.map { it.seccion } + listOf("U", "A", "B", "C", "D")).distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GradientHero(
                title = "Tesorería",
                subtitle = "Busca al estudiante, selecciona los servicios y emite el ticket.",
                trailing = { Icon(Icons.Rounded.Payments, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White) },
            )
        }
        if (state.loadingCatalogs || state.searching || state.registering || state.emitting) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        item { NoticeCard(state.error, error = true) }
        item { NoticeCard(state.message) }
        state.period?.let { period ->
            item {
                NoticeCard("Período activo: ${period.nombre}${period.diasRestantes?.let { " · $it días restantes" }.orEmpty()}")
            }
        } ?: item { NoticeCard("No hay período abierto. La emisión de tickets está bloqueada.", error = true) }

        item {
            SectionCard("Buscar estudiante", subtitle = "La búsqueda comienza automáticamente al completar los 8 dígitos") {
                OutlinedTextField(
                    value = state.dni,
                    onValueChange = viewModel::onDniChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DNI") },
                    leadingIcon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text(if (state.dni.length == 8) "Consulta completada" else "${state.dni.length}/8 dígitos") },
                )
                if (student != null) StudentSummary(student, state.selectedStudentId, viewModel::selectCareer)
                if (student?.existe == true && !anotherCareer) {
                    OutlinedButton(onClick = {
                        anotherCareer = true; programId = null; shiftId = null; section = "U"
                    }, enabled = state.programs.any { it.id !in existingPrograms }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Matricular en otra carrera", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        if (showRegistration) item {
            SectionCard(
                title = if (anotherCareer) "Nueva carrera del estudiante" else "Registrar estudiante",
                subtitle = if (student?.encontradoReniec == true) "Los nombres llegaron del proveedor de identidad; completa la matrícula." else "Completa los datos obligatorios.",
            ) {
                OutlinedTextField(names, { names = it }, Modifier.fillMaxWidth(), label = { Text("Nombres") }, singleLine = true)
                OutlinedTextField(surnames, { surnames = it }, Modifier.fillMaxWidth(), label = { Text("Apellidos") }, singleLine = true)
                Text("Programa", style = MaterialTheme.typography.labelLarge)
                CatalogSelector(availablePrograms, programId, { it.id }, { it.nombre }) { programId = it }
                Text("Turno", style = MaterialTheme.typography.labelLarge)
                CatalogSelector(state.shifts, shiftId, { it.id }, { it.nombre }) { shiftId = it }
                Text("Sección", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(sections, section) { section = it }
                Text("Sexo", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(listOf("H", "M"), sex) { sex = it }
                OutlinedTextField(birthDate, { birthDate = it.take(10) }, Modifier.fillMaxWidth(), label = { Text("Fecha de nacimiento (AAAA-MM-DD)") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, Modifier.fillMaxWidth(), label = { Text("Celular") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                Text("Condición", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(listOf("G", "P", "B"), condition) { condition = it }
                Button(
                    onClick = {
                        viewModel.registerStudent(
                            CreateStudentRequest(
                                dni = state.dni, nombres = names.trim(), apellidos = surnames.trim(),
                                programaId = requireNotNull(programId), turnoId = requireNotNull(shiftId),
                                seccion = section, sexo = sex.ifBlank { null },
                                fechaNacimiento = birthDate.ifBlank { null }, celular = phone.ifBlank { null }, condicion = condition,
                            ),
                        )
                    },
                    enabled = !state.registering && names.isNotBlank() && surnames.isNotBlank() && programId != null && shiftId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar estudiante") }
            }
        }

        if (state.selectedStudentId != null && !anotherCareer) {
            item {
                SectionCard("Seleccionar servicios", subtitle = "Puedes ajustar el importe antes de agregarlo al ticket") {
                    state.services.forEach { service ->
                        ServiceLine(service, amounts[service.id], onToggle = { selected ->
                            if (selected) amounts[service.id] = "%.2f".format(java.util.Locale.US, service.precio) else amounts.remove(service.id)
                        }, onAmount = { amounts[service.id] = it })
                    }
                }
            }
            item {
                SectionCard("Resumen del ticket") {
                    if (selectedLines.isEmpty()) Text("Selecciona uno o más servicios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    selectedLines.forEach { line ->
                        val service = state.services.firstOrNull { it.id == line.servicioId }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(service?.nombre.orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(money(line.importe), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL", style = MaterialTheme.typography.titleMedium)
                        Text(money(total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { viewModel.emitTicket(selectedLines) },
                        enabled = state.period != null && selectedLines.isNotEmpty() && !state.emitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                        Text(if (state.emitting) "Emitiendo…" else "Emitir ticket", modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StudentSummary(student: pe.edu.hzg.tesoreria.features.treasury.data.StudentLookupDto, selectedId: Int?, select: (Int) -> Unit) {
    NoticeCard(
        when {
            student.existe -> "Estudiante encontrado: ${student.nombreCompleto}"
            student.encontradoReniec -> "Identidad encontrada: ${student.nombreCompleto}"
            else -> "DNI no encontrado. Registra al estudiante manualmente."
        },
        error = !student.existe && !student.encontradoReniec,
    )
    if (student.carreras.size > 1) {
        Text("Elige la carrera a la que corresponde el pago", style = MaterialTheme.typography.labelLarge)
        student.carreras.forEach { career -> CareerChip(career, selectedId == career.alumnoId) { select(career.alumnoId) } }
    } else if (student.carreras.size == 1) {
        val career = student.carreras.first()
        Text("${career.programa} · ${career.turno} · Sección ${career.seccion}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CareerChip(career: StudentCareerDto, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${career.programa} · ${career.turno} · ${career.seccion}") },
        leadingIcon = if (selected) ({ Icon(Icons.Rounded.CheckCircle, contentDescription = null) }) else null,
    )
}

@Composable
private fun <T> CatalogSelector(items: List<T>, selected: Int?, id: (T) -> Int, label: (T) -> String, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(items, key = id) { item -> FilterChip(selected = selected == id(item), onClick = { onSelect(id(item)) }, label = { Text(label(item)) }) }
    }
}

@Composable
private fun ChoiceRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items.distinct()) { item -> FilterChip(selected = selected == item, onClick = { onSelect(item) }, label = { Text(item) }) }
    }
}

@Composable
private fun ServiceLine(service: TreasuryServiceDto, amount: String?, onToggle: (Boolean) -> Unit, onAmount: (String) -> Unit) {
    val selected = amount != null
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(selected, onCheckedChange = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(service.nombre, style = MaterialTheme.typography.bodyMedium)
            Text("Tarifa: ${money(service.precio)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) OutlinedTextField(
            value = amount.orEmpty(), onValueChange = { onAmount(it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(10)) },
            modifier = Modifier.fillMaxWidth(.34f), label = { Text("Importe") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        )
    }
}

@Composable
private fun EmittedTicketScreen(
    ticket: TicketDto,
    configuration: pe.edu.hzg.tesoreria.features.treasury.data.InstitutionConfigDto?,
    onNew: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { GradientHero("Ticket emitido", "La operación quedó registrada con el correlativo ${ticket.numeroTicket}.") }
        item { NoticeCard("Ticket emitido correctamente: ${ticket.numeroTicket}") }
        item { TicketPreview(ticket) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { TicketOutput.print(context, ticket, configuration) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Print, contentDescription = null); Text("Imprimir", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { TicketOutput.share(context, ticket) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Share, contentDescription = null); Text("Compartir", Modifier.padding(start = 6.dp))
                }
            }
        }
        item { OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, contentDescription = null); Text("Nuevo ticket", Modifier.padding(start = 6.dp)) } }
    }
}

@Composable
fun TicketPreview(ticket: TicketDto) {
    SectionCard("Ticket ${ticket.numeroTicket}", subtitle = "${ticket.alumnoNombre} · DNI ${ticket.dni}") {
        Text("${ticket.programa} · ${ticket.turno}", style = MaterialTheme.typography.bodyMedium)
        ticket.detalles.forEach { detail ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(detail.servicio, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(money(detail.importe), style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL", style = MaterialTheme.typography.titleMedium)
            Text(money(ticket.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text("SON: ${ticket.totalEnLetras}", style = MaterialTheme.typography.bodySmall)
        Text("${shortDate(ticket.fechaEmision)} · ${ticket.usuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
