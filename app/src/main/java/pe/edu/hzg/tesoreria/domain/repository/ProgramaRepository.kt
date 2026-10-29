package pe.edu.hzg.tesoreria.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.Programa

interface ProgramaRepository {
    fun observeProgramas(soloActivos: Boolean = false): Flow<List<Programa>>
    suspend fun refresh(soloActivos: Boolean = false): Result<Unit>
    suspend fun create(nombre: String): Result<Int>
    suspend fun update(programa: Programa): Result<Unit>
    suspend fun delete(id: Int): Result<Unit>
    /** Discards only this user's local proposal after fetching a fresh server snapshot. */
    suspend fun discardPending(id: Int): Result<Unit>
}
