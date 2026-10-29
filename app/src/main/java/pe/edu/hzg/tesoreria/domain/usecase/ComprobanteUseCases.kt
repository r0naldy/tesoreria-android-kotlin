package pe.edu.hzg.tesoreria.domain.usecase

import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft
import pe.edu.hzg.tesoreria.domain.repository.ComprobanteRepository

class ObserveComprobantesUseCase(private val repository: ComprobanteRepository) {
    operator fun invoke() = repository.observeComprobantes()
}

class RefreshComprobantesUseCase(private val repository: ComprobanteRepository) {
    suspend operator fun invoke() = repository.refresh()
}

class FindComprobanteUseCase(private val repository: ComprobanteRepository) {
    suspend operator fun invoke(id: Int) = repository.findById(id)
}

class QueueComprobanteUseCase(private val repository: ComprobanteRepository) {
    suspend operator fun invoke(draft: ComprobanteDraft) = repository.queueForUpload(draft)
}
