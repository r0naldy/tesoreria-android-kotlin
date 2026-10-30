package pe.edu.hzg.tesoreria.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = android.content.Context
@Composable actual fun platformContext(): PlatformContext = LocalContext.current
