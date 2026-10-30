package pe.edu.hzg.tesoreria.security

sealed interface SsoDeepLinkResult {
    data object NotSsoLink : SsoDeepLinkResult
    data class Valid(val token: String) : SsoDeepLinkResult
    data class Invalid(val userMessage: String) : SsoDeepLinkResult
}
