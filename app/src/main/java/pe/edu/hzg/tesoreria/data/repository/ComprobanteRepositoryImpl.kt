package pe.edu.hzg.tesoreria.data.repository

import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.hzg.tesoreria.data.local.asListFlow
import pe.edu.hzg.tesoreria.data.coroutines.runSuspendCatching
import pe.edu.hzg.tesoreria.data.remote.TesoreriaApi
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteDto
import pe.edu.hzg.tesoreria.data.sync.PendingComprobantePayload
import pe.edu.hzg.tesoreria.data.sync.PendingEntityType
import pe.edu.hzg.tesoreria.data.sync.PendingOperationStore
import pe.edu.hzg.tesoreria.data.sync.PendingOperationType
import pe.edu.hzg.tesoreria.data.sync.SyncScheduler
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.Comprobante
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft
import pe.edu.hzg.tesoreria.domain.model.ComprobanteProducto
import pe.edu.hzg.tesoreria.domain.model.SyncStatus
import pe.edu.hzg.tesoreria.domain.repository.ComprobanteRepository
import pe.edu.hzg.tesoreria.domain.validation.ComprobanteDraftValidator

class ComprobanteRepositoryImpl(
    private val database: AppDatabase,
    private val api: TesoreriaApi,
    private val writer: ComprobanteLocalWriter,
    private val pendingStore: PendingOperationStore,
    private val scheduler: SyncScheduler,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ComprobanteRepository {
    private val queries get() = database.appDatabaseQueries

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeComprobantes(): Flow<List<Comprobante>> = pendingStore.observeOwnerIdentity()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(emptyList())
            else queries.selectComprobanteSummariesForOwner(owner, ::mapSummary).asListFlow(ioDispatcher)
        }

    override suspend fun refresh(): Result<Unit> = runSuspendCatching {
        withContext(ioDispatcher) {
            val remote = api.getComprobantes()
            database.transaction { remote.forEach(writer::upsertSummary) }
        }
    }

    override suspend fun findById(id: Int): Result<Comprobante> = runSuspendCatching {
        withContext(ioDispatcher) {
            val dto = api.getComprobante(id)
            val localId = writer.upsertDetail(dto)
            dto.toDomain(localId)
        }
    }

    override suspend fun queueForUpload(draft: ComprobanteDraft): Result<String> = runSuspendCatching {
        ComprobanteDraftValidator.requireValid(draft)
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        pendingStore.enqueue(
            PendingEntityType.COMPROBANTE,
            localId,
            PendingOperationType.UPLOAD,
            json.encodeToString(PendingComprobantePayload(localId, draft.imagenUri)),
        ) { owner -> writer.insertDraft(draft, localId, owner, now) }
        // Persistence already succeeded. A scheduler failure must not make the
        // caller create a duplicate; manual sync/login can retry the durable queue.
        runSuspendCatching { scheduler.schedule() }
        localId
    }

    private fun mapSummary(
        localId: String,
        remoteId: Long?,
        proveedor: String?,
        ruc: String?,
        tipoDocumento: String?,
        numeroComprobante: String?,
        fechaEmision: String?,
        moneda: String,
        totalCentimos: Long,
        categoria: String?,
        concepto: String?,
        confianza: Long,
        esDuplicadoProbable: Long,
        imagenUri: String?,
        imagenUrl: String?,
        fechaRegistro: String?,
        usuario: String,
        estado: String,
        syncStatus: String,
        syncError: String?,
    ) = Comprobante(
        localId = localId,
        id = remoteId?.toInt(),
        proveedor = proveedor,
        ruc = ruc,
        tipoDocumento = tipoDocumento,
        numeroComprobante = numeroComprobante,
        fechaEmision = fechaEmision,
        moneda = moneda,
        total = totalCentimos / 100.0,
        categoria = categoria,
        concepto = concepto,
        confianza = confianza.toInt(),
        esDuplicadoProbable = esDuplicadoProbable != 0L,
        imagenUri = imagenUri,
        imagenUrl = imagenUrl,
        fechaRegistro = fechaRegistro,
        usuario = usuario,
        estado = estado,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.ERROR),
        syncError = syncError,
    )

    private fun ComprobanteDto.toDomain(localId: String) = Comprobante(
        localId = localId,
        id = id,
        proveedor = proveedor,
        ruc = ruc,
        tipoDocumento = tipoDocumento,
        numeroComprobante = numeroComprobante,
        fechaEmision = fechaEmision,
        horaEmision = horaEmision,
        moneda = moneda,
        subtotal = subtotal,
        igv = igv,
        total = total,
        categoria = categoria,
        concepto = concepto,
        metodoPago = metodoPago,
        confianza = confianza,
        esDuplicadoProbable = esDuplicadoProbable,
        observaciones = observaciones,
        imagenUrl = imagenUrl,
        fechaRegistro = fechaRegistro,
        usuario = usuario,
        estado = estado,
        productos = productos.map {
            ComprobanteProducto(it.orden, it.descripcion, it.cantidad, it.precioUnitario, it.importe)
        },
        syncStatus = SyncStatus.SYNCED,
    )
}
