package pe.edu.hzg.tesoreria.features.academic.ui

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import pe.edu.hzg.tesoreria.features.academic.AcademicShareFile

@Composable
actual fun CollectAcademicShares(files: SharedFlow<AcademicShareFile>) {
    val context = LocalContext.current
    LaunchedEffect(files) {
        files.collect { file ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = file.mimeType
                putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(file.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, file.title))
        }
    }
}
