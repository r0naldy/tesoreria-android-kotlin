package pe.edu.hzg.tesoreria.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.SyncSummary

interface SyncRepository {
    fun observePendingCount(): Flow<Long>
    suspend fun synchronize(): Result<SyncSummary>
}
