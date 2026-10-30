package pe.edu.hzg.tesoreria.domain.usecase

import pe.edu.hzg.tesoreria.domain.repository.SyncRepository

class ObservePendingCountUseCase(private val repository: SyncRepository) {
    operator fun invoke() = repository.observePendingCount()
}

class SynchronizePendingUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.synchronize()
}
