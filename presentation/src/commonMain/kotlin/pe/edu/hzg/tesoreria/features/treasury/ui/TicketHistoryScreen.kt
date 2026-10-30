package pe.edu.hzg.tesoreria.features.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.TicketHistoryViewModel
import pe.edu.hzg.tesoreria.ui.components.GradientHero

@Composable
fun TicketHistoryScreen(viewModel: TicketHistoryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail
    val context = platformContext()
    if (detail != null) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                OutlinedButton(onClick = viewModel::closeDetail) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    Text("Volver al historial", Modifier.padding(start = 6.dp))
                }
            }
            item { TicketPreview(detail) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { TicketOutput.print(context, detail, state.configuration) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Print, contentDescription = null); Text("Imprimir", Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { TicketOutput.share(context, detail) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Share, contentDescription = null); Text("Compartir", Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        return
    }

    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val query = search.trim().lowercase()
    val visible = state.items.filter {
        query.isBlank() || it.numeroTicket.lowercase().contains(query) ||
            it.alumnoNombre.lowercase().contains(query) || it.dni.contains(query)
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { GradientHero("Historial de tickets", "Consulta, vuelve a imprimir o comparte cualquier ticket emitido.", trailing = { Icon(Icons.Rounded.History, null, tint = androidx.compose.ui.graphics.Color.White) }) }
        if (state.loading || state.detailLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { NoticeCard(state.error, error = true) }
        item {
            SectionCard("Filtros", subtitle = "Las fechas usan el formato AAAA-MM-DD") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(from, { from = it.take(10) }, Modifier.weight(1f), label = { Text("Desde") }, singleLine = true)
                    OutlinedTextField(to, { to = it.take(10) }, Modifier.weight(1f), label = { Text("Hasta") }, singleLine = true)
                }
                OutlinedTextField(
                    dni, { dni = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), label = { Text("DNI exacto (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                )
                Button(onClick = { viewModel.load(from.ifBlank { null }, to.ifBlank { null }, dni.ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FilterAlt, contentDescription = null); Text("Aplicar filtros", Modifier.padding(start = 6.dp))
                }
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Buscar en resultados") }, singleLine = true)
            }
        }
        if (!state.loading && visible.isEmpty()) item { NoticeCard("No hay tickets para los filtros elegidos.") }
        items(visible, key = { it.id }) { ticket ->
            SectionCard("Ticket ${ticket.numeroTicket}", modifier = Modifier.fillMaxWidth().clickable { viewModel.open(ticket.id) }, subtitle = shortDate(ticket.fechaEmision)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ticket.alumnoNombre, style = MaterialTheme.typography.bodyMedium)
                        Text("DNI ${ticket.dni} · ${ticket.usuario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(ticket.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
