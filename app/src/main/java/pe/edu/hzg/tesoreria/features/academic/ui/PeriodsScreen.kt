package pe.edu.hzg.tesoreria.features.academic.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pe.edu.hzg.tesoreria.features.academic.AcademicDocumentOutput
import pe.edu.hzg.tesoreria.features.academic.data.PeriodDetailDto
import pe.edu.hzg.tesoreria.features.academic.data.PeriodDto
import pe.edu.hzg.tesoreria.features.academic.data.PeriodSummaryDto
import pe.edu.hzg.tesoreria.features.academic.presentation.PeriodsViewModel

@Composable
fun PeriodsScreen(
    isAdmin: Boolean,
    viewModel: PeriodsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showOpen by rememberSaveable { mutableStateOf(false) }
    var submittedRevision by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.mutationRevision) {
        if (submittedRevision?.let { state.mutationRevision > it } == true) {
            showOpen = false
            submittedRevision = null
        }
    }
    CollectAcademicShares(viewModel.shares)

    if (state.detail != null) {
        val detail = requireNotNull(state.detail)
        PeriodDetail(
            detail = detail,
            loading = state.loading || state.actionLoading,
            error = state.error,
            onBack = viewModel::clearDetail,
            onShareAct = { viewModel.shareClosingAct(detail.periodo) },
            onPrintAct = { AcademicDocumentOutput.printClosingAct(context, detail.periodo) },
        )
    } else {
        Scaffold(
            floatingActionButton = {
                if (isAdmin && state.active == null) {
                    ExtendedFloatingActionButton(
                        onClick = { if (!state.actionLoading && !state.loading) { viewModel.clearFeedback(); showOpen = true } },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Abrir año") },
                    )
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AcademicHeader(
                        title = "Períodos académicos",
                        subtitle = "Años de caja, semestres ASISTO e ingresos históricos",
                        icon = Icons.Rounded.CalendarMonth,
                        onRefresh = { viewModel.refresh() },
                    )
                }
                if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                state.error?.let { item { FeedbackCard(it, error = true) } }
                state.message?.let { item { FeedbackCard(it, error = false) } }
                state.active?.let { active ->
                    item { ActivePeriodCard(active) }
                }
                items(state.periods, key = { it.id }) { period ->
                    PeriodRow(
                        period = period,
                        isAdmin = isAdmin,
                        busy = state.actionLoading || state.loading,
                        onDetail = { viewModel.loadDetail(period.id) },
                        onClose = { viewModel.prepareClose(period.id) },
                    )
                }
                if (!state.loading && state.periods.isEmpty()) {
                    item {
                        Text(
                            "Todavía no existe ningún año de caja.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showOpen) {
        OpenPeriodDialog(
            busy = state.actionLoading,
            error = state.mutationError,
            onConfirm = { year, observations ->
                submittedRevision = state.mutationRevision
                viewModel.open(year, observations)
            },
            onDismiss = { showOpen = false; viewModel.clearFeedback() },
        )
    }

    state.closingSummary?.let { summary ->
        ClosePeriodDialog(
            summary = summary,
            busy = state.actionLoading,
            error = state.mutationError,
            onConfirm = viewModel::confirmClose,
            onDismiss = viewModel::cancelClose,
        )
    }

    state.lastClosedSummary?.let { summary ->
        ClosedPeriodSummaryDialog(
            summary = summary,
            busy = state.actionLoading,
            onShare = { viewModel.shareClosingAct(summary) },
            onPrint = { AcademicDocumentOutput.printClosingAct(context, summary) },
            onDismiss = viewModel::dismissClosedSummary,
        )
    }
}

@Composable
private fun ActivePeriodCard(period: PeriodDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Año abierto ${period.nombre}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        period.semestreActivo?.let { "Semestre activo: ${it.nombre}" } ?: "Sin semestre activo informado por ASISTO",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                StatusPill("Abierto", positive = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${period.tickets} tickets", style = MaterialTheme.typography.labelLarge)
                Text(money(period.ingresos), style = MaterialTheme.typography.labelLarge)
                Text("${period.diasRestantes ?: 0} días", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PeriodRow(
    period: PeriodDto,
    isAdmin: Boolean,
    busy: Boolean,
    onDetail: () -> Unit,
    onClose: () -> Unit,
) {
    val open = period.estado.equals("Abierto", true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(period.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${shortDate(period.fechaInicio)} – ${shortDate(period.fechaFin)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(period.estado, positive = open)
            }
            if (period.semestres.isNotEmpty()) {
                Text(
                    period.semestres.joinToString(" · ") { "${it.nombre}${if (it.cerrado) " cerrado" else ""}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDetail, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null)
                    Text(" Ingresos")
                }
                if (isAdmin && open) {
                    Button(onClick = onClose, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Text(" Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodDetail(
    detail: PeriodDetailDto,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onShareAct: () -> Unit,
    onPrintAct: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
                Column {
                    Text("Ingresos de ${detail.periodo.nombre}", style = MaterialTheme.typography.headlineSmall)
                    Text("Detalle y desgloses del período", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        error?.let { item { FeedbackCard(it, error = true) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetric("Ingresos", money(detail.total), Icons.Rounded.Payments, Modifier.weight(1f))
                DetailMetric("Tickets", detail.periodo.tickets.toString(), Icons.AutoMirrored.Rounded.ReceiptLong, Modifier.weight(1f))
            }
        }
        if (detail.periodo.estado.equals("Cerrado", true)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onShareAct, enabled = !loading, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Text(" Compartir acta")
                    }
                    OutlinedButton(onClick = onPrintAct, enabled = !loading, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Print, contentDescription = null)
                        Text(" Imprimir")
                    }
                }
            }
        }
        item { PeriodBreakdown("Ingresos por concepto", detail.resumenPorServicio) }
        item { PeriodBreakdown("Resumen por programa", detail.resumenPorPrograma) }
        item { PeriodBreakdown("Resumen por usuario", detail.resumenPorUsuario) }
        item { Text("Movimientos", style = MaterialTheme.typography.titleMedium) }
        items(detail.items) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.alumno, style = MaterialTheme.typography.titleSmall)
                    Text("${shortDate(item.fecha)} · ${item.numeroTicket} · ${item.servicio}")
                    Text("${item.programa} · ${money(item.importe)}", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun PeriodBreakdown(
    title: String,
    groups: List<pe.edu.hzg.tesoreria.features.academic.data.SummaryGroupDto>,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) {
                Text(
                    "Sin datos para este desglose.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                groups.forEach { group ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(group.nombre, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${group.cantidad} · ${money(group.monto)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OpenPeriodDialog(
    busy: Boolean,
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by rememberSaveable { mutableStateOf(LocalDateYear.current()) }
    var observations by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text("Abrir año de caja") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("El servidor lo abrirá del 1 de enero al 31 de diciembre. Solo puede existir uno abierto.")
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Año") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !busy,
                )
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observaciones") },
                    minLines = 2,
                    enabled = !busy,
                )
                error?.let { FeedbackCard(it, error = true) }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(year, observations) }, enabled = year.length == 4 && (year.toIntOrNull() ?: 0) > 0 && !busy) {
                Text(if (busy) "Abriendo…" else "Abrir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

private object LocalDateYear {
    fun current(): String = java.time.LocalDate.now().year.toString()
}

@Composable
private fun ClosePeriodDialog(
    summary: PeriodSummaryDto,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text("Cerrar definitivamente ${summary.nombre}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${summary.tickets} tickets")
                Text("${summary.estudiantes} estudiantes")
                Text("${summary.servicios} servicios")
                Text("Ingresos: ${money(summary.ingresos)}", style = MaterialTheme.typography.titleMedium)
                FeedbackCard("Esta acción no se puede deshacer.", error = true)
                error?.let { FeedbackCard(it, error = true) }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !busy) { Text(if (busy) "Cerrando…" else "Cerrar definitivamente") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun ClosedPeriodSummaryDialog(
    summary: PeriodSummaryDto,
    busy: Boolean,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Acta de cierre · ${summary.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackCard("El período quedó cerrado correctamente.", error = false)
                Text("Rango: ${shortDate(summary.fechaInicio)} – ${shortDate(summary.fechaFin)}")
                Text("Tickets emitidos: ${summary.tickets}")
                Text("Estudiantes atendidos: ${summary.estudiantes}")
                Text("Servicios cobrados: ${summary.servicios}")
                Text("Total recaudado: ${money(summary.ingresos)}", style = MaterialTheme.typography.titleMedium)
                summary.usuarioApertura?.takeIf(String::isNotBlank)?.let { Text("Abierto por: $it") }
                summary.usuarioCierre?.takeIf(String::isNotBlank)?.let { Text("Cerrado por: $it") }
                summary.fechaCierre?.let { Text("Fecha de cierre: ${shortDate(it)}") }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrint, enabled = !busy) {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Text(" Imprimir")
                }
                Button(onClick = onShare, enabled = !busy) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Text(" PDF")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}
