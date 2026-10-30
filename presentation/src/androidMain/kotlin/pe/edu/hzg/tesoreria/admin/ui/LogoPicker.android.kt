package pe.edu.hzg.tesoreria.admin.ui

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

@Composable
internal actual fun rememberLogoPicker(onResult: (Result<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentCallback by rememberUpdatedState(onResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { currentCallback(readLogoDataUrl(context, uri)) }
    }
    return { picker.launch("image/*") }
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
