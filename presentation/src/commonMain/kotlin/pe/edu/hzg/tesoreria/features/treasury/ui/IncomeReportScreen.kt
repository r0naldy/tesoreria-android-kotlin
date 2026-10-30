package pe.edu.hzg.tesoreria.features.treasury.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.IncomeReportViewModel
import pe.edu.hzg.tesoreria.ui.components.GradientHero
import pe.edu.hzg.tesoreria.ui.components.MetricCard

@Composable
fun IncomeReportScreen(viewModel: IncomeReportViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = platformContext()
    var from by remember { mutableStateOf(monthStartIso()) }
    var to by remember { mutableStateOf(todayIso()) }
    LaunchedEffect(Unit) { viewModel.load(from, to) }
    val report = state.report

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { GradientHero("Reporte de ingresos", "Detalle contable de servicios cobrados por fecha.", trailing = { Icon(Icons.Rounded.Assessment, null, tint = Color.White) }) }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { NoticeCard(state.error, error = true) }
        item {
            SectionCard("Período del informe") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(from, { from = it.take(10) }, Modifier.weight(1f), label = { Text("Desde") }, singleLine = true)
                    OutlinedTextField(to, { to = it.take(10) }, Modifier.weight(1f), label = { Text("Hasta") }, singleLine = true)
                }
                Button(onClick = { viewModel.load(from, to) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FilterAlt, null); Text("Consultar", Modifier.padding(start = 6.dp))
                }
            }
        }
        if (report != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Total", money(report.total), Icons.Rounded.Assessment, Color(0xFF6B40F5), Modifier.weight(1f))
                    MetricCard("Tickets", report.cantidadTickets.toString(), Icons.AutoMirrored.Rounded.ReceiptLong, Color(0xFF178F68), Modifier.weight(1f))
                }
            }
            item {
                MetricCard(
                    "Servicios cobrados",
                    report.cantidadServicios.toString(),
                    Icons.Rounded.Assessment,
                    Color(0xFF2879E8),
                    Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { IncomeReportOutput.shareSummary(context, report) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Text("Compartir", Modifier.padding(start = 6.dp))
                    }
                    Button(
                        onClick = { IncomeReportOutput.exportCsv(context, report) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Text("Exportar CSV", Modifier.padding(start = 6.dp))
                    }
                }
            }
            item {
                SectionCard("Resumen por servicio") {
                    report.resumenPorServicio.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text("${item.cantidad} cobro(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(money(item.monto), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item {
                SectionCard("Resumen por programa") {
                    if (report.resumenPorPrograma.isEmpty()) {
                        Text("Sin datos por programa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    report.resumenPorPrograma.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${item.cantidad} servicio(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(money(item.monto), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item { Text("Movimientos · ${report.items.size}", style = MaterialTheme.typography.titleMedium) }
            items(report.items) { item ->
                SectionCard("${item.numeroTicket} · ${item.servicio}", subtitle = shortDate(item.fecha)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.alumno, style = MaterialTheme.typography.bodyMedium)
                            Text("DNI ${item.dni} · ${item.programa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(item.importe), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
