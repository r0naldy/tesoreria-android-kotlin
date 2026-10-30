package pe.edu.hzg.tesoreria.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import pe.edu.hzg.tesoreria.camera.CameraFileFactory

@Composable
internal actual fun rememberReceiptCapture(onCaptured: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val currentCallback by rememberUpdatedState(onCaptured)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) captureUri?.let { currentCallback(it.toString()) }
    }
    return { CameraFileFactory.createImageUri(context).also { captureUri = it; launcher.launch(it) } }
}
