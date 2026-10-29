package pe.edu.hzg.tesoreria.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import pe.edu.hzg.tesoreria.data.coroutines.runSuspendCatching
import pe.edu.hzg.tesoreria.data.remote.TesoreriaApi
import pe.edu.hzg.tesoreria.data.remote.dto.AuthResponseDto
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: TesoreriaApi,
    private val tokenStore: TokenStore,
) : AuthRepository {
    override fun observeSession(): Flow<AuthSession?> = tokenStore.observe()

    override suspend fun login(username: String, password: String): Result<AuthSession> = runSuspendCatching {
        persist(api.login(username, password))
    }

    override suspend fun loginFromAsisto(token: String): Result<AuthSession> = runSuspendCatching {
        persist(api.loginFromAsisto(token))
    }

    private suspend fun persist(response: AuthResponseDto): AuthSession {
        currentCoroutineContext().ensureActive()
        return AuthSession(
            token = response.token,
            username = response.username,
            nombreCompleto = response.nombreCompleto,
            rol = response.rol,
            expiresAtEpochMillis = System.currentTimeMillis() + response.expiraEnMinutos * 60_000L,
        ).also { tokenStore.save(it) }
    }

    override suspend fun logout() = tokenStore.clear()
}
