package pe.edu.hzg.tesoreria.admin.domain

import pe.edu.hzg.tesoreria.admin.model.*

interface AdminRepository {
    suspend fun listarUsuarios(): Result<List<UsuarioAdministrado>>
    suspend fun crearUsuario(usuario: NuevoUsuario): Result<Unit>
    suspend fun editarUsuario(id: Int, nombreCompleto: String): Result<Unit>
    suspend fun alternarEstadoUsuario(id: Int): Result<Unit>
    suspend fun cambiarRolUsuario(id: Int, rol: RolUsuario): Result<Unit>
    suspend fun eliminarUsuario(id: Int): Result<Unit>
    suspend fun obtenerConfiguracion(): Result<ConfiguracionInstitucional>
    suspend fun actualizarConfiguracion(configuracion: ConfiguracionInstitucional): Result<Unit>
    suspend fun listarAuditoria(top: Int = 200): Result<List<RegistroAuditoria>>
}
