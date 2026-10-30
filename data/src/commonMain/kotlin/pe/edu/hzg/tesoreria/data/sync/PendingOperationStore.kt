@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package pe.edu.hzg.tesoreria.data.sync

import pe.edu.hzg.tesoreria.domain.util.nowMillis
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import pe.edu.hzg.tesoreria.data.local.asListFlow
import pe.edu.hzg.tesoreria.data.local.asOneFlow
import pe.edu.hzg.tesoreria.domain.model.SessionIdentity
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.AuthSession

class PendingOperationStore(
    private val database: AppDatabase,
    private val tokenStore: TokenStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val queries get() = database.appDatabaseQueries

    // One mutex for UI catalogue changes, refresh and the background outbox.
    // A worker must not recreate a row while a foreground edit/delete is running.
    val catalogMutex = Mutex()

    suspend fun requireCatalogSession(): AuthSession = tokenStore.read()
        ?.takeUnless { it.isExpired() }
        ?.also { check(it.rol.equals("Administrador", ignoreCase = true)) {
            "Solo un Administrador puede modificar este catálogo."
        } }
        ?: error("Se requiere una sesión activa para modificar el catálogo.")

    suspend fun allPendingEntityIds(entityType: String): Set<String> = withContext(ioDispatcher) {
        queries.selectAllPendingEntityIds(entityType).executeAsList().toSet()
    }

    suspend fun operationStillExists(operation: PendingOperation): Boolean = withContext(ioDispatcher) {
        queries.selectPendingById(operation.id, operation.ownerIdentity).executeAsOneOrNull() != null
    }

    fun observeOwnerIdentity(): Flow<String?> = tokenStore.observe()
        .map { session -> session?.takeUnless { it.isExpired() }?.let(SessionIdentity::fromSession) }
        .distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCatalogPending(entityType: String): Flow<Map<Int, String?>> = observeOwnerIdentity()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(emptyMap())
            else queries.selectPendingForType(owner, entityType).asListFlow(ioDispatcher)
                .map { rows -> rows.mapNotNull { row -> row.entity_local_id.toIntOrNull()?.let { it to row.last_error } }.toMap() }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCount(): Flow<Long> = observeOwnerIdentity()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(0L)
            else queries.countPendingOperations(owner).asOneFlow(ioDispatcher)
        }

    suspend fun enqueue(
        entityType: String,
        entityLocalId: String,
        operation: String,
        payloadJson: String,
        writeLocal: (ownerIdentity: String) -> Unit = {},
    ): String {
        // Capture before dispatching IO: a login switch while waiting for the
        // database must never transfer this operation to the next user.
        val owner = requireCurrentOwner()
        return withContext(ioDispatcher) {
            val id = Uuid.random().toString()
            database.transaction {
                // The receipt, its line items and its durable outbox entry are
                // one atomic write. The callback must contain database work only.
                writeLocal(owner)
                if (operation == PendingOperationType.DELETE) {
                    queries.deletePendingForEntity(owner, entityType, entityLocalId)
                } else if (operation == PendingOperationType.UPDATE) {
                    queries.deletePendingForEntityAndOperation(owner, entityType, entityLocalId, operation)
                }
                queries.insertPendingOperation(
                    id = id,
                    owner_identity = owner,
                    entity_type = entityType,
                    entity_local_id = entityLocalId,
                    operation = operation,
                    payload_json = payloadJson,
                    created_at = nowMillis(),
                )
            }
            id
        }
    }

    suspend fun replaceCreatePayload(
        entityType: String,
        entityLocalId: String,
        payloadJson: String,
    ): Boolean = withContext(ioDispatcher) {
        val owner = requireCurrentOwner()
        val pending = queries.findPendingOperation(
            owner_identity = owner,
            entity_type = entityType,
            entity_local_id = entityLocalId,
            operation = PendingOperationType.CREATE,
        ).executeAsOneOrNull() ?: return@withContext false
        queries.updatePendingPayload(payload_json = payloadJson, id = pending.id, owner_identity = owner)
        true
    }

    suspend fun pendingEntityIds(entityType: String): Set<String> = withContext(ioDispatcher) {
        val owner = currentOwner() ?: return@withContext emptySet()
        queries.selectPendingEntityIds(owner, entityType).executeAsList().toSet()
    }

    suspend fun ready(limit: Long = 25): List<PendingOperation> = withContext(ioDispatcher) {
        val owner = currentOwner() ?: return@withContext emptyList()
        queries.selectReadyPendingOperations(
            owner_identity = owner,
            now = nowMillis(),
            limit = limit,
        ) { id, ownerIdentity, entityType, entityLocalId, operation, payload, attempts, lastError, createdAt, nextAttemptAt ->
            PendingOperation(
                id = id,
                ownerIdentity = ownerIdentity,
                entityType = entityType,
                entityLocalId = entityLocalId,
                operation = operation,
                payloadJson = payload,
                attempts = attempts.toInt(),
                lastError = lastError,
                createdAt = createdAt,
                nextAttemptAt = nextAttemptAt,
            )
        }.executeAsList()
    }

    suspend fun markSucceeded(operation: PendingOperation) = withContext(ioDispatcher) {
        queries.deletePendingOperation(operation.id, operation.ownerIdentity)
    }

    suspend fun markFailed(operation: PendingOperation, error: Throwable) = withContext(ioDispatcher) {
        val delay = (30_000L shl operation.attempts.coerceAtMost(10))
            .coerceAtMost(6 * 60 * 60 * 1_000L)
        queries.markPendingFailure(
            last_error = error.message?.take(500) ?: error::class.simpleName,
            next_attempt_at = nowMillis() + delay,
            id = operation.id,
            owner_identity = operation.ownerIdentity,
        )
    }

    suspend fun markUploadInFlight(operation: PendingOperation) = withContext(ioDispatcher) {
        queries.markPendingInFlight(
            last_error = UPLOAD_IN_FLIGHT,
            id = operation.id,
            owner_identity = operation.ownerIdentity,
        )
    }

    suspend fun markRequiresReview(operation: PendingOperation, error: Throwable) = withContext(ioDispatcher) {
        val detail = error.message?.take(350) ?: error::class.simpleName.orEmpty()
        val message = "$REQUIRES_REVIEW $detail".take(500)
        database.transaction {
            queries.markPendingRequiresReview(
                last_error = message,
                id = operation.id,
                owner_identity = operation.ownerIdentity,
            )
            if (operation.entityType == PendingEntityType.COMPROBANTE) {
                queries.markComprobanteSyncError(
                    sync_error = message,
                    updated_at = nowMillis(),
                    local_id = operation.entityLocalId,
                )
            }
        }
    }

    suspend fun removeEntity(entityType: String, entityLocalId: String) = withContext(ioDispatcher) {
        val owner = requireCurrentOwner()
        queries.deletePendingForEntity(owner, entityType, entityLocalId)
    }

    suspend fun sessionFor(operation: PendingOperation): AuthSession? =
        tokenStore.read()?.takeUnless { it.isExpired() }
            ?.takeIf { SessionIdentity.fromSession(it) == operation.ownerIdentity }

    private suspend fun currentOwner(): String? =
        tokenStore.read()?.takeUnless { it.isExpired() }?.let(SessionIdentity::fromSession)

    private suspend fun requireCurrentOwner(): String = currentOwner()
        ?: error("Se requiere una sesión activa para guardar una operación pendiente.")

    companion object {
        const val UPLOAD_IN_FLIGHT = "ENVIO_EN_CURSO_SIN_CONFIRMAR"
        const val REQUIRES_REVIEW = "REQUIERE_VERIFICACION_MANUAL: no se reintentará automáticamente."
    }
}
