package pe.edu.hzg.tesoreria.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.repository.ProgramaRepository

class ObserveProgramasUseCase(private val repository: ProgramaRepository) {
    operator fun invoke(soloActivos: Boolean = false): Flow<List<Programa>> =
        repository.observeProgramas(soloActivos)
}

class RefreshProgramasUseCase(private val repository: ProgramaRepository) {
    suspend operator fun invoke(soloActivos: Boolean = false) = repository.refresh(soloActivos)
}

class CreateProgramaUseCase(private val repository: ProgramaRepository) {
    suspend operator fun invoke(nombre: String) = repository.create(nombre.trim())
}

class UpdateProgramaUseCase(private val repository: ProgramaRepository) {
    suspend operator fun invoke(programa: Programa) = repository.update(programa)
}

class DeleteProgramaUseCase(private val repository: ProgramaRepository) {
    suspend operator fun invoke(id: Int) = repository.delete(id)
}
