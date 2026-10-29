package pe.edu.hzg.tesoreria.data.repository

import kotlin.math.roundToLong
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteDto
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteListItemDto
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft

class ComprobanteLocalWriter(private val database: AppDatabase) {
    private val queries get() = database.appDatabaseQueries

    /** Called inside PendingOperationStore's transaction, never as a separate commit. */
    internal fun insertDraft(draft: ComprobanteDraft, localId: String, owner: String, now: Long) {
        queries.upsertComprobante(
            local_id = localId,
            remote_id = null,
            proveedor = draft.proveedor,
            ruc = draft.ruc,
            tipo_documento = draft.tipoDocumento,
            numero_comprobante = draft.numeroComprobante,
            fecha_emision = draft.fechaEmision,
            hora_emision = draft.horaEmision,
            moneda = draft.moneda,
            subtotal_centimos = draft.subtotal.toCentsOrNull(),
            igv_centimos = draft.igv.toCentsOrNull(),
            total_centimos = (draft.total ?: 0.0).toCents(),
            categoria = draft.categoria,
            concepto = draft.concepto,
            metodo_pago = draft.metodoPago,
            confianza = 0L,
            es_duplicado_probable = 0L,
            observaciones = draft.observaciones,
            imagen_uri = draft.imagenUri,
            imagen_url = null,
            fecha_registro = utcIsoNow(now),
            usuario = owner,
            estado = "Pendiente",
            sync_status = "PENDING",
            sync_error = null,
            created_at = now,
            updated_at = now,
        )
        draft.productos.forEachIndexed { index, item ->
            queries.insertProducto(
                comprobante_local_id = localId,
                orden = (index + 1).toLong(),
                descripcion = item.descripcion,
                cantidad = item.cantidad,
                precio_unitario_centimos = item.precioUnitario.toCentsOrNull(),
                importe_centimos = item.importe.toCentsOrNull(),
            )
        }
    }

    fun upsertDetail(
        dto: ComprobanteDto,
        preferredLocalId: String? = null,
        localImageUri: String? = null,
        clearLocalImageUri: Boolean = false,
    ): String {
        val previous = queries.selectComprobanteByRemoteId(dto.id.toLong()).executeAsOneOrNull()
        val localId = previous?.local_id ?: preferredLocalId ?: "remote-${dto.id}"
        val now = System.currentTimeMillis()
        database.transaction {
            queries.upsertComprobante(
                local_id = localId,
                remote_id = dto.id.toLong(),
                proveedor = dto.proveedor,
                ruc = dto.ruc,
                tipo_documento = dto.tipoDocumento,
                numero_comprobante = dto.numeroComprobante,
                fecha_emision = dto.fechaEmision,
                hora_emision = dto.horaEmision,
                moneda = dto.moneda,
                subtotal_centimos = dto.subtotal.toCentsOrNull(),
                igv_centimos = dto.igv.toCentsOrNull(),
                total_centimos = dto.total.toCents(),
                categoria = dto.categoria,
                concepto = dto.concepto,
                metodo_pago = dto.metodoPago,
                confianza = dto.confianza.toLong(),
                es_duplicado_probable = if (dto.esDuplicadoProbable) 1L else 0L,
                observaciones = dto.observaciones,
                imagen_uri = if (clearLocalImageUri) null else localImageUri ?: previous?.imagen_uri,
                imagen_url = dto.imagenUrl,
                fecha_registro = dto.fechaRegistro,
                usuario = dto.usuario,
                estado = dto.estado,
                sync_status = "SYNCED",
                sync_error = null,
                created_at = previous?.created_at ?: now,
                updated_at = now,
            )
            queries.deleteProductosForComprobante(localId)
            dto.productos.forEachIndexed { index, producto ->
                queries.insertProducto(
                    comprobante_local_id = localId,
                    orden = (index + 1).toLong(),
                    descripcion = producto.descripcion,
                    cantidad = producto.cantidad,
                    precio_unitario_centimos = producto.precioUnitario.toCentsOrNull(),
                    importe_centimos = producto.importe.toCentsOrNull(),
                )
            }
        }
        return localId
    }

    fun upsertSummary(dto: ComprobanteListItemDto) {
        val previous = queries.selectComprobanteByRemoteId(dto.id.toLong()).executeAsOneOrNull()
        val localId = previous?.local_id ?: "remote-${dto.id}"
        val now = System.currentTimeMillis()
        queries.upsertComprobante(
            local_id = localId,
            remote_id = dto.id.toLong(),
            proveedor = dto.proveedor,
            ruc = dto.ruc,
            tipo_documento = dto.tipoDocumento,
            numero_comprobante = dto.numeroComprobante,
            fecha_emision = dto.fechaEmision,
            hora_emision = previous?.hora_emision,
            moneda = dto.moneda,
            subtotal_centimos = previous?.subtotal_centimos,
            igv_centimos = previous?.igv_centimos,
            total_centimos = dto.total.toCents(),
            categoria = dto.categoria,
            concepto = dto.concepto,
            metodo_pago = previous?.metodo_pago,
            confianza = dto.confianza.toLong(),
            es_duplicado_probable = if (dto.esDuplicadoProbable) 1L else 0L,
            observaciones = previous?.observaciones,
            imagen_uri = previous?.imagen_uri,
            imagen_url = previous?.imagen_url,
            fecha_registro = dto.fechaRegistro,
            usuario = dto.usuario,
            estado = dto.estado,
            sync_status = "SYNCED",
            sync_error = null,
            created_at = previous?.created_at ?: now,
            updated_at = now,
        )
    }

    private fun Double.toCents(): Long = (this * 100.0).roundToLong()
    private fun Double?.toCentsOrNull(): Long? = this?.toCents()
}
