package pe.edu.hzg.tesoreria.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.AuthSession

class PendingOperationOwnershipTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var tokens: FakeTokenStore
    private lateinit var store: PendingOperationStore

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        tokens = FakeTokenStore()
        store = PendingOperationStore(database, tokens, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = driver.close()

    @Test
    fun `logout and login as another user never processes previous owner queue`() = runBlocking {
        tokens.save(session("  ADMIN  "))
        store.enqueue(PendingEntityType.COMPROBANTE, "receipt-a", PendingOperationType.UPLOAD, "{}")

        tokens.clear()
        assertTrue(store.ready().isEmpty())

        tokens.save(session("finanzas"))
        store.enqueue(PendingEntityType.COMPROBANTE, "receipt-b", PendingOperationType.UPLOAD, "{}")
        assertEquals(listOf("receipt-b"), store.ready().map { it.entityLocalId })

        tokens.save(session("admin"))
        assertEquals(listOf("receipt-a"), store.ready().map { it.entityLocalId })
        assertEquals("admin", store.ready().single().ownerIdentity)
    }

    @Test
    fun `legacy operations remain quarantined instead of being claimed by next login`() = runBlocking {
        database.appDatabaseQueries.insertPendingOperation(
            id = "legacy-operation",
            owner_identity = "",
            entity_type = PendingEntityType.COMPROBANTE,
            entity_local_id = "legacy-receipt",
            operation = PendingOperationType.UPLOAD,
            payload_json = "{}",
            created_at = 1L,
        )

        tokens.save(session("admin"))
        assertTrue(store.ready().isEmpty())
        assertEquals(
            1L,
            database.appDatabaseQueries
                .countPendingOperations("")
                .executeAsOne(),
        )
    }

    @Test
    fun `upload is parked before post so process restart cannot duplicate it`() = runBlocking {
        tokens.save(session("admin"))
        store.enqueue(PendingEntityType.COMPROBANTE, "receipt-a", PendingOperationType.UPLOAD, "{}")
        val operation = store.ready().single()

        store.markUploadInFlight(operation)

        assertTrue(store.ready().isEmpty())
        val stored = database.appDatabaseQueries.findPendingOperation(
            owner_identity = "admin",
            entity_type = PendingEntityType.COMPROBANTE,
            entity_local_id = "receipt-a",
            operation = PendingOperationType.UPLOAD,
        ).executeAsOne()
        assertEquals(Long.MAX_VALUE, stored.next_attempt_at)
        assertEquals(PendingOperationStore.UPLOAD_IN_FLIGHT, stored.last_error)
    }

    private fun session(username: String) = AuthSession(
        token = "token-$username",
        username = username,
        nombreCompleto = username,
        rol = "Administrador",
        expiresAtEpochMillis = Long.MAX_VALUE,
    )

    private class FakeTokenStore : TokenStore {
        private val state = MutableStateFlow<AuthSession?>(null)

        override fun observe(): Flow<AuthSession?> = state
        override suspend fun read(): AuthSession? = state.value
        override suspend fun save(session: AuthSession) {
            state.value = session
        }
        override suspend fun clear() {
            state.value = null
        }
        override suspend fun clearIfTokenMatches(token: String): Boolean {
            if (state.value?.token != token) return false
            state.value = null
            return true
        }
    }
}
