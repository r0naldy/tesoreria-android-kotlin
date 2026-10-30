package pe.edu.hzg.tesoreria.platform

import androidx.compose.runtime.Composable

expect abstract class PlatformContext
@Composable expect fun platformContext(): PlatformContext
