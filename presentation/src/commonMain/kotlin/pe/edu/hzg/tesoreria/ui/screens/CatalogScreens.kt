package pe.edu.hzg.tesoreria.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.model.Servicio
import pe.edu.hzg.tesoreria.presentation.CatalogUiState
import pe.edu.hzg.tesoreria.presentation.ProgramasViewModel
import pe.edu.hzg.tesoreria.presentation.ServiciosViewModel
import pe.edu.hzg.tesoreria.ui.theme.TesoreriaTheme

@Composable
fun ProgramasScreen(canEdit: Boolean, viewModel: ProgramasViewModel = koinViewModel()) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Programa?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Programa?>(null) }
    var discarding by remember { mutableStateOf<Programa?>(null) }
    var submittedRevision by remember { mutableStateOf<Int?>(null) }
    val rows = remember(allItems) {
        allItems.map { CatalogItem(it.id, it.nombre, it.activo, "Programa de estudio", pending = it.syncPending, syncError = it.syncError) }
    }
    LaunchedEffect(state.mutationRevision) {
        if (submittedRevision?.let { state.mutationRevision > it } == true) {
            creating = false; editing = null; deleting = null; discarding = null; submittedRevision = null
        }
    }
    CatalogContent(
        title = "Programas de estudio", subtitle = "Organiza las carreras y su disponibilidad para matrícula.",
        singular = "programa", icon = Icons.Rounded.School, rows = rows, state = state, canEdit = canEdit,
        dialogOpen = creating || editing != null || deleting != null || discarding != null,
        onCreate = { viewModel.clearMessage(); creating = true },
        onEdit = { id -> viewModel.clearMessage(); editing = allItems.find { it.id == id } },
        onDelete = { id -> viewModel.clearMessage(); deleting = allItems.find { it.id == id } },
        onRefresh = viewModel::refresh, onClearMessage = viewModel::clearMessage,
        onDiscard = { id -> viewModel.clearMessage(); discarding = allItems.find { it.id == id } },
    )
    if (creating || editing != null) {
        val item = editing
        CatalogEditor(
            title = if (item == null) "Nuevo programa" else "Editar programa",
            description = "Usa el nombre oficial de la carrera. Necesitas conexión para confirmar los cambios en el servidor.",
            initialName = item?.nombre.orEmpty(), initialActive = item?.activo ?: true,
            editing = item != null, withPrice = false, saving = state.isSaving, error = state.mutationError,
            onDismiss = { creating = false; editing = null; viewModel.clearMessage() },
            onSave = { name, _, active ->
                submittedRevision = state.mutationRevision
                if (item == null) viewModel.create(name) else viewModel.update(item, name, active)
            },
        )
    }
    deleting?.let { item ->
        DeleteDialog(item.nombre, state.isSaving, state.mutationError,
            onDismiss = { deleting = null; viewModel.clearMessage() },
            onConfirm = { submittedRevision = state.mutationRevision; viewModel.delete(item.id) })
    }
    discarding?.let { item ->
        DiscardPendingDialog(item.nombre, state.isSaving, state.mutationError,
            onDismiss = { discarding = null; viewModel.clearMessage() },
            onConfirm = { submittedRevision = state.mutationRevision; viewModel.discardPending(item.id) })
    }
}

@Composable
fun ServiciosScreen(canEdit: Boolean, viewModel: ServiciosViewModel = koinViewModel()) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Servicio?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Servicio?>(null) }
    var discarding by remember { mutableStateOf<Servicio?>(null) }
    var submittedRevision by remember { mutableStateOf<Int?>(null) }
    val rows = remember(allItems) {
        allItems.map { CatalogItem(it.id, it.nombre, it.activo, "Importe por servicio", formatCatalogPrice(it.precio), it.syncPending, it.syncError) }
    }
    LaunchedEffect(state.mutationRevision) {
        if (submittedRevision?.let { state.mutationRevision > it } == true) {
            creating = false; editing = null; deleting = null; discarding = null; submittedRevision = null
        }
    }
    CatalogContent(
        title = "Servicios", subtitle = "Administra los conceptos de pago y sus precios vigentes.",
        singular = "servicio", icon = Icons.Rounded.Inventory2, rows = rows, state = state, canEdit = canEdit,
        dialogOpen = creating || editing != null || deleting != null || discarding != null,
        onCreate = { viewModel.clearMessage(); creating = true },
        onEdit = { id -> viewModel.clearMessage(); editing = allItems.find { it.id == id } },
        onDelete = { id -> viewModel.clearMessage(); deleting = allItems.find { it.id == id } },
        onRefresh = viewModel::refresh, onClearMessage = viewModel::clearMessage,
        onDiscard = { id -> viewModel.clearMessage(); discarding = allItems.find { it.id == id } },
    )
    if (creating || editing != null) {
        val item = editing
        CatalogEditor(
            title = if (item == null) "Nuevo servicio" else "Editar servicio",
            description = "Este concepto estará disponible al emitir tickets. Necesitas conexión para confirmar los cambios.",
            initialName = item?.nombre.orEmpty(), initialActive = item?.activo ?: true,
            initialPrice = item?.precio?.let(::formatCatalogPriceInput).orEmpty(),
            editing = item != null, withPrice = true, saving = state.isSaving, error = state.mutationError,
            onDismiss = { creating = false; editing = null; viewModel.clearMessage() },
            onSave = { name, price, active ->
                submittedRevision = state.mutationRevision
                if (item == null) viewModel.create(name, price) else viewModel.update(item, name, price, active)
            },
        )
    }
    deleting?.let { item ->
        DeleteDialog(item.nombre, state.isSaving, state.mutationError,
            onDismiss = { deleting = null; viewModel.clearMessage() },
            onConfirm = { submittedRevision = state.mutationRevision; viewModel.delete(item.id) })
    }
    discarding?.let { item ->
        DiscardPendingDialog(item.nombre, state.isSaving, state.mutationError,
            onDismiss = { discarding = null; viewModel.clearMessage() },
            onConfirm = { submittedRevision = state.mutationRevision; viewModel.discardPending(item.id) })
    }
}

internal data class CatalogItem(val id: Int, val name: String, val active: Boolean, val detail: String,
    val price: String? = null, val pending: Boolean = false, val syncError: String? = null)

@Composable
internal fun CatalogContent(
    title: String, subtitle: String, singular: String, icon: ImageVector,
    rows: List<CatalogItem>, state: CatalogUiState, canEdit: Boolean, dialogOpen: Boolean,
    onCreate: () -> Unit, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit,
    onRefresh: () -> Unit, onClearMessage: () -> Unit,
    onDiscard: (Int) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CatalogStateFilter.ALL) }
    val visible = remember(rows, query, filter) {
        rows.filter { matchesCatalogQuery(it.name, query) && filter.includes(it.active) }
    }
    val activeCount = remember(rows) { rows.count { it.active } }
    val busy = state.isLoading || state.isSaving
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.widthIn(max = 1320.dp).fillMaxSize().testTag("catalog-grid"),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    CatalogHero(title, subtitle, singular, icon, canEdit, busy, onCreate, onRefresh)
                }
                if (!dialogOpen && !state.message.isNullOrBlank()) {
                    item(key = "feedback", span = { GridItemSpan(maxLineSpan) }) {
                        CatalogFeedback(state.message.orEmpty(), state.isError, onClearMessage)
                    }
                }
                item(key = "metrics", span = { GridItemSpan(maxLineSpan) }) { CatalogMetrics(rows.size, activeCount, rows.size - activeCount) }
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar $singular por nombre") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpiar búsqueda") }
                            },
                            singleLine = true, shape = RoundedCornerShape(16.dp),
                        )
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogStateFilter.entries.forEach { value ->
                                FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value.label) })
                            }
                        }
                        Text("${visible.size} de ${rows.size} ${if (singular == "programa") "programas" else "servicios"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (!canEdit) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Solo consulta · los cambios requieren un administrador", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (visible.isEmpty() && !state.isLoading) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        CatalogEmpty(query.isNotBlank() || filter != CatalogStateFilter.ALL, singular) { query = ""; filter = CatalogStateFilter.ALL }
                    }
                }
                items(visible, key = { it.id }, contentType = { "catalog-row" }) { item ->
                    CatalogRow(item, icon, canEdit, !busy, { onEdit(item.id) }, { onDelete(item.id) }, { onDiscard(item.id) })
                }
            }
        }
    }
}

@Composable
private fun CatalogHero(title: String, subtitle: String, singular: String, icon: ImageVector,
    canEdit: Boolean, busy: Boolean, onCreate: () -> Unit, onRefresh: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)),
            RoundedCornerShape(24.dp),
        ).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(icon, null, Modifier.padding(12.dp).size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (canEdit) {
                Button(onClick = onCreate, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Nuevo $singular")
                }
            }
            OutlinedButton(onClick = onRefresh, enabled = !busy, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Refresh, "Actualizar catálogo", Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CatalogMetrics(total: Int, active: Int, inactive: Int) {
    Surface(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Total" to total, "Activos" to active, "Inactivos" to inactive).forEachIndexed { index, metric ->
                Column(
                    Modifier.weight(1f).background(if (index == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(metric.first, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(metric.second.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(item: CatalogItem, icon: ImageVector, canEdit: Boolean, enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onDiscard: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Icon(icon, null, Modifier.padding(10.dp).size(22.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (item.active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (item.active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (item.active) "Activo" else "Inactivo", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                item.price?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary) }
            }
            if (item.pending || item.id < 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudUpload, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cambio local pendiente de confirmación", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!item.syncError.isNullOrBlank()) CatalogError(item.syncError)
            if (canEdit) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, enabled = enabled && !item.pending, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.Edit, "Editar ${item.name}", Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Editar")
                    }
                    TextButton(onClick = onDelete, enabled = enabled && !item.pending, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Rounded.DeleteOutline, "Eliminar ${item.name}", Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Eliminar")
                    }
                }
                if (item.pending) {
                    OutlinedButton(onClick = onDiscard, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Descartar borrador local")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscardPendingDialog(name: String, saving: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
        icon = { Icon(Icons.Rounded.Refresh, null) },
        title = { Text("¿Descartar el borrador local?", fontWeight = FontWeight.Medium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("Se consultará el servidor y se quitará únicamente tu propuesta pendiente de este dispositivo. No se eliminará ni modificará ningún registro del sistema empresarial. Si la creación ya llegó al servidor, aparecerá en el listado actualizado.")
                Text("Necesitas conexión. Si no se puede confirmar el catálogo, el borrador se conserva.", style = MaterialTheme.typography.bodySmall)
                if (!error.isNullOrBlank()) CatalogError(error)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !saving) {
                if (saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(if (saving) "Comprobando…" else "Descartar solo borrador")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Conservar borrador") } },
    )
}

@Composable
private fun CatalogEditor(
    title: String, description: String, initialName: String, initialActive: Boolean,
    editing: Boolean, withPrice: Boolean, saving: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (String, Double, Boolean) -> Unit, initialPrice: String = "",
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var price by rememberSaveable(initialPrice) { mutableStateOf(initialPrice) }
    var active by rememberSaveable(initialActive) { mutableStateOf(initialActive) }
    val validName = name.trim().isNotEmpty() && name.trim().length <= 200
    val parsedPrice = parseCatalogPrice(price)
    val priceError = withPrice && price.isNotBlank() && parsedPrice == null
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    CatalogFormDialog(
        title = title,
        icon = if (withPrice) Icons.Rounded.Inventory2 else Icons.Rounded.School,
        busy = saving,
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(), enabled = !saving, minLines = 2, maxLines = 4,
                    isError = name.trim().length > 200,
                    supportingText = { Text("${name.trim().length}/200 caracteres") }, shape = RoundedCornerShape(12.dp),
                )
                if (withPrice) {
                    OutlinedTextField(
                        value = price, onValueChange = { price = it }, label = { Text("Precio *") }, prefix = { Text("S/ ") },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving, isError = priceError,
                        supportingText = { Text(if (priceError) "Importe de 0 a 99 999 999,99, con hasta 2 decimales." else "Soles · punto o coma decimal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp),
                    )
                }
                if (editing) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(if (active) "Registro activo" else "Registro inactivo", style = MaterialTheme.typography.titleSmall)
                                Text("Desactivar conserva su historial.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = active, onCheckedChange = { active = it }, enabled = !saving)
                        }
                    }
                }
                if (!error.isNullOrBlank()) CatalogError(error)
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                keyboard?.hide()
                onSave(name.trim(), parsedPrice ?: 0.0, active)
            }, enabled = !saving && validName && (!withPrice || parsedPrice != null)) {
                if (saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(if (saving) "Guardando…" else if (editing) "Guardar cambios" else "Crear")
            }
        },
    )
}

/** Full-window inset handling keeps the fixed action row above the keyboard on phones and tablets. */
@Composable
private fun CatalogFormDialog(
    title: String,
    icon: ImageVector,
    busy: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = !busy,
            dismissOnClickOutside = !busy,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
                        Spacer(Modifier.weight(1f))
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogFeedback(message: String, error: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle, null, Modifier.size(22.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cerrar aviso") }
        }
    }
}

@Composable
private fun DeleteDialog(name: String, saving: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
        icon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("¿Eliminar este registro?", fontWeight = FontWeight.Medium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("Esta acción no se puede deshacer. Si tiene matrículas, tickets u otros movimientos asociados, el servidor impedirá eliminarlo. Puedes desactivarlo desde Editar para conservar el historial.")
                if (!error.isNullOrBlank()) CatalogError(error)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                if (saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(if (saving) "Eliminando…" else "Sí, eliminar")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Conservar") } },
    )
}

@Composable
private fun CatalogError(message: String, retry: (() -> Unit)? = null, enabled: Boolean = true) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(20.dp)); Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            retry?.let { TextButton(onClick = it, enabled = enabled) { Text("Reintentar") } }
        }
    }
}

@Composable
private fun CatalogEmpty(filtered: Boolean, singular: String, onClear: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (filtered) Icons.Rounded.SearchOff else Icons.Rounded.Inventory2, null, Modifier.size(36.dp), MaterialTheme.colorScheme.primary)
            Text(if (filtered) "No hay coincidencias" else "Todavía no hay ${singular}s", style = MaterialTheme.typography.titleMedium)
            Text(if (filtered) "Prueba otro nombre o cambia el estado." else "Actualiza el catálogo o crea el primer registro con una cuenta administradora.", style = MaterialTheme.typography.bodyMedium)
            if (filtered) TextButton(onClick = onClear) { Text("Quitar filtros") }
        }
    }
}
