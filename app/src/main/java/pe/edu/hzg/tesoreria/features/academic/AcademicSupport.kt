package pe.edu.hzg.tesoreria.features.academic

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AcademicShareFile(
    val uri: Uri,
    val mimeType: String,
    val title: String,
)

class AcademicExportManager(private val context: Context) {
    private val directory: File
        get() = File(context.filesDir, "pending-receipts/academic-exports").apply { mkdirs() }

    @Synchronized
    fun writeBytes(fileName: String, bytes: ByteArray, mimeType: String, title: String): AcademicShareFile {
        AcademicExportRetention.cleanup(directory)
        val file = nextAvailableFile(safeName(fileName))
        file.writeBytes(bytes)
        return AcademicShareFile(uriFor(file), mimeType, title)
    }

    fun writeText(fileName: String, text: String, mimeType: String, title: String): AcademicShareFile =
        writeBytes(fileName, text.toByteArray(Charsets.UTF_8), mimeType, title)

    private fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun safeName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(120)
        .ifBlank { "archivo" }

    private fun nextAvailableFile(name: String): File {
        val first = File(directory, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 2
        while (index < 10_000) {
            val candidate = File(directory, "${stem}_$index$extension")
            if (!candidate.exists()) return candidate
            index++
        }
        return File(directory, "${stem}_${System.currentTimeMillis()}$extension")
    }
}

/**
 * Shared files cannot be deleted immediately because the receiving app reads the FileProvider URI
 * asynchronously. A 24-hour window keeps sharing reliable while preventing academic exports from
 * accumulating indefinitely in private storage.
 */
internal object AcademicExportRetention {
    private const val MAX_AGE_MILLIS = 24L * 60L * 60L * 1_000L
    private const val ACTIVE_SHARE_GRACE_MILLIS = 60L * 60L * 1_000L
    private const val MAX_RETAINED_FILES = 12

    fun cleanup(directory: File, nowMillis: Long = System.currentTimeMillis()) {
        val files = directory.listFiles()?.filter(File::isFile).orEmpty()
        files.filter { file ->
            file.name.endsWith(".part") ||
                file.lastModified() <= 0L ||
                nowMillis - file.lastModified() > MAX_AGE_MILLIS
        }.forEach { it.delete() }

        directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_RETAINED_FILES - 1) // Leave room for the export about to be written.
            // Do not invalidate a URI that may still be open in the Android share sheet.
            ?.filter { nowMillis - it.lastModified() > ACTIVE_SHARE_GRACE_MILLIS }
            ?.forEach { it.delete() }
    }
}

suspend fun Throwable.academicMessage(): String {
    if (this is ClientRequestException && response.status.value == 403) {
        return "Tu usuario no tiene permiso para realizar esta acción."
    }
    if (this is ResponseException) {
        val body = runCatching { response.bodyAsText() }.getOrNull()
        val serverMessage = body?.let { raw ->
            runCatching {
                Json.parseToJsonElement(raw).jsonObject["mensaje"]?.jsonPrimitive?.content
            }.getOrNull()
        }
        if (!serverMessage.isNullOrBlank()) return serverMessage
        return when (response.status.value) {
            401 -> "La sesión venció. Vuelve a iniciar sesión."
            404 -> "No se encontró la información solicitada."
            in 500..599 -> "El servidor no pudo completar la operación."
            else -> "No se pudo completar la operación (${response.status.value})."
        }
    }
    return message?.takeIf { it.isNotBlank() }
        ?: "No se pudo conectar con el sistema empresarial."
}
