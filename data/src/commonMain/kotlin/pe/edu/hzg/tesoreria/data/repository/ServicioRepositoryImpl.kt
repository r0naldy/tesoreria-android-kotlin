package pe.edu.hzg.tesoreria.data.repository

import pe.edu.hzg.tesoreria.domain.util.nowMillis
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pe.edu.hzg.tesoreria.data.local.asListFlow
import pe.edu.hzg.tesoreria.data.remote.CatalogApi
import pe.edu.hzg.tesoreria.data.remote.dto.ServicioDto
import pe.edu.hzg.tesoreria.domain.model.SessionIdentity
import pe.edu.hzg.tesoreria.data.sync.PendingEntityType
import pe.edu.hzg.tesoreria.data.sync.PendingOperationStore
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.Servicio
import pe.edu.hzg.tesoreria.domain.repository.ServicioRepository

class ServicioRepositoryImpl(
    private val database: AppDatabase,
    private val api: CatalogApi,
    private val pendingStore: PendingOperationStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ServicioRepository {
    private val queries get() = database.appDatabaseQueries

    override fun observeServicios(soloActivos: Boolean): Flow<List<Servicio>> {
        val cached = if (soloActivos) queries.selectActiveServicios(::mapServicio)
            else queries.selectAllServicios(::mapServicio)
        return combine(cached.asListFlow(ioDispatcher), pendingStore.observeCatalogPending(PendingEntityType.SERVICIO)) { rows, pending ->
            rows.filter { it.id > 0 || it.id in pending }.map {
                it.copy(syncPending = it.id in pending, syncError = pending[it.id])
            }
        }
    }

    override suspend fun refresh(soloActivos: Boolean): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            withContext(ioDispatcher) {
                val remote = api.getServicios(soloActivos)
                val pendingIds = pendingStore.allPendingEntityIds(PendingEntityType.SERVICIO)
                database.transaction {
                    applySnapshot(remote, pendingIds, removeAbsent = !soloActivos)
                }
            }
        }
    }

    override suspend fun create(nombre: String, precio: Double): Result<Int> = catalogCall(isCreate = true) {
        val normalized = validatedCatalogName(nombre)
        val amount = validatedCatalogPrice(precio)
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            withContext(ioDispatcher) {
                val id = api.createServicio(normalized, amount, session.token)
                check(id > 0) { "El servidor no devolvió un identificador válido. Actualiza el listado antes de volver a crear." }
                queries.upsertServicio(id.toLong(), normalized, amount.toCents(), "Activo", nowMillis())
                id
            }
        }
    }

    override suspend fun update(servicio: Servicio): Result<Unit> = catalogCall {
        val normalized = validatedCatalogName(servicio.nombre)
        val amount = validatedCatalogPrice(servicio.precio)
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            ensureNotPending(servicio.id)
            withContext(ioDispatcher) {
                api.updateServicio(servicio.id, normalized, amount, servicio.activo, session.token)
                queries.upsertServicio(servicio.id.toLong(), normalized, amount.toCents(), if (servicio.activo) "Activo" else "Inactivo", nowMillis())
            }
        }
    }

    override suspend fun delete(id: Int): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            ensureNotPending(id)
            withContext(ioDispatcher) {
                try {
                    api.deleteServicio(id, session.token)
                } catch (error: ClientRequestException) {
                    if (error.response.status.value != 404) throw error
                }
                queries.deleteServicioById(id.toLong())
            }
        }
    }

    private suspend fun ensureNotPending(id: Int) {
        check(id > 0 && id.toString() !in pendingStore.allPendingEntityIds(PendingEntityType.SERVICIO)) {
            "Este servicio tiene un cambio pendiente. Sincroniza o revisa el error pendiente antes de modificarlo."
        }
    }

    override suspend fun discardPending(id: Int): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            val owner = SessionIdentity.fromSession(session)
            withContext(ioDispatcher) {
                val owners = queries.selectPendingOwnersForEntity(PendingEntityType.SERVICIO, id.toString()).executeAsList()
                check(owner in owners) { "No hay un borrador pendiente de tu usuario para este servicio." }
                check(owners.all { it == owner }) { "Otro usuario también tiene un cambio pendiente en este servicio. No se descartó ningún cambio." }
                val remote = api.getServicios(false, session.token)
                check(pendingStore.requireCatalogSession().token == session.token) { "La sesión cambió. Vuelve a abrir el catálogo; el borrador se conservó." }
                val protectedIds = pendingStore.allPendingEntityIds(PendingEntityType.SERVICIO) - id.toString()
                database.transaction {
                    queries.deletePendingForEntity(owner, PendingEntityType.SERVICIO, id.toString())
                    if (id < 0) queries.deleteServicioById(id.toLong())
                    applySnapshot(remote, protectedIds, removeAbsent = true)
                }
            }
        }
    }

    /** Invoked inside the caller's transaction; partial snapshots never purge cache. */
    private fun applySnapshot(remote: List<ServicioDto>, protectedIds: Set<String>, removeAbsent: Boolean) {
        val now = nowMillis()
        if (removeAbsent) {
            val remoteIds = remote.map { it.id.toLong() }.toSet()
            queries.selectAllServicios().executeAsList()
                .filter { it.id > 0 && it.id !in remoteIds && it.id.toString() !in protectedIds }
                .forEach { queries.deleteServicioById(it.id) }
        }
        remote.filterNot { it.id.toString() in protectedIds }.forEach {
            queries.upsertServicio(it.id.toLong(), it.nombre, it.precio.toCents(), it.estado, now)
        }
    }

    private fun mapServicio(id: Long, nombre: String, precioCentimos: Long, estado: String,
        @Suppress("UNUSED_PARAMETER") updatedAt: Long) = Servicio(id.toInt(), nombre, precioCentimos.fromCents(), estado)
}
