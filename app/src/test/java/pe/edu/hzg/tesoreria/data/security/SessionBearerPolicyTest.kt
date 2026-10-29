package pe.edu.hzg.tesoreria.data.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import pe.edu.hzg.tesoreria.domain.model.AuthSession

class SessionBearerPolicyTest {
    private val base = "https://api.example.test"
    private val endpoint = "$base/api/programas"

    @Test fun `each request uses latest session after logout and login`() = runBlocking {
        val tokens = TestTokens(session("old-user", "old-token"))
        val policy = SessionBearerPolicy(base, tokens)
        assertEquals("Bearer old-token", policy.authorizationFor(endpoint, null))
        tokens.clear()
        assertNull(policy.authorizationFor(endpoint, null))
        tokens.save(session("new-user", "new-token"))
        assertEquals("Bearer new-token", policy.authorizationFor(endpoint, null))
    }

    @Test fun `explicit owner token is not overwritten by different current session`() = runBlocking {
        val policy = SessionBearerPolicy(base, TestTokens(session("new-user", "new-token")))
        assertEquals("Bearer pinned-owner-token", policy.authorizationFor(endpoint, "Bearer pinned-owner-token"))
    }

    @Test fun `late 401 from previous session never clears current session`() = runBlocking {
        val tokens = TestTokens(session("new-user", "new-token"))
        val policy = SessionBearerPolicy(base, tokens)
        assertFalse(policy.onUnauthorized(endpoint, "Bearer old-token"))
        assertEquals("new-token", tokens.read()?.token)
    }

    @Test fun `late 401 from same username with renewed token never clears renewed session`() = runBlocking {
        val tokens = TestTokens(session("admin", "renewed-token"))
        val policy = SessionBearerPolicy(base, tokens)
        assertFalse(policy.onUnauthorized(endpoint, "Bearer old-token"))
        assertEquals("renewed-token", tokens.read()?.token)
    }

    @Test fun `401 clears exactly the rejected token using atomic compare and clear`() = runBlocking {
        val tokens = TestTokens(session("admin", "current-token"))
        val policy = SessionBearerPolicy(base, tokens)
        assertTrue(policy.onUnauthorized(endpoint, "Bearer current-token"))
        assertNull(tokens.read())
    }

    @Test fun `login and SSO do not send existing bearer and rejected login cannot logout`() = runBlocking {
        val tokens = TestTokens(session("admin", "current-token"))
        val policy = SessionBearerPolicy(base, tokens)
        listOf("$base/api/auth/login", "$base/api/auth/sso").forEach { url ->
            assertNull(policy.authorizationFor(url, null))
            assertNull(policy.authorizationFor(url, "Bearer current-token"))
            assertFalse(policy.onUnauthorized(url, "Bearer current-token"))
        }
        assertNotNull(tokens.read())
    }

    @Test fun `expired tokens are not attached`() = runBlocking {
        val policy = SessionBearerPolicy(base, TestTokens(session("admin", "expired").copy(expiresAtEpochMillis = 1)))
        assertNull(policy.authorizationFor(endpoint, null))
    }

    @Test fun `HTTPS scheme exact host and effective port are required`() = runBlocking {
        val tokens = TestTokens(session("admin", "secret"))
        val policy = SessionBearerPolicy(base, tokens)
        listOf("http://api.example.test/api/programas", "https://api.example.test:444/api/programas",
            "https://api.example.test.attacker.test/api/programas", "https://attacker.test/api/programas",
            "https://user:pass@api.example.test/api/programas").forEach { url ->
            assertFalse(url, policy.isTrusted(url))
            assertNull(policy.authorizationFor(url, "Bearer explicit"))
            assertFalse(policy.onUnauthorized(url, "Bearer secret"))
        }
        assertEquals("secret", tokens.read()?.token)
        assertTrue(policy.isTrusted("https://API.EXAMPLE.TEST:443/api/programas"))
    }

    @Test fun `configured nondefault TLS port is exact`() {
        val policy = SessionBearerPolicy("https://api.example.test:8443", TestTokens(null))
        assertTrue(policy.isTrusted("https://api.example.test:8443/api/programas"))
        assertFalse(policy.isTrusted(endpoint))
    }

    @Test fun `unauthenticated 401 cannot clear active session`() = runBlocking {
        val tokens = TestTokens(session("admin", "secret"))
        val policy = SessionBearerPolicy(base, tokens)
        assertFalse(policy.onUnauthorized(endpoint, null))
        assertFalse(policy.onUnauthorized(endpoint, "Basic abc"))
        assertFalse(policy.onUnauthorized(endpoint, "Bearer "))
        assertNotNull(tokens.read())
    }

    @Test fun `invalid API origins fail before network`() {
        listOf("http://api.example.test", "https://user:pass@api.example.test",
            "https://api.example.test?token=x", "https://api.example.test#x").forEach {
            assertTrue(runCatching { SessionBearerPolicy(it, TestTokens(null)) }.isFailure)
        }
    }

    @Test fun `mounted API base keeps public authentication paths token free`() = runBlocking {
        val policy = SessionBearerPolicy("$base/tesoreria", TestTokens(session("admin", "secret")))
        assertNull(policy.authorizationFor("$base/tesoreria/api/auth/login", null))
        assertEquals("Bearer secret", policy.authorizationFor("$base/tesoreria/api/programas", null))
    }

    private fun session(user: String, token: String) = AuthSession(token, user, user, "Administrador", Long.MAX_VALUE)

    private class TestTokens(initial: AuthSession?) : TokenStore {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override fun observe(): Flow<AuthSession?> = state
        override suspend fun read() = state.value
        override suspend fun save(session: AuthSession) = mutex.withLock { state.value = session }
        override suspend fun clear() = mutex.withLock { state.value = null }
        override suspend fun clearIfTokenMatches(token: String): Boolean = mutex.withLock {
            if (state.value?.token != token) return@withLock false
            state.value = null
            true
        }
    }
}
