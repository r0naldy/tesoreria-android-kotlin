package pe.edu.hzg.tesoreria.domain.model

import kotlinx.datetime.Clock
data class AuthSession(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val expiresAtEpochMillis: Long,
) {
    fun isExpired(nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds()): Boolean =
        nowEpochMillis >= expiresAtEpochMillis
}
