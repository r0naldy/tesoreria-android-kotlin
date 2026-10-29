package pe.edu.hzg.tesoreria.data.local

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class ReceiptImage(
    val bytes: ByteArray,
    val fileName: String,
    val mediaType: String,
)

class ReceiptImageReader(private val contentResolver: ContentResolver) {
    fun read(uriText: String): ReceiptImage {
        val uri = Uri.parse(uriText)
        val bytes = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> File(requireNotNull(uri.path)).readBytes()
            else -> contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("No se pudo abrir la imagen del comprobante.")
        }
        require(bytes.isNotEmpty()) { "La imagen está vacía." }
        require(bytes.size <= MAX_IMAGE_BYTES) { "La imagen supera el límite de 20 MB." }

        return ReceiptImage(
            bytes = bytes,
            fileName = displayName(uri) ?: "comprobante.jpg",
            mediaType = contentResolver.getType(uri) ?: "image/jpeg",
        )
    }

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.lastPathSegment
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    /**
     * Removes the app-owned temporary image after a successful backend response.
     * Cleanup is deliberately best-effort: a filesystem cleanup failure must not
     * cause the already accepted receipt to be uploaded a second time.
     */
    fun deleteAfterConfirmedUpload(uriText: String): Boolean = runCatching {
        val uri = Uri.parse(uriText)
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val file = File(requireNotNull(uri.path))
                !file.exists() || file.delete()
            }
            ContentResolver.SCHEME_CONTENT -> contentResolver.delete(uri, null, null) > 0
            else -> false
        }
    }.getOrDefault(false)

    private companion object {
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
