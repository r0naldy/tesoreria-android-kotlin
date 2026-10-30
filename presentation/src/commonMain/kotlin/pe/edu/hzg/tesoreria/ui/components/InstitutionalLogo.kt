package pe.edu.hzg.tesoreria.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

internal expect fun decodeInstitutionalLogo(dataUrl: String?): androidx.compose.ui.graphics.ImageBitmap?
