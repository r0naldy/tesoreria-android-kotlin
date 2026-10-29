package pe.edu.hzg.tesoreria.admin.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pe.edu.hzg.tesoreria.admin.model.RolUsuario
import pe.edu.hzg.tesoreria.admin.model.UsuarioAdministrado
import pe.edu.hzg.tesoreria.admin.presentation.UsuariosViewModel

@Composable
fun UsuariosScreen(
    isAdmin: Boolean,
    currentUsername: String,
) {
    if (!isAdmin) {
        AdminAccessDenied("La administración de usuarios")
        return
    }
    UsuariosAdminContent(currentUsername = currentUsername)
}

@Composable
private fun UsuariosAdminContent(
    currentUsername: String,
    viewModel: UsuariosViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var createDialog by remember { mutableStateOf(false) }
    var toggleTarget by remember { mutableStateOf<UsuarioAdministrado?>(null) }
    var roleTarget by remember { mutableStateOf<UsuarioAdministrado?>(null) }
    var deleteTarget by remember { mutableStateOf<UsuarioAdministrado?>(null) }
    var editTarget by remember { mutableStateOf<UsuarioAdministrado?>(null) }
    var query by remember { mutableStateOf("") }
    var observedRevision by remember { mutableStateOf(state.mutationRevision) }

    LaunchedEffect(state.mutationRevision) {
        if (observedRevision != state.mutationRevision) {
            createDialog = false
            editTarget = null
            toggleTarget = null
            roleTarget = null
            deleteTarget = null
            observedRevision = state.mutationRevision
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it) }
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!state.isSaving) { viewModel.clearMutationError(); createDialog = true } },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nuevo usuario") },
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AdminHeader(
                    title = "Usuarios",
                    subtitle = "Cuentas, permisos y acceso al sistema",
                    icon = Icons.Rounded.Group,
                    onRefresh = viewModel::refresh,
                )
            }
            if (state.isLoading || state.isSaving) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            item {
                val admins = state.usuarios.count { it.rol == RolUsuario.Administrador }
                val activos = state.usuarios.count(UsuarioAdministrado::activo)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Usuarios", state.usuarios.size.toString(), Color(0xFF486DEA), Modifier.weight(1f))
                    AdminMetric("Activos", activos.toString(), Color(0xFF10A976), Modifier.weight(1f))
                    AdminMetric("Administradores", admins.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Buscar usuario o nombre") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
            }
            val filtered = state.usuarios.filter { query.isBlank() || it.username.contains(query, true) || it.nombreCompleto.contains(query, true) }
            if (filtered.isEmpty() && !state.isLoading) item {
                Text("No hay usuarios que coincidan con tu búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            items(filtered, key = { it.id }) { usuario ->
                UsuarioCard(
                    usuario = usuario,
                    isCurrent = usuario.username.equals(currentUsername, ignoreCase = true),
                    enabled = !state.isSaving,
                    onToggle = { viewModel.clearMutationError(); toggleTarget = usuario },
                    onRole = { viewModel.clearMutationError(); roleTarget = usuario },
                    onDelete = { viewModel.clearMutationError(); deleteTarget = usuario },
                    onEdit = { viewModel.clearMutationError(); editTarget = usuario },
                )
            }
        }
    }

    if (createDialog) {
        CrearUsuarioDialog(
            saving = state.isSaving,
            error = state.mutationError,
            onDismiss = { if (!state.isSaving) createDialog = false },
            onCreate = viewModel::crear,
        )
    }
    editTarget?.let { usuario ->
        EditarUsuarioDialog(usuario, state.isSaving, state.mutationError,
            onDismiss = { if (!state.isSaving) editTarget = null },
            onSave = { viewModel.editar(usuario, it) })
    }
    toggleTarget?.let { usuario ->
        ConfirmDialog(
            title = if (usuario.activo) "Desactivar usuario" else "Activar usuario",
            text = if (usuario.activo) {
                "${usuario.username} no podrá volver a iniciar sesión. Las sesiones JWT ya emitidas pueden seguir vigentes hasta expirar."
            } else {
                "${usuario.username} recuperará el acceso al sistema."
            },
            confirmLabel = if (usuario.activo) "Desactivar" else "Activar",
            busy = state.isSaving,
            error = state.mutationError,
            onDismiss = { if (!state.isSaving) toggleTarget = null },
            onConfirm = {
                viewModel.alternarEstado(usuario, currentUsername)
            },
        )
    }
    roleTarget?.let { usuario ->
        CambiarRolDialog(
            usuario = usuario,
            busy = state.isSaving,
            error = state.mutationError,
            onDismiss = { if (!state.isSaving) roleTarget = null },
            onConfirm = { role ->
                viewModel.cambiarRol(usuario, role, currentUsername)
            },
        )
    }
    deleteTarget?.let { usuario ->
        ConfirmDialog(
            title = "Eliminar usuario",
            text = "¿Eliminar definitivamente a ${usuario.username}? Si tiene tickets, egresos u otro historial, debes desactivarlo para conservar los movimientos.",
            confirmLabel = "Eliminar",
            busy = state.isSaving,
            error = state.mutationError,
            onDismiss = { if (!state.isSaving) deleteTarget = null },
            onConfirm = {
                viewModel.eliminar(usuario, currentUsername)
            },
        )
    }
}

@Composable
private fun UsuarioCard(
    usuario: UsuarioAdministrado,
    isCurrent: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onRole: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = usuario.nombreCompleto.ifBlank { usuario.username },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = buildString {
                            append(usuario.username)
                            append(" · ")
                            append(usuario.rol.apiValue)
                            if (isCurrent) append(" · tu cuenta")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(usuario.estado, usuario.activo)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar nombre", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onToggle, enabled = enabled && !isCurrent) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = if (usuario.activo) "Desactivar" else "Activar",
                        tint = if (usuario.activo) MaterialTheme.colorScheme.error else Color(0xFF10A976),
                    )
                }
                IconButton(onClick = onRole, enabled = enabled && !isCurrent) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Cambiar rol")
                }
                IconButton(onClick = onDelete, enabled = enabled && !isCurrent) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CrearUsuarioDialog(
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, RolUsuario) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(RolUsuario.Finanzas) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo usuario") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") }, singleLine = true)
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text("Contraseña") },
                    supportingText = { Text("Mínimo 6 caracteres") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Text("Rol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RolUsuario.entries.forEach { item ->
                        FilterChip(
                            selected = role == item,
                            onClick = { role = item },
                            label = { Text(item.apiValue) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(username, name, password, role) },
                enabled = username.isNotBlank() && name.isNotBlank() && password.length >= 6 && !saving,
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } },
    )
}

@Composable
private fun CambiarRolDialog(
    usuario: UsuarioAdministrado,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (RolUsuario) -> Unit,
) {
    var selected by remember(usuario.id) { mutableStateOf(usuario.rol) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
        title = { Text("Cambiar rol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("${usuario.username} · El cambio se aplica cuando vuelva a iniciar sesión.")
                RolUsuario.entries.forEach { role ->
                    FilterChip(
                        selected = selected == role,
                        onClick = { selected = role },
                        label = { Text(role.apiValue) },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }, enabled = !busy && selected != usuario.rol) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { Button(onClick = onConfirm, enabled = !busy) { Text(if (busy) "Procesando…" else confirmLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun EditarUsuarioDialog(usuario: UsuarioAdministrado, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(usuario.id) { mutableStateOf(usuario.nombreCompleto) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar usuario") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cuenta: ${usuario.username}", style = MaterialTheme.typography.bodySmall)
            Text("El acceso, la contraseña y el historial no cambian.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it.take(150) }, Modifier.fillMaxWidth(), enabled = !busy,
                label = { Text("Nombre completo") }, supportingText = { Text("${name.length}/150") }, singleLine = true)
        } },
        confirmButton = { Button(onClick = { onSave(name) }, enabled = !busy && name.isNotBlank()) { Text("Guardar cambios") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}
