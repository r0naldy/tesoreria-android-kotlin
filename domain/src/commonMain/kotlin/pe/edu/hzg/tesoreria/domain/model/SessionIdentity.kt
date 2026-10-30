package pe.edu.hzg.tesoreria.domain.model

import pe.edu.hzg.tesoreria.domain.model.AuthSession

/**
 * Stable local ownership key for data queued while a user is authenticated.
 *
 * It deliberately excludes the JWT and the role: both can change while the
 * account remains the same. The API uses usernames as the account identity.
 */
object SessionIdentity {
    fun fromUsername(username: String): String =
        username.trim().lowercase().also {
            require(it.isNotEmpty()) { "La sesión no contiene un usuario válido." }
        }

    fun fromSession(session: AuthSession): String = fromUsername(session.username)
}
