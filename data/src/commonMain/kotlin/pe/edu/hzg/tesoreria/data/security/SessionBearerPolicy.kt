package pe.edu.hzg.tesoreria.data.security

internal expect class SessionBearerPolicy(baseUrl: String, tokens: TokenStore) {
    fun isTrusted(url: String): Boolean
    suspend fun authorizationFor(url: String, explicitAuthorization: String?): String?
    suspend fun onUnauthorized(url: String, sentAuthorization: String?): Boolean
}
