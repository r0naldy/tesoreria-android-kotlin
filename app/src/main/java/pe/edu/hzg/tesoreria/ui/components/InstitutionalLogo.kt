package pe.edu.hzg.tesoreria.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Renders the public institutional logo without adding an image-loading/network dependency.
 * The source is already returned by /api/configuracion as a data URL. Bounds and sampling keep a
 * highly compressed, oversized image from allocating an unbounded bitmap on the login screen.
 */
@Composable
fun InstitutionalLogo(
    dataUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    val bitmap = remember(dataUrl) { decodeInstitutionalLogo(dataUrl) }
    if (bitmap == null) {
        fallback()
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

private fun decodeInstitutionalLogo(dataUrl: String?): android.graphics.Bitmap? {
    if (dataUrl.isNullOrBlank() || dataUrl.length > MAX_DATA_URL_CHARS) return null
    val separator = dataUrl.indexOf("base64,")
    if (!dataUrl.startsWith("data:image/", ignoreCase = true) || separator < 0) return null

    return runCatching {
        val bytes = Base64.decode(dataUrl.substring(separator + "base64,".length), Base64.DEFAULT)
        if (bytes.isEmpty() || bytes.size > MAX_ENCODED_BYTES) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (largest / sample > TARGET_BITMAP_SIDE) sample *= 2

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }.getOrNull()
}

private const val MAX_ENCODED_BYTES = 800 * 1024
private const val MAX_DATA_URL_CHARS = 1_200_000
private const val TARGET_BITMAP_SIDE = 512
