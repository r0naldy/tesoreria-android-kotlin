package pe.edu.hzg.tesoreria.data.remote

import pe.edu.hzg.tesoreria.data.remote.dto.ProgramaDto
import pe.edu.hzg.tesoreria.data.remote.dto.ServicioDto

/** The existing REST catalogue contract, injectable for offline regression tests. */
interface CatalogApi {
    suspend fun getProgramas(soloActivos: Boolean, bearerToken: String? = null): List<ProgramaDto>
    suspend fun createPrograma(nombre: String, bearerToken: String? = null): Int
    suspend fun updatePrograma(id: Int, nombre: String, activo: Boolean, bearerToken: String? = null)
    suspend fun deletePrograma(id: Int, bearerToken: String? = null)
    suspend fun getServicios(soloActivos: Boolean, bearerToken: String? = null): List<ServicioDto>
    suspend fun createServicio(nombre: String, precio: Double, bearerToken: String? = null): Int
    suspend fun updateServicio(id: Int, nombre: String, precio: Double, activo: Boolean, bearerToken: String? = null)
    suspend fun deleteServicio(id: Int, bearerToken: String? = null)
}
