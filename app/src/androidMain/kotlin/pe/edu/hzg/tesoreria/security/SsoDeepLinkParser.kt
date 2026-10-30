package pe.edu.hzg.tesoreria.security

import java.net.URI
import java.net.URLDecoder



/**
 * Accepts only the verified HTTPS app-link shape already used by the web client:
 * `https://<api-host>/sso#token=<short-lived-signed-jwt>`.
 *
 * The token is deliberately rejected in the query string because queries reach web servers,
 * proxies and analytics. A fragment is kept client-side and the API independently validates its
 * issuer, audience, signature, expiry and one-time `jti` before creating a Tesorería session.
 */
object SsoDeepLinkParser {
    fun parse(rawUri: String?, expectedBaseUrl: String): SsoDeepLinkResult {
        if (rawUri.isNullOrBlank()) return SsoDeepLinkResult.NotSsoLink

        val uri = runCatching { URI(rawUri) }.getOrNull()
            ?: return invalidLink()
        val base = runCatching { URI(expectedBaseUrl) }.getOrNull()
            ?: return SsoDeepLinkResult.NotSsoLink

        val isExpectedOrigin = uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(base.host, ignoreCase = true) &&
            uri.rawUserInfo == null &&
            effectivePort(uri) == effectivePort(base)
        val isSsoPath = uri.path == "/sso" || uri.path == "/sso/"
        if (!isExpectedOrigin || !isSsoPath) return SsoDeepLinkResult.NotSsoLink

        if (!uri.rawQuery.isNullOrBlank()) {
            return SsoDeepLinkResult.Invalid(
                "El acceso de ASISTO no es seguro: el token debe viajar en el fragmento del enlace.",
            )
        }

        val tokenValues = uri.rawFragment
            ?.split('&')
            .orEmpty()
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = decode(part.substring(0, separator)) ?: return@mapNotNull null
                if (key != "token") return@mapNotNull null
                decode(part.substring(separator + 1))
            }

        val token = tokenValues.singleOrNull()
            ?: return SsoDeepLinkResult.Invalid(
                "Falta el acceso de ASISTO. Vuelve a ASISTO y abre Tesorería nuevamente.",
            )

        val validJwtShape = token.length in 32..MAX_TOKEN_LENGTH &&
            token.count { it == '.' } == 2 &&
            token.none(Char::isWhitespace) &&
            token.none(Char::isISOControl)
        if (!validJwtShape) return invalidLink()

        return SsoDeepLinkResult.Valid(token)
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        uri.scheme.equals("http", ignoreCase = true) -> 80
        else -> -1
    }

    private fun decode(value: String): String? = runCatching {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrNull()

    private fun invalidLink() = SsoDeepLinkResult.Invalid(
        "El acceso de ASISTO es inválido o está dañado. Solicita uno nuevo desde ASISTO.",
    )

    private const val MAX_TOKEN_LENGTH = 8_192
}
