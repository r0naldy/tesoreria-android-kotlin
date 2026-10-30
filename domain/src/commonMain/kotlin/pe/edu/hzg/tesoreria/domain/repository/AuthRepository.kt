package pe.edu.hzg.tesoreria.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.AuthSession

interface AuthRepository {
    fun observeSession(): Flow<AuthSession?>
    suspend fun login(username: String, password: String): Result<AuthSession>
    suspend fun loginFromAsisto(token: String): Result<AuthSession>
    suspend fun logout()
}
