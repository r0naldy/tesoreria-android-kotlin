package pe.edu.hzg.tesoreria.admin.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import pe.edu.hzg.tesoreria.admin.model.CampoConfiguracion
import pe.edu.hzg.tesoreria.admin.presentation.ConfiguracionViewModel

@Composable
fun ConfiguracionScreen(
    canEdit: Boolean,
    viewModel: ConfiguracionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                readLogoDataUrl(context, uri).fold(
                    onSuccess = viewModel::updateLogo,
                    onFailure = { viewModel.reportLogoError(it.message ?: "No se pudo leer el logo.") },
                )
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it) }
        viewModel.clearMessage()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AdminHeader(
                    title = "Configuración",
                    subtitle = "Identidad institucional y datos de los documentos",
                    icon = Icons.Rounded.Business,
                    onRefresh = viewModel::refresh,
                )
            }
            if (state.isLoading || state.isSaving) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!canEdit) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null)
                            Text(
                                "Vista de solo lectura. Únicamente un Administrador puede guardar cambios.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            item {
                LogoCard(
                    dataUrl = state.form.logoBase64,
                    canEdit = canEdit && !state.isSaving,
                    onPick = { logoPicker.launch("image/*") },
                )
            }
            item { SectionTitle("Institución") }
            item {
                ConfigField(
                    label = "Nombre de la institución",
                    value = state.form.nombreInstitucion,
                    canEdit = canEdit,
                    onValueChange = { viewModel.update(CampoConfiguracion.NombreInstitucion, it) },
                )
            }
            item {
                ConfigField("Ciudad", state.form.ciudad, canEdit) {
                    viewModel.update(CampoConfiguracion.Ciudad, it)
                }
            }
            item {
                ConfigField("Código modular", state.form.codigoModular, canEdit) {
                    viewModel.update(CampoConfiguracion.CodigoModular, it)
                }
            }
            item {
                ConfigField("Dirección", state.form.direccion, canEdit) {
                    viewModel.update(CampoConfiguracion.Direccion, it)
                }
            }
            item {
                ConfigField("Base legal", state.form.baseLegal, canEdit) {
                    viewModel.update(CampoConfiguracion.BaseLegal, it)
                }
            }
            item { SectionTitle("Comprobante") }
            item {
                ConfigField("Título del comprobante", state.form.tituloComprobante, canEdit) {
                    viewModel.update(CampoConfiguracion.TituloComprobante, it)
                }
            }
            item {
                ConfigField("Tipo de comprobante", state.form.tipoComprobante, canEdit) {
                    viewModel.update(CampoConfiguracion.TipoComprobante, it)
                }
            }
            item { SectionTitle("Registro oficial de matrícula") }
            item {
                ConfigField("DRE / GRE", state.form.dreGre, canEdit) {
                    viewModel.update(CampoConfiguracion.DreGre, it)
                }
            }
            item {
                ConfigField("UGEL", state.form.ugel, canEdit) {
                    viewModel.update(CampoConfiguracion.Ugel, it)
                }
            }
            item {
                ConfigField("Resolución de creación", state.form.resolucionCreacion, canEdit) {
                    viewModel.update(CampoConfiguracion.ResolucionCreacion, it)
                }
            }
            item {
                ConfigField("Resolución de autorización", state.form.resolucionAutorizacion, canEdit) {
                    viewModel.update(CampoConfiguracion.ResolucionAutorizacion, it)
                }
            }
            item {
                ConfigField("Periodo lectivo", state.form.periodoLectivo, canEdit) {
                    viewModel.update(CampoConfiguracion.PeriodoLectivo, it)
                }
            }
            item {
                ConfigField("Modalidad del servicio", state.form.modalidadServicio, canEdit) {
                    viewModel.update(CampoConfiguracion.ModalidadServicio, it)
                }
            }
            item {
                ConfigField("Nivel formativo", state.form.nivelFormativo, canEdit) {
                    viewModel.update(CampoConfiguracion.NivelFormativo, it)
                }
            }
            item {
                ConfigField("Tipo de plan", state.form.tipoPlan, canEdit) {
                    viewModel.update(CampoConfiguracion.TipoPlan, it)
                }
            }
            if (canEdit) {
                item {
                    Button(
                        onClick = { viewModel.save(canEdit = true) },
                        enabled = state.loaded && !state.isSaving && state.form.nombreInstitucion.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Text("  Guardar cambios")
                    }
                }
            }
            item { Box(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LogoCard(
    dataUrl: String?,
    canEdit: Boolean,
    onPick: () -> Unit,
) {
    val bitmap = remember(dataUrl) {
        if (dataUrl.isNullOrBlank() || dataUrl.length > MAX_LOGO_DATA_URL_CHARS) {
            null
        } else {
            runCatching {
                val payload = dataUrl.substringAfter("base64,", dataUrl)
                val bytes = Base64.decode(payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Logo institucional",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canEdit) {
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text("  Seleccionar logo")
                }
            }
            Text(
                "PNG, JPG, WEBP o SVG · máximo 800 KB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    canEdit: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = !canEdit,
        singleLine = label !in setOf("Nombre de la institución", "Resolución de creación", "Resolución de autorización"),
        minLines = 1,
        maxLines = 3,
        shape = RoundedCornerShape(16.dp),
    )
}

private suspend fun readLogoDataUrl(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
            ?: throw IllegalArgumentException("Selecciona un archivo de imagen válido.")
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_LOGO_BYTES) {
                    throw IllegalArgumentException("El logo no debe superar 800 KB.")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("No se pudo abrir la imagen seleccionada.")
        if (bytes.isEmpty()) throw IllegalArgumentException("La imagen seleccionada está vacía.")
        "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }
}

private const val MAX_LOGO_BYTES = 800 * 1024
private const val MAX_LOGO_DATA_URL_CHARS = 1_200_000
