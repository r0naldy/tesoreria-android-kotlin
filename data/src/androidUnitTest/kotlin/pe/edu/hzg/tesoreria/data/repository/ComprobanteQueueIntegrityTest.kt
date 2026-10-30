package pe.edu.hzg.tesoreria.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteDto
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.data.sync.PendingEntityType
import pe.edu.hzg.tesoreria.data.sync.PendingOperationStore
import pe.edu.hzg.tesoreria.data.sync.PendingOperationType
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft
import pe.edu.hzg.tesoreria.domain.model.ComprobanteProducto

/** Real SQLite rollback/visibility checks; no network, credentials or device files. */
class ComprobanteQueueIntegrityTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var tokens: QueueTokens
    private lateinit var store: PendingOperationStore
    private lateinit var writer: ComprobanteLocalWriter

    @Before fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        tokens = QueueTokens()
        store = PendingOperationStore(database, tokens, Dispatchers.Unconfined)
        writer = ComprobanteLocalWriter(database)
    }

    @After fun tearDown() = driver.close()

    @Test fun `receipt products and owner operation commit together`() = runBlocking {
        enqueue("receipt-a")
        val stored = database.appDatabaseQueries.selectComprobanteByLocalId("receipt-a").executeAsOne()
        assertEquals("admin", stored.usuario)
        assertEquals(1500L, stored.total_centimos)
        assertEquals(2, database.appDatabaseQueries.selectProductosForComprobante("receipt-a").executeAsList().size)
        val operation = store.ready().single()
        assertEquals("admin", operation.ownerIdentity)
        assertEquals("receipt-a", operation.entityLocalId)
    }

    @Test fun `failure inside local write rolls back receipt products and outbox`() = runBlocking {
        val result = runCatching {
            store.enqueue(PendingEntityType.COMPROBANTE, "receipt-a", PendingOperationType.UPLOAD, "{}") { owner ->
                writer.insertDraft(draft(), "receipt-a", owner, 1)
                throw IllegalStateException("simulated write failure")
            }
        }
        assertTrue(result.isFailure)
        assertNothingPersisted("receipt-a")
    }

    @Test fun `outbox insert failure rolls back previously written receipt and products`() = runBlocking {
        driver.execute(null,
            "CREATE TRIGGER reject_outbox BEFORE INSERT ON pending_operations BEGIN SELECT RAISE(ABORT, 'simulated outbox failure'); END", 0)
        assertTrue(runCatching { enqueue("receipt-a") }.isFailure)
        assertNothingPersisted("receipt-a")
    }

    @Test fun `product insert failure rolls back whole receipt`() = runBlocking {
        driver.execute(null,
            "CREATE TRIGGER reject_product BEFORE INSERT ON productos BEGIN SELECT RAISE(ABORT, 'simulated product failure'); END", 0)
        assertTrue(runCatching { enqueue("receipt-a") }.isFailure)
        assertNothingPersisted("receipt-a")
    }

    @Test fun `missing session refuses queue before persisting private data`() = runBlocking {
        tokens.clear()
        assertTrue(runCatching { enqueue("receipt-a") }.isFailure)
        assertNothingPersisted("receipt-a")
    }

    @Test fun `owner is captured before IO dispatch not reassigned to later session`() = runBlocking {
        val dispatcher = PausedDispatcher()
        val delayedStore = PendingOperationStore(database, tokens, dispatcher)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            delayedStore.enqueue(PendingEntityType.COMPROBANTE, "receipt-a", PendingOperationType.UPLOAD, "{}") { owner ->
                writer.insertDraft(draft(), "receipt-a", owner, 1)
            }
        }
        tokens.current = tokens.current?.copy(username = "new-user", token = "new-token")
        dispatcher.runPending()
        pending.await()
        assertEquals("admin", database.appDatabaseQueries.selectComprobanteByLocalId("receipt-a").executeAsOne().usuario)
        assertEquals(1L, database.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
        assertEquals(0L, database.appDatabaseQueries.countPendingOperations("new-user").executeAsOne())
    }

    @Test fun `private drafts are visible only to owner while confirmed receipts remain shared`() = runBlocking {
        enqueue("admin-draft")
        tokens.current = tokens.current?.copy(username = "finance", token = "finance-token")
        enqueue("finance-draft")
        writer.upsertDetail(ComprobanteDto(id = 91, total = 4.0, fechaRegistro = "2026-10-29T00:00:00Z"))
        assertEquals(setOf("admin-draft", "remote-91"), visibleIds("admin"))
        assertEquals(setOf("finance-draft", "remote-91"), visibleIds("finance"))
    }

    @Test fun `unowned orphan drafts stay quarantined instead of becoming public`() {
        writer.insertDraft(draft(), "orphan", "", 1)
        assertTrue(visibleIds("admin").isEmpty())
        assertTrue(visibleIds("finance").isEmpty())
    }

    @Test fun `parked ambiguous upload remains visible exclusively to its owner`() = runBlocking {
        enqueue("admin-draft")
        store.markUploadInFlight(store.ready().single())
        assertTrue(store.ready().isEmpty())
        assertEquals(setOf("admin-draft"), visibleIds("admin"))
        assertTrue(visibleIds("finance").isEmpty())
    }

    @Test fun `cancellation inside transaction rolls back without orphan draft`() = runBlocking {
        val cancelled = CancellationException("worker stopped")
        val failure = runCatching {
            store.enqueue(PendingEntityType.COMPROBANTE, "receipt-a", PendingOperationType.UPLOAD, "{}") { owner ->
                writer.insertDraft(draft(), "receipt-a", owner, 1)
                throw cancelled
            }
        }.exceptionOrNull()
        assertSame(cancelled, failure)
        assertNothingPersisted("receipt-a")
    }

    private suspend fun enqueue(id: String) {
        store.enqueue(PendingEntityType.COMPROBANTE, id, PendingOperationType.UPLOAD, "{}") { owner ->
            writer.insertDraft(draft(), id, owner, 1)
        }
    }

    private fun visibleIds(owner: String): Set<String> =
        database.appDatabaseQueries.selectComprobanteSummariesForOwner(owner).executeAsList().map { it.local_id }.toSet()

    private fun assertNothingPersisted(id: String) {
        assertNull(database.appDatabaseQueries.selectComprobanteByLocalId(id).executeAsOneOrNull())
        assertTrue(database.appDatabaseQueries.selectProductosForComprobante(id).executeAsList().isEmpty())
        assertEquals(0L, database.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
    }

    private fun draft() = ComprobanteDraft(
        imagenUri = "content://private.example.test/receipt.jpg", total = 15.0,
        productos = listOf(ComprobanteProducto(1, "Papel", 1.0, 10.0, 10.0),
            ComprobanteProducto(2, "Lápices", 1.0, 5.0, 5.0)),
    )

    private class PausedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        fun runPending() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }

    private class QueueTokens : TokenStore {
        private val state = MutableStateFlow<AuthSession?>(AuthSession("test-token", "admin", "Admin", "Administrador", Long.MAX_VALUE))
        var current: AuthSession?
            get() = state.value
            set(value) { state.value = value }
        override fun observe(): Flow<AuthSession?> = state
        override suspend fun read() = state.value
        override suspend fun save(session: AuthSession) { state.value = session }
        override suspend fun clear() { state.value = null }
        override suspend fun clearIfTokenMatches(token: String): Boolean {
            if (state.value?.token != token) return false
            state.value = null
            return true
        }
    }
}
