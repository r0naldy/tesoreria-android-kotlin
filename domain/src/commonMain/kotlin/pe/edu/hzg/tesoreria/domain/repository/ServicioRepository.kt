package pe.edu.hzg.tesoreria.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.Servicio

interface ServicioRepository {
    fun observeServicios(soloActivos: Boolean = false): Flow<List<Servicio>>
    suspend fun refresh(soloActivos: Boolean = false): Result<Unit>
    suspend fun create(nombre: String, precio: Double): Result<Int>
    suspend fun update(servicio: Servicio): Result<Unit>
    suspend fun delete(id: Int): Result<Unit>
    /** Never deletes a server record; removes only this user's local proposal. */
    suspend fun discardPending(id: Int): Result<Unit>
}
