package pe.edu.hzg.tesoreria.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pe.edu.hzg.tesoreria.data.remote.CatalogApi
import pe.edu.hzg.tesoreria.data.remote.dto.ProgramaDto
import pe.edu.hzg.tesoreria.data.remote.dto.ServicioDto
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.data.sync.*
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.model.Servicio

/** Uses real SQLite and the repository boundary; never calls the production API. */
class CatalogCrudRegressionTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: AppDatabase
    private lateinit var tokens: CatalogTestTokens
    private lateinit var store: PendingOperationStore
    private lateinit var api: CatalogTestApi
    private lateinit var programs: ProgramaRepositoryImpl
    private lateinit var services: ServicioRepositoryImpl

    @Before fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        db = AppDatabase(driver)
        tokens = CatalogTestTokens()
        store = PendingOperationStore(db, tokens, Dispatchers.Unconfined)
        api = CatalogTestApi()
        programs = ProgramaRepositoryImpl(db, api, store, Dispatchers.Unconfined)
        services = ServicioRepositoryImpl(db, api, store, Dispatchers.Unconfined)
    }

    @After fun tearDown() = driver.close()

    @Test fun `program complete CRUD uses server ID and commits only confirmed values`() = runBlocking {
        val id = programs.create("  Gastronomía  ").getOrThrow()
        assertEquals(41, id)
        var item = programs.observeProgramas().first().single()
        assertEquals("Gastronomía", item.nombre)
        assertFalse(item.syncPending)
        programs.update(item.copy(nombre = "Cocina", estado = "Inactivo")).getOrThrow()
        item = programs.observeProgramas().first().single()
        assertEquals("Cocina", item.nombre)
        assertFalse(item.activo)
        programs.delete(id).getOrThrow()
        assertTrue(programs.observeProgramas().first().isEmpty())
        assertEquals(listOf("program:create", "program:update:41", "program:delete:41"), api.calls)
        assertTrue(store.ready().isEmpty())
    }

    @Test fun `service complete CRUD saves cents and active state correctly`() = runBlocking {
        val id = services.create(" Constancia ", 15.25).getOrThrow()
        assertEquals(51, id)
        var item = services.observeServicios().first().single()
        assertEquals(1525L, db.appDatabaseQueries.selectServicioById(id.toLong()).executeAsOne().precio_centimos)
        services.update(item.copy(nombre = "Constancia actualizada", precio = 20.50, estado = "Inactivo")).getOrThrow()
        item = services.observeServicios().first().single()
        assertEquals(20.50, item.precio, 0.0)
        assertFalse(item.activo)
        services.delete(id).getOrThrow()
        assertTrue(services.observeServicios().first().isEmpty())
        assertEquals(listOf("service:create", "service:update:51", "service:delete:51"), api.calls)
    }

    @Test fun `rejected program creation does not invent a successful local row`() = runBlocking {
        api.error = CatalogOperationException("Ya existe un programa con ese nombre.")
        val result = programs.create("Duplicado")
        assertTrue(result.isFailure)
        assertEquals("Ya existe un programa con ese nombre.", result.exceptionOrNull()?.message)
        assertTrue(db.appDatabaseQueries.selectAllProgramas().executeAsList().isEmpty())
        assertTrue(store.ready().isEmpty())
    }

    @Test fun `rejected service creation does not create offline ghost or retry`() = runBlocking {
        api.error = UnknownHostException("offline")
        assertTrue(services.create("Servicio", 5.0).isFailure)
        assertTrue(db.appDatabaseQueries.selectAllServicios().executeAsList().isEmpty())
        assertTrue(store.ready().isEmpty())
    }

    @Test fun `failed program edit preserves last server confirmed record`() = runBlocking {
        seedProgram()
        api.error = CatalogOperationException("Nombre duplicado")
        assertTrue(programs.update(Programa(1, "Cambiar", "Inactivo", "date")).isFailure)
        val row = db.appDatabaseQueries.selectProgramaById(1).executeAsOne()
        assertEquals("Original", row.nombre)
        assertEquals("Activo", row.estado)
    }

    @Test fun `failed service edit preserves amount and status`() = runBlocking {
        seedService()
        api.error = UnknownHostException()
        assertTrue(services.update(Servicio(2, "Nuevo", 8.0, "Inactivo")).isFailure)
        val row = db.appDatabaseQueries.selectServicioById(2).executeAsOne()
        assertEquals("Original", row.nombre)
        assertEquals(500L, row.precio_centimos)
        assertEquals("Activo", row.estado)
    }

    @Test fun `protected program deletion stays visible and reports backend reason`() = runBlocking {
        seedProgram()
        api.error = CatalogOperationException("No se puede eliminar: el programa tiene alumnos asociados. Desactívelo en su lugar.")
        val result = programs.delete(1)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Desactívelo"))
        assertNotNull(db.appDatabaseQueries.selectProgramaById(1).executeAsOneOrNull())
    }

    @Test fun `protected service deletion never removes displayed row`() = runBlocking {
        seedService()
        api.error = CatalogOperationException("El servicio ya fue cobrado en tickets.")
        assertTrue(services.delete(2).isFailure)
        assertNotNull(db.appDatabaseQueries.selectServicioById(2).executeAsOneOrNull())
    }

    @Test fun `invalid names are rejected before REST for create and edit`() = runBlocking {
        assertTrue(programs.create("  ").isFailure)
        assertTrue(programs.update(Programa(1, "x".repeat(201), "Activo", "")).isFailure)
        assertTrue(services.create("x".repeat(201), 1.0).isFailure)
        assertTrue(services.update(Servicio(2, "", 1.0, "Activo")).isFailure)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `invalid prices are rejected before REST including NaN infinity and decimals`() = runBlocking {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, 100_000_000.0, 1.001).forEach { price ->
            assertTrue("create $price", services.create("Servicio", price).isFailure)
            assertTrue("update $price", services.update(Servicio(2, "Servicio", price, "Activo")).isFailure)
        }
        assertTrue(api.calls.isEmpty())
        assertEquals(0.0, validatedCatalogPrice(0.0), 0.0)
        assertEquals(99_999_999.99, validatedCatalogPrice(99_999_999.99), 0.0)
    }

    @Test fun `finance role cannot mutate either catalog even through repository`() = runBlocking {
        tokens.session = tokens.session.copy(rol = "Finanzas")
        assertTrue(programs.create("Nombre").isFailure)
        assertTrue(programs.update(Programa(1, "Nombre", "Activo", "")).isFailure)
        assertTrue(programs.delete(1).isFailure)
        assertTrue(services.create("Nombre", 1.0).isFailure)
        assertTrue(services.update(Servicio(2, "Nombre", 1.0, "Activo")).isFailure)
        assertTrue(services.delete(2).isFailure)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `expired session does not mutate or process a pending operation`() = runBlocking {
        val id = store.enqueue(PendingEntityType.PROGRAMA, "-1", PendingOperationType.CREATE, "{}")
        val operation = store.ready().single { it.id == id }
        tokens.session = tokens.session.copy(expiresAtEpochMillis = 1)
        assertTrue(programs.create("Nombre").isFailure)
        assertTrue(store.ready().isEmpty())
        assertNull(store.sessionFor(operation))
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `full refresh reconciles deleted server rows instead of resurrecting them`() = runBlocking {
        seedProgram(); seedService()
        programs.refresh().getOrThrow()
        services.refresh().getOrThrow()
        assertTrue(db.appDatabaseQueries.selectAllProgramas().executeAsList().isEmpty())
        assertTrue(db.appDatabaseQueries.selectAllServicios().executeAsList().isEmpty())
    }

    @Test fun `active-only refresh never purges inactive cached records`() = runBlocking {
        seedProgram("Inactivo"); seedService("Inactivo")
        programs.refresh(soloActivos = true).getOrThrow()
        services.refresh(soloActivos = true).getOrThrow()
        assertEquals(1, db.appDatabaseQueries.selectAllProgramas().executeAsList().size)
        assertEquals(1, db.appDatabaseQueries.selectAllServicios().executeAsList().size)
    }

    @Test fun `pending drafts belong to owner and refresh protects all owner queues`() = runBlocking {
        db.appDatabaseQueries.upsertPrograma(-9, "Borrador", "Activo", "", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "-9", PendingOperationType.CREATE, "{}")
        assertTrue(programs.observeProgramas().first().single().syncPending)
        tokens.session = tokens.session.copy(username = "other")
        assertTrue(programs.observeProgramas().first().isEmpty())
        programs.refresh().getOrThrow()
        assertNotNull(db.appDatabaseQueries.selectProgramaById(-9).executeAsOneOrNull())
    }

    @Test fun `pending error is visible and blocks overwriting unresolved legacy draft`() = runBlocking {
        seedService()
        store.enqueue(PendingEntityType.SERVICIO, "2", PendingOperationType.UPDATE, "{}")
        store.markRequiresReview(store.ready().single(), IllegalStateException("Precio rechazado"))
        val row = services.observeServicios().first().single()
        assertTrue(row.syncPending)
        assertTrue(row.syncError.orEmpty().contains("Precio rechazado"))
        assertTrue(services.update(row.copy(precio = 9.0)).isFailure)
        assertTrue(services.delete(2).isFailure)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `legacy inactive program create remaps once then queues PUT not second POST`() = runBlocking {
        val json = Json
        db.appDatabaseQueries.upsertPrograma(-4, "Programa", "Inactivo", "date", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "-4", PendingOperationType.CREATE,
            json.encodeToString(PendingProgramaPayload(-4, "Programa", false)))
        val operation = store.ready().single()
        val sync = CatalogOutboxSynchronizer(db, api, store, json)
        sync.synchronize(operation)
        sync.synchronize(operation)
        val update = store.ready().single()
        assertEquals(PendingOperationType.UPDATE, update.operation)
        assertEquals("41", update.entityLocalId)
        sync.synchronize(update)
        assertEquals(listOf("program:create", "program:update:41"), api.calls)
        assertNull(db.appDatabaseQueries.selectProgramaById(-4).executeAsOneOrNull())
        assertEquals("Inactivo", db.appDatabaseQueries.selectProgramaById(41).executeAsOne().estado)
    }

    @Test fun `legacy create lost response is parked and not blindly replayed`() = runBlocking {
        store.enqueue(PendingEntityType.SERVICIO, "-3", PendingOperationType.CREATE,
            Json.encodeToString(PendingServicioPayload(-3, "Servicio", 5.0, true)))
        val operation = store.ready().single()
        api.error = SocketTimeoutException("response lost")
        val failure = runCatching { CatalogOutboxSynchronizer(db, api, store, Json).synchronize(operation) }.exceptionOrNull()
        assertTrue(failure is CatalogPostFailure)
        assertFalse((failure as CatalogPostFailure).retryIsSafe)
        assertTrue(store.ready().isEmpty())
        assertEquals(1, api.calls.size)
    }

    @Test fun `legacy DNS failure is the only type safe to retry before any POST arrives`() = runBlocking {
        store.enqueue(PendingEntityType.PROGRAMA, "-2", PendingOperationType.CREATE,
            Json.encodeToString(PendingProgramaPayload(-2, "Programa", true)))
        val operation = store.ready().single()
        api.error = UnknownHostException()
        val failure = runCatching { CatalogOutboxSynchronizer(db, api, store, Json).synchronize(operation) }.exceptionOrNull()
        assertTrue(failure is CatalogPostFailure)
        assertTrue((failure as CatalogPostFailure).retryIsSafe)
    }

    @Test fun `cancellation is not swallowed or converted into a saved operation`() = runBlocking {
        api.error = CancellationException("cancel")
        val failure = runCatching { programs.create("Nombre") }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertTrue(db.appDatabaseQueries.selectAllProgramas().executeAsList().isEmpty())
        assertTrue(store.ready().isEmpty())
    }

    @Test fun `network refresh failure keeps last confirmed offline data`() = runBlocking {
        seedProgram(); seedService()
        api.error = UnknownHostException()
        assertTrue(programs.refresh().isFailure)
        assertTrue(services.refresh().isFailure)
        assertEquals("Original", programs.observeProgramas().first().single().nombre)
        assertEquals("Original", services.observeServicios().first().single().nombre)
    }

    @Test fun `discard ambiguous program creation fetches real record and never deletes remotely`() = runBlocking {
        db.appDatabaseQueries.upsertPrograma(-4, "Propuesta", "Activo", "date", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "-4", PendingOperationType.CREATE, "{}")
        store.markUploadInFlight(store.ready().single())
        api.programList = listOf(ProgramaDto(72, "Programa confirmado", "Activo", "date"))
        programs.discardPending(-4).getOrThrow()
        assertNull(db.appDatabaseQueries.selectProgramaById(-4).executeAsOneOrNull())
        assertEquals("Programa confirmado", programs.observeProgramas().first().single().nombre)
        assertFalse(programs.observeProgramas().first().single().syncPending)
        assertEquals(0L, db.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
        assertEquals(listOf("program:false:test-token"), api.readCalls)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `discard pending program edit restores server fields without PUT`() = runBlocking {
        db.appDatabaseQueries.upsertPrograma(1, "Cambio local rechazado", "Inactivo", "date", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "1", PendingOperationType.UPDATE, "{}")
        api.programList = listOf(ProgramaDto(1, "Nombre confirmado", "Activo", "server-date"))
        programs.discardPending(1).getOrThrow()
        val restored = programs.observeProgramas().first().single()
        assertEquals("Nombre confirmado", restored.nombre)
        assertTrue(restored.activo)
        assertEquals("server-date", restored.fechaCreacion)
        assertTrue(store.ready().isEmpty())
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `discard service draft keeps real receipt operation and other proposals untouched`() = runBlocking {
        db.appDatabaseQueries.upsertServicio(-8, "Borrador", 100, "Activo", 1)
        db.appDatabaseQueries.upsertServicio(2, "Otro cambio pendiente", 900, "Inactivo", 1)
        store.enqueue(PendingEntityType.SERVICIO, "-8", PendingOperationType.CREATE, "{}")
        store.enqueue(PendingEntityType.SERVICIO, "2", PendingOperationType.UPDATE, "{}")
        store.enqueue(PendingEntityType.COMPROBANTE, "receipt-private", PendingOperationType.UPLOAD, "{}")
        api.serviceList = listOf(ServicioDto(2, "Servidor anterior", 5.0, "Activo"), ServicioDto(63, "Servidor nuevo", 6.5, "Activo"))
        services.discardPending(-8).getOrThrow()
        assertNull(db.appDatabaseQueries.selectServicioById(-8).executeAsOneOrNull())
        assertEquals("Otro cambio pendiente", db.appDatabaseQueries.selectServicioById(2).executeAsOne().nombre)
        assertEquals("Servidor nuevo", db.appDatabaseQueries.selectServicioById(63).executeAsOne().nombre)
        assertEquals(setOf("2", "receipt-private"), store.ready().map { it.entityLocalId }.toSet())
        assertTrue(api.calls.isEmpty())
        assertEquals(listOf("service:false:test-token"), api.readCalls)
    }

    @Test fun `discard service pending delete restores server row that optimistic code hid`() = runBlocking {
        store.enqueue(PendingEntityType.SERVICIO, "51", PendingOperationType.DELETE, "{}")
        api.serviceList = listOf(ServicioDto(51, "Servicio con tickets", 15.0, "Activo"))
        services.discardPending(51).getOrThrow()
        assertEquals("Servicio con tickets", services.observeServicios().first().single().nombre)
        assertTrue(store.ready().isEmpty())
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `discard needs successful snapshot and preserves draft while offline`() = runBlocking {
        db.appDatabaseQueries.upsertPrograma(-4, "Propuesta", "Activo", "date", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "-4", PendingOperationType.CREATE, "{}")
        api.error = UnknownHostException()
        assertTrue(programs.discardPending(-4).isFailure)
        assertNotNull(db.appDatabaseQueries.selectProgramaById(-4).executeAsOneOrNull())
        assertEquals("-4", store.ready().single().entityLocalId)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `another user cannot discard previous owner draft`() = runBlocking {
        db.appDatabaseQueries.upsertServicio(-6, "Borrador privado", 100, "Activo", 1)
        store.enqueue(PendingEntityType.SERVICIO, "-6", PendingOperationType.CREATE, "{}")
        tokens.session = tokens.session.copy(username = "other-admin")
        assertTrue(services.discardPending(-6).isFailure)
        assertNotNull(db.appDatabaseQueries.selectServicioById(-6).executeAsOneOrNull())
        assertEquals(1L, db.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
        assertTrue(api.readCalls.isEmpty())
    }

    @Test fun `shared positive row conflict never discards other owner proposals`() = runBlocking {
        seedProgram()
        store.enqueue(PendingEntityType.PROGRAMA, "1", PendingOperationType.UPDATE, "{}")
        tokens.session = tokens.session.copy(username = "other-admin")
        store.enqueue(PendingEntityType.PROGRAMA, "1", PendingOperationType.UPDATE, "{}")
        tokens.session = tokens.session.copy(username = "admin")
        assertTrue(programs.discardPending(1).isFailure)
        assertEquals(1L, db.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
        assertEquals(1L, db.appDatabaseQueries.countPendingOperations("other-admin").executeAsOne())
        assertTrue(api.readCalls.isEmpty())
    }

    @Test fun `session change while snapshot loads leaves pending data intact`() = runBlocking {
        db.appDatabaseQueries.upsertPrograma(-4, "Propuesta", "Activo", "date", 1)
        store.enqueue(PendingEntityType.PROGRAMA, "-4", PendingOperationType.CREATE, "{}")
        api.onRead = { tokens.session = tokens.session.copy(username = "new-admin", token = "new-token") }
        assertTrue(programs.discardPending(-4).isFailure)
        assertNotNull(db.appDatabaseQueries.selectProgramaById(-4).executeAsOneOrNull())
        assertEquals(1L, db.appDatabaseQueries.countPendingOperations("admin").executeAsOne())
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `discard without owned proposal cannot remove ordinary remote record`() = runBlocking {
        seedProgram(); seedService()
        assertTrue(programs.discardPending(1).isFailure)
        assertTrue(services.discardPending(2).isFailure)
        assertNotNull(db.appDatabaseQueries.selectProgramaById(1).executeAsOneOrNull())
        assertNotNull(db.appDatabaseQueries.selectServicioById(2).executeAsOneOrNull())
        assertTrue(api.readCalls.isEmpty())
        assertTrue(api.calls.isEmpty())
    }

    private fun seedProgram(state: String = "Activo") = db.appDatabaseQueries.upsertPrograma(1, "Original", state, "date", 1)
    private fun seedService(state: String = "Activo") = db.appDatabaseQueries.upsertServicio(2, "Original", 500, state, 1)
}

private class CatalogTestTokens : TokenStore {
    private val state = MutableStateFlow<AuthSession?>(AuthSession("test-token", "admin", "Admin", "Administrador", Long.MAX_VALUE))
    var session: AuthSession
        get() = requireNotNull(state.value)
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

private class CatalogTestApi : CatalogApi {
    val calls = mutableListOf<String>()
    val readCalls = mutableListOf<String>()
    var onRead: (() -> Unit)? = null
    var error: Throwable? = null
    var programList = emptyList<ProgramaDto>()
    var serviceList = emptyList<ServicioDto>()
    private fun checkCall(call: String, token: String?) {
        calls += call
        assertEquals("test-token", token)
        error?.let { throw it }
    }
    override suspend fun getProgramas(soloActivos: Boolean, bearerToken: String?): List<ProgramaDto> {
        readCalls += "program:$soloActivos:$bearerToken"
        onRead?.invoke()
        error?.let { throw it }; return programList
    }
    override suspend fun getServicios(soloActivos: Boolean, bearerToken: String?): List<ServicioDto> {
        readCalls += "service:$soloActivos:$bearerToken"
        onRead?.invoke()
        error?.let { throw it }; return serviceList
    }
    override suspend fun createPrograma(nombre: String, bearerToken: String?): Int {
        checkCall("program:create", bearerToken); return 41
    }
    override suspend fun createServicio(nombre: String, precio: Double, bearerToken: String?): Int {
        checkCall("service:create", bearerToken); return 51
    }
    override suspend fun updatePrograma(id: Int, nombre: String, activo: Boolean, bearerToken: String?) = checkCall("program:update:$id", bearerToken)
    override suspend fun updateServicio(id: Int, nombre: String, precio: Double, activo: Boolean, bearerToken: String?) = checkCall("service:update:$id", bearerToken)
    override suspend fun deletePrograma(id: Int, bearerToken: String?) = checkCall("program:delete:$id", bearerToken)
    override suspend fun deleteServicio(id: Int, bearerToken: String?) = checkCall("service:delete:$id", bearerToken)
}
