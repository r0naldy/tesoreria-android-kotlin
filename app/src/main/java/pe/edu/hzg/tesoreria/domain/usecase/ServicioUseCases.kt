package pe.edu.hzg.tesoreria.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.Servicio
import pe.edu.hzg.tesoreria.domain.repository.ServicioRepository

class ObserveServiciosUseCase(private val repository: ServicioRepository) {
    operator fun invoke(soloActivos: Boolean = false): Flow<List<Servicio>> =
        repository.observeServicios(soloActivos)
}

class RefreshServiciosUseCase(private val repository: ServicioRepository) {
    suspend operator fun invoke(soloActivos: Boolean = false) = repository.refresh(soloActivos)
}

class CreateServicioUseCase(private val repository: ServicioRepository) {
    suspend operator fun invoke(nombre: String, precio: Double) =
        repository.create(nombre.trim(), precio)
}

class UpdateServicioUseCase(private val repository: ServicioRepository) {
    suspend operator fun invoke(servicio: Servicio) = repository.update(servicio)
}

class DeleteServicioUseCase(private val repository: ServicioRepository) {
    suspend operator fun invoke(id: Int) = repository.delete(id)
}
