package pe.edu.hzg.tesoreria.features.academic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.features.academic.domain.model.ComparisonDay
import pe.edu.hzg.tesoreria.features.academic.domain.model.ComparisonGroup
import pe.edu.hzg.tesoreria.features.academic.presentation.ComparisonViewModel

@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data
    CollectAcademicShares(viewModel.shares)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AcademicHeader(
                title = "Comparativa financiera",
                subtitle = "Ingresos de tickets frente a egresos registrados",
                icon = Icons.Rounded.Balance,
                onRefresh = viewModel::load,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.from,
                    onValueChange = viewModel::setFrom,
                    modifier = Modifier.weight(1f),
                    label = { Text("Desde") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.to,
                    onValueChange = viewModel::setTo,
                    modifier = Modifier.weight(1f),
                    label = { Text("Hasta") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::load, modifier = Modifier.weight(1f)) { Text("Aplicar rango") }
                OutlinedButton(
                    onClick = viewModel::exportCsv,
                    enabled = data != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(" CSV")
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        state.error?.let { item { FeedbackCard(it, error = true) } }
        data?.let { comparison ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparisonMetric(
                        label = "Ingresos",
                        value = money(comparison.totalIngresos),
                        detail = "${comparison.cantidadIngresos} tickets",
                        color = Color(0xFF149668),
                        modifier = Modifier.weight(1f),
                    )
                    ComparisonMetric(
                        label = "Egresos",
                        value = money(comparison.totalEgresos),
                        detail = "${comparison.cantidadEgresos} comprobantes",
                        color = Color(0xFFE54D66),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                ComparisonMetric(
                    label = "Balance",
                    value = money(comparison.balance),
                    detail = if (comparison.balance >= 0) "Superávit" else "Déficit",
                    color = if (comparison.balance >= 0) Color(0xFF5D4AE8) else Color(0xFFE54D66),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { SectionTitle("Ingresos y egresos por día") }
            items(comparison.serie, key = { it.fecha }) { day ->
                DailyBar(day)
            }
            item { SectionTitle("Ingresos por concepto") }
            items(comparison.ingresosPorConcepto, key = { "i-${it.nombre}" }) { group ->
                GroupRow(group, comparison.totalIngresos, Color(0xFF149668))
            }
            item { SectionTitle("Egresos por categoría") }
            items(comparison.egresosPorCategoria, key = { "e-${it.nombre}" }) { group ->
                GroupRow(group, comparison.totalEgresos, Color(0xFFE54D66))
            }
            item { SectionTitle("Últimos movimientos") }
            items(comparison.movimientos) { movement ->
                val income = movement.tipo.equals("Ingreso", true)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            if (income) Icons.Rounded.Payments else Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = if (income) Color(0xFF149668) else Color(0xFFE54D66),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movement.descripcion, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${shortDate(movement.fecha)} · ${movement.detalle} · ${movement.usuario}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            (if (income) "+" else "−") + money(movement.monto),
                            color = if (income) Color(0xFF149668) else Color(0xFFE54D66),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonMetric(
    label: String,
    value: String,
    detail: String,
    color: Color,
    modifier: Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, color = color, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DailyBar(day: ComparisonDay) {
    val max = maxOf(1.0, day.ingresos, day.egresos)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shortDate(day.fecha), style = MaterialTheme.typography.labelLarge)
            FinanceBar("Ingresos", day.ingresos, max, Color(0xFF149668))
            FinanceBar("Egresos", day.egresos, max, Color(0xFFE54D66))
        }
    }
}

@Composable
private fun FinanceBar(label: String, value: Double, max: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(62.dp), style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / max).toFloat().coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(50))
                    .padding(vertical = 4.dp),
            )
        }
        Text(money(value), modifier = Modifier.width(82.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GroupRow(group: ComparisonGroup, total: Double, color: Color) {
    val fraction = if (total > 0) (group.monto / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(group.nombre, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${group.cantidad} · ${money(group.monto)}", style = MaterialTheme.typography.labelMedium)
        }
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .background(color, RoundedCornerShape(50))
                    .padding(vertical = 4.dp),
            )
        }
    }
}
