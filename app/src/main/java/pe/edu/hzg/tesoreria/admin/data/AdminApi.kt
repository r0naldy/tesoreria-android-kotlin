package pe.edu.hzg.tesoreria.admin.data

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pe.edu.hzg.tesoreria.data.remote.ApiClient

class AdminApi(
    apiClient: ApiClient,
    baseUrl: String,
) {
    private val client = apiClient.http
    private val apiRoot = "${baseUrl.trimEnd('/')}/api"

    suspend fun usuarios(): List<UsuarioAdminDto> =
        client.get("$apiRoot/usuarios").body()

    suspend fun crearUsuario(request: CrearUsuarioRequest): Int =
        client.post("$apiRoot/usuarios") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<IdCreadoResponse>().id

    /**
     * Este endpoint alterna el estado; deliberadamente no hay reintento automático ni outbox.
     * Repetir la petición podría devolver al usuario a su estado original.
     */
    suspend fun alternarEstadoUsuario(id: Int) {
        client.put("$apiRoot/usuarios/$id/estado")
    }

    suspend fun cambiarRolUsuario(id: Int, rol: String) {
        client.put("$apiRoot/usuarios/$id/rol") {
            contentType(ContentType.Application.Json)
            setBody(CambiarRolRequest(rol))
        }
    }

    suspend fun eliminarUsuario(id: Int) {
        client.delete("$apiRoot/usuarios/$id")
    }

    suspend fun editarUsuario(id: Int, nombreCompleto: String) {
        client.put("$apiRoot/usuarios/$id") {
            contentType(ContentType.Application.Json)
            setBody(EditarUsuarioRequest(id, nombreCompleto))
        }
    }

    suspend fun configuracion(): ConfiguracionAdminDto =
        client.get("$apiRoot/configuracion").body()

    suspend fun actualizarConfiguracion(request: ActualizarConfiguracionRequest) {
        client.put("$apiRoot/configuracion") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun auditoria(top: Int): List<RegistroAuditoriaDto> =
        client.get("$apiRoot/auditoria") {
            url { parameters.append("top", top.coerceIn(1, MAX_AUDIT_ROWS).toString()) }
        }.body()

    private companion object {
        const val MAX_AUDIT_ROWS = 500
    }
}
