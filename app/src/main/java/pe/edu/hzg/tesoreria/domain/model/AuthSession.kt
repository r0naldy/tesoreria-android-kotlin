package pe.edu.hzg.tesoreria.domain.model

data class AuthSession(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val expiresAtEpochMillis: Long,
) {
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        nowEpochMillis >= expiresAtEpochMillis
}
