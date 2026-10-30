package pe.edu.hzg.tesoreria.ui.screens

import android.graphics.*
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import java.io.*
import java.net.*
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Expense
import pe.edu.hzg.tesoreria.security.RemoteReceiptImagePolicy

private const val MAX_RECEIPT_IMAGE_BYTES = 10 * 1024 * 1024
private const val MAX_RECEIPT_BASE64_CHARS = 14 * 1024 * 1024
private const val MAX_RECEIPT_REDIRECTS = 3
private const val MAX_RECEIPT_SOURCE_PIXELS = 100_000_000L

internal actual fun loadReceiptBitmap(item: Expense, apiBaseUrl: String): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    item.imagenBase64?.let { encoded ->
        val comma = encoded.indexOf(',')
        if (comma >= 0 && !encoded.substring(0, comma).startsWith("data:image/", ignoreCase = true)) {
            return@runCatching null
        }
        val payload = if (comma >= 0) encoded.substring(comma + 1) else encoded
        if (payload.length > MAX_RECEIPT_BASE64_CHARS) return@runCatching null
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        if (bytes.size > MAX_RECEIPT_IMAGE_BYTES) return@runCatching null
        return@runCatching decodeSampled(bytes)
    }
    val raw = item.imagenUrl ?: item.imagenRuta?.let { "/uploads/${it.trimStart('/')}" } ?: return@runCatching null
    val uri = RemoteReceiptImagePolicy.resolveInitial(raw, apiBaseUrl)
        ?: return@runCatching null
    downloadReceiptBytes(uri, apiBaseUrl)?.let(::decodeSampled)
}.getOrNull()?.asImageBitmap()

private fun downloadReceiptBytes(initialUri: URI, apiBaseUrl: String): ByteArray? {
    var current = initialUri
    for (redirectCount in 0..MAX_RECEIPT_REDIRECTS) {
        val connection = current.toURL().openConnection() as? HttpURLConnection ?: return null
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "image/*")
            connection.setRequestProperty("Accept-Encoding", "identity")

            when (connection.responseCode) {
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER,
                307,
                308,
                -> {
                    if (redirectCount == MAX_RECEIPT_REDIRECTS) return null
                    val location = connection.getHeaderField("Location") ?: return null
                    current = RemoteReceiptImagePolicy.resolveRedirect(
                        location = location,
                        current = current,
                        apiBaseUrl = apiBaseUrl,
                    ) ?: return null
                    continue
                }
                HttpURLConnection.HTTP_OK -> Unit
                else -> return null
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_RECEIPT_IMAGE_BYTES) return null
            val contentType = connection.contentType?.substringBefore(';')?.trim()
            if (contentType != null && !contentType.startsWith("image/", ignoreCase = true)) return null
            return connection.inputStream.use { it.readWithLimit(MAX_RECEIPT_IMAGE_BYTES) }
        } finally {
            connection.disconnect()
        }
    }
    return null
}

private fun InputStream.readWithLimit(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun decodeSampled(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    if (bounds.outWidth.toLong() * bounds.outHeight.toLong() > MAX_RECEIPT_SOURCE_PIXELS) return null
    var sample = 1
    while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}
