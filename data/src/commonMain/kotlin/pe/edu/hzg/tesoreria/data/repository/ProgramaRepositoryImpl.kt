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
import pe.edu.hzg.tesoreria.data.remote.dto.ProgramaDto
import pe.edu.hzg.tesoreria.domain.model.SessionIdentity
import pe.edu.hzg.tesoreria.data.sync.PendingEntityType
import pe.edu.hzg.tesoreria.data.sync.PendingOperationStore
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.repository.ProgramaRepository

/** Offline reads, server-confirmed writes: a rejected DELETE never hides a row. */
class ProgramaRepositoryImpl(
    private val database: AppDatabase,
    private val api: CatalogApi,
    private val pendingStore: PendingOperationStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProgramaRepository {
    private val queries get() = database.appDatabaseQueries

    override fun observeProgramas(soloActivos: Boolean): Flow<List<Programa>> {
        val cached = if (soloActivos) queries.selectActiveProgramas(::mapPrograma)
            else queries.selectAllProgramas(::mapPrograma)
        return combine(cached.asListFlow(ioDispatcher), pendingStore.observeCatalogPending(PendingEntityType.PROGRAMA)) { rows, pending ->
            rows.filter { it.id > 0 || it.id in pending }.map {
                it.copy(syncPending = it.id in pending, syncError = pending[it.id])
            }
        }
    }

    override suspend fun refresh(soloActivos: Boolean): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            withContext(ioDispatcher) {
                val remote = api.getProgramas(soloActivos)
                val pendingIds = pendingStore.allPendingEntityIds(PendingEntityType.PROGRAMA)
                database.transaction {
                    applySnapshot(remote, pendingIds, removeAbsent = !soloActivos)
                }
            }
        }
    }

    override suspend fun create(nombre: String): Result<Int> = catalogCall(isCreate = true) {
        val normalized = validatedCatalogName(nombre)
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            withContext(ioDispatcher) {
                val id = api.createPrograma(normalized, session.token)
                check(id > 0) { "El servidor no devolvió un identificador válido. Actualiza el listado antes de volver a crear." }
                queries.upsertPrograma(id.toLong(), normalized, "Activo", utcIsoNow(), nowMillis())
                id
            }
        }
    }

    override suspend fun update(programa: Programa): Result<Unit> = catalogCall {
        val normalized = validatedCatalogName(programa.nombre)
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            ensureNotPending(programa.id)
            withContext(ioDispatcher) {
                api.updatePrograma(programa.id, normalized, programa.activo, session.token)
                val date = queries.selectProgramaById(programa.id.toLong()).executeAsOneOrNull()?.fecha_creacion
                    ?: programa.fechaCreacion
                queries.upsertPrograma(programa.id.toLong(), normalized, if (programa.activo) "Activo" else "Inactivo", date, nowMillis())
            }
        }
    }

    override suspend fun delete(id: Int): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            ensureNotPending(id)
            withContext(ioDispatcher) {
                try {
                    api.deletePrograma(id, session.token)
                } catch (error: ClientRequestException) {
                    if (error.response.status.value != 404) throw error
                }
                queries.deleteProgramaById(id.toLong())
            }
        }
    }

    private suspend fun ensureNotPending(id: Int) {
        check(id > 0 && id.toString() !in pendingStore.allPendingEntityIds(PendingEntityType.PROGRAMA)) {
            "Este programa tiene un cambio pendiente. Sincroniza o revisa el error pendiente antes de modificarlo."
        }
    }

    override suspend fun discardPending(id: Int): Result<Unit> = catalogCall {
        pendingStore.catalogMutex.withLock {
            val session = pendingStore.requireCatalogSession()
            val owner = SessionIdentity.fromSession(session)
            withContext(ioDispatcher) {
                val owners = queries.selectPendingOwnersForEntity(PendingEntityType.PROGRAMA, id.toString()).executeAsList()
                check(owner in owners) { "No hay un borrador pendiente de tu usuario para este programa." }
                check(owners.all { it == owner }) { "Otro usuario también tiene un cambio pendiente en este programa. No se descartó ningún cambio." }
                // A possibly successful earlier POST is recovered by GET, never undone remotely.
                val remote = api.getProgramas(false, session.token)
                check(pendingStore.requireCatalogSession().token == session.token) { "La sesión cambió. Vuelve a abrir el catálogo; el borrador se conservó." }
                val protectedIds = pendingStore.allPendingEntityIds(PendingEntityType.PROGRAMA) - id.toString()
                database.transaction {
                    queries.deletePendingForEntity(owner, PendingEntityType.PROGRAMA, id.toString())
                    if (id < 0) queries.deleteProgramaById(id.toLong())
                    applySnapshot(remote, protectedIds, removeAbsent = true)
                }
            }
        }
    }

    /** Invoked inside the caller's transaction; partial snapshots never purge cache. */
    private fun applySnapshot(remote: List<ProgramaDto>, protectedIds: Set<String>, removeAbsent: Boolean) {
        val now = nowMillis()
        if (removeAbsent) {
            val remoteIds = remote.map { it.id.toLong() }.toSet()
            queries.selectAllProgramas().executeAsList()
                .filter { it.id > 0 && it.id !in remoteIds && it.id.toString() !in protectedIds }
                .forEach { queries.deleteProgramaById(it.id) }
        }
        remote.filterNot { it.id.toString() in protectedIds }.forEach {
            queries.upsertPrograma(it.id.toLong(), it.nombre, it.estado, it.fechaCreacion, now)
        }
    }

    private fun mapPrograma(id: Long, nombre: String, estado: String, fechaCreacion: String,
        @Suppress("UNUSED_PARAMETER") updatedAt: Long) = Programa(id.toInt(), nombre, estado, fechaCreacion)
}
