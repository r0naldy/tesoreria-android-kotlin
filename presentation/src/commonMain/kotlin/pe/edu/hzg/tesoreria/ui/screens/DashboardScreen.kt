package pe.edu.hzg.tesoreria.ui.screens

import pe.edu.hzg.tesoreria.domain.util.decimal
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.features.treasury.domain.model.CountItem
import pe.edu.hzg.tesoreria.features.treasury.domain.dashboardSummary
import pe.edu.hzg.tesoreria.features.treasury.presentation.TreasuryDashboardViewModel
import pe.edu.hzg.tesoreria.features.treasury.ui.DashboardOutput
import pe.edu.hzg.tesoreria.features.treasury.ui.NoticeCard
import pe.edu.hzg.tesoreria.features.treasury.ui.SectionCard
import pe.edu.hzg.tesoreria.features.treasury.ui.money
import pe.edu.hzg.tesoreria.features.treasury.ui.shortDate
import pe.edu.hzg.tesoreria.ui.components.GradientHero
import pe.edu.hzg.tesoreria.ui.components.MetricCard

@Composable
fun DashboardScreen(session: AuthSession, viewModel: TreasuryDashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = platformContext()
    val dashboard = state.data
    val filteredReport = remember(state.report, state.program, state.service, state.user) {
        state.filteredReport
    }
    val printableSummary = remember(dashboard, state.period, filteredReport) {
        if (dashboard != null && filteredReport != null) {
            dashboardSummary(dashboard, state.period, filteredReport)
        } else {
            null
        }
    }
    var from by rememberSaveable { mutableStateOf(state.from) }
    var to by rememberSaveable { mutableStateOf(state.to) }
    val programs = remember(state.report) {
        state.report?.items.orEmpty().map { it.programa }.filter(String::isNotBlank).distinct().sorted()
    }
    val services = remember(state.report) {
        state.report?.items.orEmpty().map { it.servicio }.filter(String::isNotBlank).distinct().sorted()
    }
    val users = remember(state.report) {
        state.report?.items.orEmpty().map { it.usuario }.filter(String::isNotBlank).distinct().sorted()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GradientHero(
                title = "¡Hola, ${session.nombreCompleto.ifBlank { session.username }}!",
                subtitle = "Resumen real de la caja, el periodo activo y los cobros registrados.",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Panel de Tesorería", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Dashboard y reporte obtenidos de la API empresarial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar panel")
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        item { NoticeCard(state.error, error = true) }
        if (dashboard != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Recaudado hoy",
                        value = money(dashboard.totalRecaudadoHoy),
                        icon = Icons.Rounded.Today,
                        color = Color(0xFF6B40F5),
                        previousLabel = "Ayer",
                        previousValue = money(dashboard.totalRecaudadoAyer),
                        current = dashboard.totalRecaudadoHoy,
                        previous = dashboard.totalRecaudadoAyer,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetricCard(
                        title = "Tickets hoy",
                        value = dashboard.ticketsHoy.toString(),
                        icon = Icons.Rounded.ConfirmationNumber,
                        color = Color(0xFF2879E8),
                        previousLabel = "Ayer",
                        previousValue = dashboard.ticketsAyer.toString(),
                        current = dashboard.ticketsHoy.toDouble(),
                        previous = dashboard.ticketsAyer.toDouble(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Recaudado este mes",
                        value = money(dashboard.totalRecaudadoMes),
                        icon = Icons.Rounded.Payments,
                        color = Color(0xFF12A675),
                        previousLabel = "Mes pasado",
                        previousValue = money(dashboard.totalRecaudadoMesPasado),
                        current = dashboard.totalRecaudadoMes,
                        previous = dashboard.totalRecaudadoMesPasado,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetricCard(
                        title = "Tickets este mes",
                        value = dashboard.ticketsMes.toString(),
                        icon = Icons.Rounded.ConfirmationNumber,
                        color = Color(0xFFF06A75),
                        previousLabel = "Mes pasado",
                        previousValue = dashboard.ticketsMesPasado.toString(),
                        current = dashboard.ticketsMes.toDouble(),
                        previous = dashboard.ticketsMesPasado.toDouble(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                MetricCard(
                    "Programas activos",
                    dashboard.programasActivos.toString(),
                    Icons.Rounded.School,
                    Color(0xFFF29A27),
                    Modifier.fillMaxWidth(),
                )
            }

            state.period?.takeIf { it.hayPeriodoActivo }?.let { period ->
                item {
                    SectionCard(
                        title = "Gestión financiera · ${period.nombre.orEmpty()}",
                        subtitle = "${shortDate(period.fechaInicio)} al ${shortDate(period.fechaFin)} · ${period.diasRestantes} días restantes",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PeriodMetric("Ingresos", money(period.ingresosAcumulados), Modifier.weight(1f))
                            PeriodMetric("Tickets", period.tickets.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PeriodMetric("Estudiantes", period.estudiantes.toString(), Modifier.weight(1f))
                            PeriodMetric("Servicios", period.servicios.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "Analizar movimientos",
                    subtitle = "La API filtra el rango; programa, servicio y usuario se aplican sobre ese resultado.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = from,
                            onValueChange = { from = it.take(10) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Desde") },
                            placeholder = { Text("AAAA-MM-DD") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = to,
                            onValueChange = { to = it.take(10) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Hasta") },
                            placeholder = { Text("AAAA-MM-DD") },
                            singleLine = true,
                        )
                    }
                    Button(
                        onClick = { viewModel.loadRange(from, to) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.FilterAlt, contentDescription = null)
                        Text("Consultar rango", Modifier.padding(start = 7.dp))
                    }
                    DashboardChoiceField("Programa", state.program, programs, viewModel::setProgram)
                    DashboardChoiceField("Servicio", state.service, services, viewModel::setService)
                    DashboardChoiceField("Usuario", state.user, users, viewModel::setUser)
                    if (state.program.isNotBlank() || state.service.isNotBlank() || state.user.isNotBlank()) {
                        OutlinedButton(onClick = viewModel::clearFilters, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Text("Limpiar filtros", Modifier.padding(start = 7.dp))
                        }
                    }
                }
            }

            if (filteredReport != null) {
                item {
                    SectionCard(
                        title = "Resultado filtrado",
                        subtitle = "${shortDate(filteredReport.desde)} al ${shortDate(filteredReport.hasta)}",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PeriodMetric("Total", money(filteredReport.total), Modifier.weight(1f))
                            PeriodMetric("Tickets", filteredReport.cantidadTickets.toString(), Modifier.weight(1f))
                        }
                        PeriodMetric("Servicios cobrados", filteredReport.cantidadServicios.toString(), Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { printableSummary?.let { DashboardOutput.share(context, it) } },
                                enabled = printableSummary != null,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Text("Compartir", Modifier.padding(start = 5.dp))
                            }
                            OutlinedButton(
                                onClick = { printableSummary?.let { DashboardOutput.print(context, it) } },
                                enabled = printableSummary != null,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null)
                                Text("Imprimir", Modifier.padding(start = 5.dp))
                            }
                        }
                        Button(
                            onClick = { DashboardOutput.exportCsv(context, filteredReport) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Text("Exportar CSV", Modifier.padding(start = 7.dp))
                        }
                        if (filteredReport.items.isEmpty()) {
                            Text("No hay movimientos para los filtros elegidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Últimos movimientos del resultado", style = MaterialTheme.typography.titleSmall)
                            filteredReport.items.takeLast(5).reversed().forEach { movement ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(movement.servicio, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                        Text(
                                            "${movement.numeroTicket} · ${movement.programa} · ${movement.usuario}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    Text(money(movement.importe), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (dashboard.ingresosDiarios.isNotEmpty()) item {
                SectionCard("Ingresos de los últimos 30 días", subtitle = "Cada barra representa lo recaudado por fecha") {
                    val max = dashboard.ingresosDiarios.maxOfOrNull { it.monto }?.coerceAtLeast(1.0) ?: 1.0
                    Row(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        dashboard.ingresosDiarios.forEach { point ->
                            Box(
                                modifier = Modifier.weight(1f)
                                    .height((12 + point.monto / max * 110).dp)
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF4B8CF7))),
                                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                                    ),
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Ranking("Servicios más cobrados", dashboard.serviciosMasCobrados, Modifier.weight(1f))
                    Ranking("Programas con más pagos", dashboard.programasConMasPagos, Modifier.weight(1f))
                }
            }
            item {
                SectionCard("Tickets recientes") {
                    if (dashboard.ultimosTickets.isEmpty()) {
                        Text("Aún no hay tickets registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    dashboard.ultimosTickets.forEach { ticket ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ticket.alumnoNombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${ticket.numeroTicket} · ${shortDate(ticket.fechaEmision)} · DNI ${ticket.dni}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${ticket.usuario} · ${ticket.estado}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(money(ticket.total), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardChoiceField(
    label: String,
    selected: String,
    values: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = values.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected.ifBlank { "Todos" }, Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = { expanded = false; onSelect("") },
                )
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = { expanded = false; onSelect(value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = .07f), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    previousLabel: String,
    previousValue: String,
    current: Double,
    previous: Double,
    modifier: Modifier = Modifier,
) {
    val change = percentageChange(current, previous)
    val improving = (change ?: current) >= 0.0 && current >= previous
    val changeColor = when {
        current == previous -> MaterialTheme.colorScheme.onSurfaceVariant
        improving -> Color(0xFF0B8A5A)
        else -> MaterialTheme.colorScheme.error
    }
    val changeText = when {
        current == previous -> "Sin variación"
        change == null -> "Nuevo frente al período anterior"
        else -> "${if (change >= 0) "+" else ""}${decimal(change, 1)}% frente al período anterior"
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                modifier = Modifier.background(color.copy(alpha = .14f), RoundedCornerShape(14.dp)).padding(9.dp),
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$previousLabel: $previousValue",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(changeText, style = MaterialTheme.typography.labelSmall, color = changeColor)
        }
    }
}

private fun percentageChange(current: Double, previous: Double): Double? = when {
    previous == 0.0 && current == 0.0 -> 0.0
    previous == 0.0 -> null
    else -> ((current - previous) / previous) * 100.0
}

@Composable
private fun Ranking(title: String, items: List<CountItem>, modifier: Modifier = Modifier) {
    SectionCard(title, modifier = modifier) {
        if (items.isEmpty()) Text("Sin datos", style = MaterialTheme.typography.bodySmall)
        items.take(5).forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${index + 1}. ${item.nombre}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
                Text("${item.cantidad} · ${money(item.monto)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
