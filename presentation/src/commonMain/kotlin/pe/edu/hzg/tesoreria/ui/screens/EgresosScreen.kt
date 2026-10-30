package pe.edu.hzg.tesoreria.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.domain.model.Comprobante
import pe.edu.hzg.tesoreria.domain.model.SyncStatus
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Expense
import pe.edu.hzg.tesoreria.features.treasury.domain.model.ExpenseListItem
import pe.edu.hzg.tesoreria.features.treasury.presentation.ExpenseBrowserViewModel
import pe.edu.hzg.tesoreria.features.treasury.ui.NoticeCard
import pe.edu.hzg.tesoreria.features.treasury.ui.SectionCard
import pe.edu.hzg.tesoreria.features.treasury.ui.money
import pe.edu.hzg.tesoreria.features.treasury.ui.monthStartIso
import pe.edu.hzg.tesoreria.features.treasury.ui.shortDate
import pe.edu.hzg.tesoreria.features.treasury.ui.todayIso
import pe.edu.hzg.tesoreria.presentation.EgresosViewModel
import pe.edu.hzg.tesoreria.ui.components.GradientHero
import pe.edu.hzg.tesoreria.ui.components.SyncBadge

private val expenseCategories = listOf("Todas", "Combustible", "Alimentacion", "Papeleria", "Materiales", "Servicios", "Movilidad", "Mantenimiento", "Tecnologia", "Limpieza", "Seguridad", "Salud", "Capacitacion", "Otros")

@Composable
fun EgresosScreen(
    viewModel: EgresosViewModel = koinViewModel(),
    browser: ExpenseBrowserViewModel = koinViewModel(),
) {
    val localItems by viewModel.items.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val captureState by viewModel.state.collectAsStateWithLifecycle()
    val remoteState by browser.state.collectAsStateWithLifecycle()
    var from by remember { mutableStateOf(monthStartIso()) }
    var to by remember { mutableStateOf(todayIso()) }
    var category by remember { mutableStateOf("Todas") }
    var search by remember { mutableStateOf("") }

    val capturePhoto = rememberReceiptCapture(viewModel::queuePhoto)
    fun loadRemote() = browser.load(from, to, category.takeUnless { it == "Todas" })
    LaunchedEffect(Unit) { loadRemote() }

    val query = search.trim().lowercase()
    val remoteItems = remoteState.items.filter { item ->
        query.isBlank() || listOf(item.proveedor, item.ruc, item.numeroComprobante, item.concepto, item.categoria)
            .any { it?.lowercase()?.contains(query) == true }
    }
    val pendingItems = localItems.filter { it.id == null || it.syncStatus != SyncStatus.SYNCED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GradientHero(
                title = "Egresos",
                subtitle = "Captura comprobantes y consulta la foto, los campos y cada concepto guardado.",
                trailing = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = Color.White) },
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SyncBadge(pending)
                Row {
                    IconButton(onClick = { viewModel.refresh(); loadRemote() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar") }
                    Button(onClick = viewModel::synchronize, enabled = !captureState.isLoading && pending > 0) {
                        Icon(Icons.Rounded.CloudSync, null); Text("Sincronizar", Modifier.padding(start = 5.dp))
                    }
                }
            }
        }
        if (captureState.isLoading || remoteState.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { NoticeCard(captureState.message, error = captureState.isError) }
        item { NoticeCard(remoteState.error, error = true) }
        item {
            Button(
                onClick = {
                    capturePhoto()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Rounded.CameraAlt, null); Text("Fotografiar comprobante", Modifier.padding(start = 7.dp)) }
        }
        item {
            SectionCard("Filtros de egresos") {
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Empresa, RUC, número o detalle") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(from, { from = it.take(10) }, Modifier.weight(1f), label = { Text("Desde") }, singleLine = true)
                    OutlinedTextField(to, { to = it.take(10) }, Modifier.weight(1f), label = { Text("Hasta") }, singleLine = true)
                }
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(expenseCategories) { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) }
                }
                Button(onClick = ::loadRemote, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FilterAlt, null); Text("Aplicar filtros", Modifier.padding(start = 6.dp))
                }
            }
        }
        if (pendingItems.isNotEmpty()) {
            item { Text("Pendientes de sincronización", style = MaterialTheme.typography.titleMedium) }
            items(pendingItems, key = { it.localId }) { item -> LocalExpenseCard(item) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Comprobantes registrados", style = MaterialTheme.typography.titleMedium)
                Text(money(remoteItems.sumOf { it.total }), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (!remoteState.loading && remoteItems.isEmpty()) item { NoticeCard("No hay egresos para los filtros elegidos.") }
        items(remoteItems, key = { it.id }) { item -> RemoteExpenseCard(item) { browser.open(item.id) } }
        item { Spacer(Modifier.height(24.dp)) }
    }

    remoteState.detail?.let { ExpenseDetailDialog(it, onClose = browser::close) }
}

@Composable
private fun RemoteExpenseCard(item: ExpenseListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.proveedor ?: "Proveedor no detectado", style = MaterialTheme.typography.titleMedium)
                    Text("RUC ${item.ruc ?: "-"} · ${item.tipoDocumento ?: "Documento"} ${item.numeroComprobante ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(money(item.total, item.moneda), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(item.concepto ?: "Sin detalle", style = MaterialTheme.typography.bodyMedium)
            Text("${item.categoria ?: "Otros"} · ${item.confianza}% confianza · ${shortDate(item.fechaEmision)}", style = MaterialTheme.typography.labelSmall, color = if (item.esDuplicadoProbable || item.confianza < 75) Color(0xFFB56A00) else Color(0xFF16865D))
        }
    }
}

@Composable
private fun LocalExpenseCard(item: Comprobante) {
    val requiresReview = item.syncError?.startsWith("REQUIERE_VERIFICACION_MANUAL") == true
    val status = when {
        requiresReview -> "Requiere verificación manual; no se reenviará automáticamente"
        item.syncStatus == SyncStatus.ERROR -> "Error de sincronización"
        item.syncStatus == SyncStatus.SYNCING -> "Sincronizando"
        else -> "Pendiente de sincronización"
    }
    SectionCard(item.proveedor ?: "Comprobante pendiente", subtitle = status) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.concepto ?: "Esperando OCR del servidor", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(money(item.total), style = MaterialTheme.typography.labelLarge)
        }
        if (requiresReview) {
            NoticeCard(
                "Comprueba en la lista del servidor si el egreso fue recibido antes de recuperarlo. La app lo bloqueó para evitar duplicados.",
                error = true,
            )
        }
    }
}

@Composable
private fun ExpenseDetailDialog(item: Expense, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Detalle del egreso", style = MaterialTheme.typography.titleLarge)
                            Text(item.proveedor ?: "Proveedor no detectado", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar") }
                    }
                }
                item { ReceiptImage(item) }
                item {
                    SectionCard("Datos leídos") {
                        Detail("RUC", item.ruc)
                        Detail("Documento", listOfNotNull(item.tipoDocumento, item.numeroComprobante).joinToString(" "))
                        Detail("Emisión", "${shortDate(item.fechaEmision)} ${item.horaEmision.orEmpty()}")
                        Detail("Subtotal", item.subtotal?.let { money(it, item.moneda) })
                        Detail("IGV", item.igv?.let { money(it, item.moneda) })
                        Detail("Total", money(item.total, item.moneda))
                        Detail("Categoría", item.categoria)
                        Detail("Concepto", item.concepto)
                        Detail("Método de pago", item.metodoPago)
                        Detail("Confianza", "${item.confianza}%")
                        Detail("Observaciones", item.observaciones)
                    }
                }
                item {
                    SectionCard("Conceptos · ${item.productos.size}") {
                        if (item.productos.isEmpty()) Text("No se detectaron líneas de detalle.")
                        item.productos.sortedBy { it.orden }.forEach { product ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${product.orden}. ${product.descripcion ?: "Sin descripción"}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                product.importe?.let { Text(money(it, item.moneda), style = MaterialTheme.typography.labelLarge) }
                            }
                            if (product.cantidad != null || product.precioUnitario != null) {
                                Text("Cantidad ${product.cantidad ?: "-"} · Unitario ${product.precioUnitario?.let { money(it, item.moneda) } ?: "-"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.takeIf(String::isNotBlank) ?: "No detectado", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReceiptImage(item: Expense) {
    val apiBaseUrl = org.koin.compose.koinInject<pe.edu.hzg.tesoreria.domain.model.ApiConfiguration>().baseUrl
    var bitmap by remember(item.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failed by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        bitmap = withContext(Dispatchers.IO) { loadReceiptBitmap(item, apiBaseUrl) }
        failed = bitmap == null
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(bitmap!!, contentDescription = "Foto del comprobante", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.ImageNotSupported, null, Modifier.size(38.dp)); Text("Foto no disponible") }
            else -> LinearProgressIndicator(Modifier.fillMaxWidth(.65f))
        }
    }
}


@Composable
internal expect fun rememberReceiptCapture(onCaptured: (String) -> Unit): () -> Unit
internal expect fun loadReceiptBitmap(item: Expense, apiBaseUrl: String): androidx.compose.ui.graphics.ImageBitmap?
