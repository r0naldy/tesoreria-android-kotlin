package pe.edu.hzg.tesoreria.data.security

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.AuthSession

interface TokenStore {
    fun observe(): Flow<AuthSession?>
    suspend fun read(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
    /** Atomically clears only the session whose token was actually rejected. */
    suspend fun clearIfTokenMatches(token: String): Boolean
}
