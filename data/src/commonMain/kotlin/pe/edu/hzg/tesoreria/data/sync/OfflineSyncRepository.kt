package pe.edu.hzg.tesoreria.data.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException
import pe.edu.hzg.tesoreria.data.repository.CatalogOperationException
import pe.edu.hzg.tesoreria.data.repository.catalogErrorMessage
import pe.edu.hzg.tesoreria.data.coroutines.runSuspendCatching
import pe.edu.hzg.tesoreria.data.local.ReceiptImageReader
import pe.edu.hzg.tesoreria.data.remote.TesoreriaApi
import pe.edu.hzg.tesoreria.data.repository.ComprobanteLocalWriter
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.SyncSummary
import pe.edu.hzg.tesoreria.domain.repository.SyncRepository

class OfflineSyncRepository(
    private val database: AppDatabase,
    private val api: TesoreriaApi,
    private val pendingStore: PendingOperationStore,
    private val imageReader: ReceiptImageReader,
    private val comprobanteWriter: ComprobanteLocalWriter,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SyncRepository {
    private val syncMutex = Mutex()
    private val catalogSync = CatalogOutboxSynchronizer(database, api, pendingStore, json)

    override fun observePendingCount(): Flow<Long> = pendingStore.observeCount()

    override suspend fun synchronize(): Result<SyncSummary> = runSuspendCatching {
        syncMutex.withLock {
            withContext(ioDispatcher) {
                var processed = 0
                var succeeded = 0
                var failed = 0
                var sessionChanged = false
                var pending: List<PendingOperation>
                do {
                    pending = pendingStore.ready()
                    for (operation in pending) {
                        val session = pendingStore.sessionFor(operation)
                        if (session == null) {
                            // A logout/login happened after this batch was read.
                            // Leave every operation untouched for its real owner.
                            sessionChanged = true
                            break
                        }
                        processed++
                        try {
                            synchronizeOne(operation, session.token)
                            pendingStore.markSucceeded(operation)
                            succeeded++
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            when (error) {
                                is CatalogPostFailure -> {
                                    if (error.retryIsSafe) pendingStore.markFailed(operation, error)
                                    else pendingStore.markRequiresReview(operation, error)
                                }
                                is ComprobantePostFailure -> {
                                    if (error.retryIsSafe) pendingStore.markFailed(operation, error)
                                    else pendingStore.markRequiresReview(operation, error)
                                }
                                is ComprobantePostPersistFailure ->
                                    pendingStore.markRequiresReview(operation, error)
                                else -> {
                                    if (operation.entityType != PendingEntityType.COMPROBANTE &&
                                        (error is IllegalStateException ||
                                            (error is ClientRequestException && error.response.status.value != 429))) {
                                        pendingStore.markRequiresReview(operation, CatalogOperationException(catalogErrorMessage(error)))
                                    } else pendingStore.markFailed(operation, error)
                                }
                            }
                            failed++
                        }
                    }
                } while (!sessionChanged && pending.size == SYNC_BATCH_SIZE)

                SyncSummary(processed = processed, succeeded = succeeded, failed = failed)
            }
        }
    }

    private suspend fun synchronizeOne(operation: PendingOperation, bearerToken: String) {
        when (operation.entityType) {
            PendingEntityType.PROGRAMA, PendingEntityType.SERVICIO -> catalogSync.synchronize(operation)
            PendingEntityType.COMPROBANTE -> synchronizeComprobante(operation, bearerToken)
            else -> error("Tipo de operación pendiente no soportado: ${operation.entityType}")
        }
    }

    private suspend fun synchronizeComprobante(operation: PendingOperation, bearerToken: String) {
        check(operation.operation == PendingOperationType.UPLOAD) {
            "Operación de comprobante no soportada: ${operation.operation}"
        }
        val payload = json.decodeFromString<PendingComprobantePayload>(operation.payloadJson)
        val image = imageReader.read(payload.imagenUri)
        // Park before POST. If the process dies after this point, a later worker
        // cannot unknowingly create the same expense for a second time.
        pendingStore.markUploadInFlight(operation)
        val remote = executeComprobantePost {
            api.uploadComprobante(
                bytes = image.bytes,
                fileName = image.fileName,
                mediaType = image.mediaType,
                bearerToken = bearerToken,
            )
        }
        try {
            comprobanteWriter.upsertDetail(
                dto = remote,
                preferredLocalId = payload.localId,
                clearLocalImageUri = true,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw ComprobantePostPersistFailure(error)
        }
        // The API response above is the confirmation that the enterprise system
        // owns the image. Never delete before both the upload and local DB update
        // have completed, and never turn a cleanup failure into a duplicate POST.
        // Cleanup is best effort. A local file deletion error must never trigger
        // another financial POST after the server already confirmed creation.
        runCatching { imageReader.deleteAfterConfirmedUpload(payload.imagenUri) }
    }

    private companion object {
        const val SYNC_BATCH_SIZE = 25
    }
}
