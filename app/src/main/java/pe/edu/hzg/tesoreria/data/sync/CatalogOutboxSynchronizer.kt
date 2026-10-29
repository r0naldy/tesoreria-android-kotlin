package pe.edu.hzg.tesoreria.data.sync

import io.ktor.client.plugins.ClientRequestException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.hzg.tesoreria.data.remote.CatalogApi
import pe.edu.hzg.tesoreria.data.repository.toCents
import pe.edu.hzg.tesoreria.db.AppDatabase

/** Processes legacy offline drafts; new catalogue mutations are online-confirmed. */
internal class CatalogOutboxSynchronizer(
    private val database: AppDatabase,
    private val api: CatalogApi,
    private val store: PendingOperationStore,
    private val json: Json,
) {
    private val queries get() = database.appDatabaseQueries

    suspend fun synchronize(operation: PendingOperation) = store.catalogMutex.withLock {
        if (!store.operationStillExists(operation)) return@withLock
        val session = store.sessionFor(operation)
            ?: error("La sesión cambió; el borrador pertenece a otro usuario.")
        check(session.rol.equals("Administrador", true)) { "Solo un Administrador puede sincronizar catálogos." }
        if (operation.operation == PendingOperationType.CREATE) store.markUploadInFlight(operation)
        var createConfirmed = false
        try {
            when (operation.entityType) {
                PendingEntityType.PROGRAMA -> when (operation.operation) {
                    PendingOperationType.CREATE -> {
                        val payload = json.decodeFromString<PendingProgramaPayload>(operation.payloadJson)
                        val remoteId = api.createPrograma(payload.nombre, session.token)
                        createConfirmed = true
                        check(remoteId > 0) { "El servidor no devolvió el ID del programa." }
                        val local = queries.selectProgramaById(payload.id.toLong()).executeAsOneOrNull()
                        database.transaction {
                            queries.deleteProgramaById(payload.id.toLong())
                            queries.upsertPrograma(remoteId.toLong(), payload.nombre, if (payload.activo) "Activo" else "Inactivo",
                                local?.fecha_creacion.orEmpty(), System.currentTimeMillis())
                            if (!payload.activo) enqueueFinalState(operation, remoteId,
                                json.encodeToString(payload.copy(id = remoteId)))
                            // Atomic with remapping: a process restart never repeats the POST.
                            queries.deletePendingOperation(operation.id, operation.ownerIdentity)
                        }
                    }
                    PendingOperationType.UPDATE -> {
                        val payload = json.decodeFromString<PendingProgramaPayload>(operation.payloadJson)
                        api.updatePrograma(payload.id, payload.nombre, payload.activo, session.token)
                    }
                    PendingOperationType.DELETE -> {
                        val payload = json.decodeFromString<PendingDeletePayload>(operation.payloadJson)
                        deleteIdempotently { api.deletePrograma(payload.id, session.token) }
                        queries.deleteProgramaById(payload.id.toLong())
                    }
                    else -> error("Operación de programa no soportada.")
                }
                PendingEntityType.SERVICIO -> when (operation.operation) {
                    PendingOperationType.CREATE -> {
                        val payload = json.decodeFromString<PendingServicioPayload>(operation.payloadJson)
                        val remoteId = api.createServicio(payload.nombre, payload.precio, session.token)
                        createConfirmed = true
                        check(remoteId > 0) { "El servidor no devolvió el ID del servicio." }
                        database.transaction {
                            queries.deleteServicioById(payload.id.toLong())
                            queries.upsertServicio(remoteId.toLong(), payload.nombre, payload.precio.toCents(),
                                if (payload.activo) "Activo" else "Inactivo", System.currentTimeMillis())
                            if (!payload.activo) enqueueFinalState(operation, remoteId,
                                json.encodeToString(payload.copy(id = remoteId)))
                            queries.deletePendingOperation(operation.id, operation.ownerIdentity)
                        }
                    }
                    PendingOperationType.UPDATE -> {
                        val payload = json.decodeFromString<PendingServicioPayload>(operation.payloadJson)
                        api.updateServicio(payload.id, payload.nombre, payload.precio, payload.activo, session.token)
                    }
                    PendingOperationType.DELETE -> {
                        val payload = json.decodeFromString<PendingDeletePayload>(operation.payloadJson)
                        deleteIdempotently { api.deleteServicio(payload.id, session.token) }
                        queries.deleteServicioById(payload.id.toLong())
                    }
                    else -> error("Operación de servicio no soportada.")
                }
                else -> error("Tipo de catálogo no soportado.")
            }
        } catch (cancelled: CancellationException) {
            // A CREATE remains parked if cancellation happened after sending it.
            throw cancelled
        } catch (error: Throwable) {
            if (operation.operation == PendingOperationType.CREATE) {
                throw CatalogPostFailure(!createConfirmed && ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(error), error)
            }
            throw error
        }
    }

    private fun enqueueFinalState(operation: PendingOperation, remoteId: Int, payload: String) {
        queries.insertPendingOperation(UUID.randomUUID().toString(), operation.ownerIdentity, operation.entityType,
            remoteId.toString(), PendingOperationType.UPDATE, payload, System.currentTimeMillis())
    }

    private suspend fun deleteIdempotently(block: suspend () -> Unit) {
        try { block() } catch (error: ClientRequestException) {
            if (error.response.status.value != 404) throw error
        }
    }
}

internal class CatalogPostFailure(val retryIsSafe: Boolean, cause: Throwable) :
    Exception("No se pudo confirmar la creación del catálogo. Actualiza el listado antes de volver a crear.", cause)
