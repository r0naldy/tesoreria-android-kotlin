package pe.edu.hzg.tesoreria.data.security

import java.net.URI

/** No second token cache: every new request sees the current encrypted session. */
internal actual class SessionBearerPolicy actual constructor(baseUrl: String, private val tokens: TokenStore) {
    private val origin = URI(baseUrl)
    private val authPrefix = "${origin.path.orEmpty().trimEnd('/')}/api/auth/"

    init {
        require(origin.scheme.equals("https", ignoreCase = true) && !origin.host.isNullOrBlank() &&
            origin.rawUserInfo == null && origin.rawQuery == null && origin.rawFragment == null) {
            "La API debe usar una URL HTTPS sin credenciales, consulta ni fragmento."
        }
    }

    actual fun isTrusted(url: String): Boolean = runCatching {
        val target = URI(url)
        target.scheme.equals("https", ignoreCase = true) &&
            target.host.equals(origin.host, ignoreCase = true) &&
            effectivePort(target) == effectivePort(origin) && target.rawUserInfo == null
    }.getOrDefault(false)

    suspend actual fun authorizationFor(url: String, explicitAuthorization: String?): String? {
        if (!isTrusted(url) || isLogin(url)) return null
        // Synchronization pins the owner's token before sending. Never replace it
        // with the token of a user who logs in while that request is queued.
        if (!explicitAuthorization.isNullOrBlank()) return explicitAuthorization
        return tokens.read()?.takeUnless { it.isExpired() }?.let { "Bearer ${it.token}" }
    }

    suspend actual fun onUnauthorized(url: String, sentAuthorization: String?): Boolean {
        if (!isTrusted(url) || isLogin(url)) return false
        val prefix = "Bearer "
        if (sentAuthorization == null || !sentAuthorization.startsWith(prefix, ignoreCase = true)) return false
        val sentToken = sentAuthorization.substring(prefix.length).trim().takeIf { it.isNotEmpty() } ?: return false
        // Compare-and-clear is atomic with login/save. A late 401 from a previous
        // request cannot clear the new session, including for the same username.
        return tokens.clearIfTokenMatches(sentToken)
    }

    private fun isLogin(url: String): Boolean = runCatching {
        URI(url).path.let { it == "${authPrefix}login" || it == "${authPrefix}sso" }
    }.getOrDefault(false)

    private fun effectivePort(uri: URI): Int = if (uri.port == -1) 443 else uri.port
}
