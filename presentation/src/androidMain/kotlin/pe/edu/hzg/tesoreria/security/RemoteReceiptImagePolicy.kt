package pe.edu.hzg.tesoreria.security

import java.net.URI

/** Same-origin policy for receipt images returned by the business API. */
internal object RemoteReceiptImagePolicy {
    fun resolveInitial(raw: String, apiBaseUrl: String): URI? {
        val apiOrigin = parseApiOrigin(apiBaseUrl) ?: return null
        val candidate = runCatching { apiOrigin.resolve(URI(raw.trim())) }.getOrNull() ?: return null
        return candidate.normalizedForRequest().takeIf { isSameOrigin(it, apiOrigin) }
    }

    fun resolveRedirect(location: String, current: URI, apiBaseUrl: String): URI? {
        val apiOrigin = parseApiOrigin(apiBaseUrl) ?: return null
        val candidate = runCatching { current.resolve(URI(location.trim())) }.getOrNull() ?: return null
        return candidate.normalizedForRequest().takeIf { isSameOrigin(it, apiOrigin) }
    }

    private fun parseApiOrigin(apiBaseUrl: String): URI? = runCatching {
        URI(apiBaseUrl.trim()).normalizedForRequest()
    }.getOrNull()?.takeIf { uri ->
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }

    private fun isSameOrigin(candidate: URI, apiOrigin: URI): Boolean =
        candidate.scheme.equals("https", ignoreCase = true) &&
            candidate.host?.equals(apiOrigin.host, ignoreCase = true) == true &&
            effectivePort(candidate) == effectivePort(apiOrigin) &&
            candidate.userInfo == null

    private fun effectivePort(uri: URI): Int = if (uri.port == -1) 443 else uri.port

    // URI.normalize keeps percent-encoded path/query bytes intact, which is important if the API
    // uses a signed image URL. The fragment is harmless because HTTP clients never transmit it.
    private fun URI.normalizedForRequest(): URI = normalize()
}
