package pe.edu.hzg.tesoreria.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.Comprobante
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft

interface ComprobanteRepository {
    fun observeComprobantes(): Flow<List<Comprobante>>
    suspend fun refresh(): Result<Unit>
    suspend fun findById(id: Int): Result<Comprobante>
    suspend fun queueForUpload(draft: ComprobanteDraft): Result<String>
}
