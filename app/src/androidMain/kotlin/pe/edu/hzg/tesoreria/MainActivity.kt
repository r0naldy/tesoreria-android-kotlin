package pe.edu.hzg.tesoreria

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pe.edu.hzg.tesoreria.security.SsoDeepLinkParser
import pe.edu.hzg.tesoreria.security.SsoDeepLinkResult
import pe.edu.hzg.tesoreria.ui.TesoreriaApp
import pe.edu.hzg.tesoreria.ui.theme.TesoreriaTheme

class MainActivity : ComponentActivity() {
    private var pendingSsoLink by mutableStateOf<SsoDeepLinkResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // DNI, financial data and receipt photos must not be exposed through screenshots,
        // screen recording or the Recents preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        captureSsoLink(intent)
        setContent {
            TesoreriaTheme {
                TesoreriaApp(
                    pendingSsoLink = pendingSsoLink,
                    onSsoLinkConsumed = { pendingSsoLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureSsoLink(intent)
    }

    private fun captureSsoLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val rawLink = intent.dataString
        // Match the web flow: remove the one-use token from the Activity intent immediately so it
        // cannot survive in saved instance state, Recents metadata or a later intent inspection.
        intent.data = null
        setIntent(intent)
        when (val parsed = SsoDeepLinkParser.parse(rawLink, BuildConfig.API_BASE_URL)) {
            SsoDeepLinkResult.NotSsoLink -> Unit
            else -> pendingSsoLink = parsed
        }
    }
}
