package pe.edu.hzg.tesoreria.admin.data

import pe.edu.hzg.tesoreria.admin.domain.AdminRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import pe.edu.hzg.tesoreria.admin.model.ConfiguracionInstitucional
import pe.edu.hzg.tesoreria.admin.model.NuevoUsuario
import pe.edu.hzg.tesoreria.admin.model.RegistroAuditoria
import pe.edu.hzg.tesoreria.admin.model.RolUsuario
import pe.edu.hzg.tesoreria.admin.model.UsuarioAdministrado
import pe.edu.hzg.tesoreria.data.remote.dto.ApiErrorDto



class OnlineAdminRepository(
    private val api: AdminApi,
) : AdminRepository {
    override suspend fun listarUsuarios() = adminCall {
        api.usuarios().map(UsuarioAdminDto::toDomain)
    }

    override suspend fun crearUsuario(usuario: NuevoUsuario) = adminCall {
        api.crearUsuario(
            CrearUsuarioRequest(
                username = usuario.username.trim(),
                nombreCompleto = usuario.nombreCompleto.trim(),
                password = usuario.password,
                rol = usuario.rol.apiValue,
            ),
        )
        Unit
    }

    override suspend fun alternarEstadoUsuario(id: Int) = adminCall {
        api.alternarEstadoUsuario(id)
    }

    override suspend fun editarUsuario(id: Int, nombreCompleto: String) = adminCall {
        api.editarUsuario(id, nombreCompleto.trim())
    }

    override suspend fun cambiarRolUsuario(id: Int, rol: RolUsuario) = adminCall {
        api.cambiarRolUsuario(id, rol.apiValue)
    }

    override suspend fun eliminarUsuario(id: Int) = adminCall {
        api.eliminarUsuario(id)
    }

    override suspend fun obtenerConfiguracion() = adminCall {
        api.configuracion().toDomain()
    }

    override suspend fun actualizarConfiguracion(configuracion: ConfiguracionInstitucional) = adminCall {
        api.actualizarConfiguracion(ActualizarConfiguracionRequest.from(configuracion))
    }

    override suspend fun listarAuditoria(top: Int) = adminCall {
        api.auditoria(top).map(RegistroAuditoriaDto::toDomain)
    }

    private suspend fun <T> adminCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (cause: Throwable) {
        Result.failure(AdminOperationException(cause.toFriendlyMessage(), cause))
    }

    private suspend fun Throwable.toFriendlyMessage(): String = when (this) {
        is ClientRequestException -> {
            val apiError = try {
                response.body<ApiErrorDto>()
            } catch (_: Throwable) {
                null
            }
            val validation = apiError?.errores
                .orEmpty()
                .mapNotNull { it.error?.takeIf(String::isNotBlank) }
                .joinToString(" · ")
            when {
                validation.isNotBlank() -> validation
                !apiError?.mensaje.isNullOrBlank() -> requireNotNull(apiError?.mensaje)
                response.status.value == 401 -> "La sesión venció. Vuelve a iniciar sesión."
                response.status.value == 403 -> "Esta acción requiere el rol Administrador."
                response.status.value == 404 -> "El registro ya no existe o fue eliminado."
                else -> "La API rechazó la operación (${response.status.value})."
            }
        }
        is ServerResponseException -> "El servidor no pudo completar la operación. Inténtalo nuevamente."
        is HttpRequestTimeoutException -> "La solicitud tardó demasiado. Revisa tu conexión."
        is IOException -> "No se pudo conectar con el sistema empresarial."
        else -> message?.takeIf(String::isNotBlank) ?: "Ocurrió un error inesperado."
    }
}

class AdminOperationException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
