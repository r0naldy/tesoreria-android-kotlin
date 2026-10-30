package pe.edu.hzg.tesoreria.data.remote

import io.ktor.client.plugins.*
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.json.*
import pe.edu.hzg.tesoreria.domain.repository.ErrorMessages

class DefaultErrorMessages : ErrorMessages {
override suspend fun academic(error: Throwable): String = with(error) {
    if (this is ClientRequestException && response.status.value == 403) {
        return@with "Tu usuario no tiene permiso para realizar esta acción."
    }
    if (this is ResponseException) {
        val body = runCatching { response.bodyAsText() }.getOrNull()
        val serverMessage = body?.let { raw ->
            runCatching {
                Json.parseToJsonElement(raw).jsonObject["mensaje"]?.jsonPrimitive?.content
            }.getOrNull()
        }
        if (!serverMessage.isNullOrBlank()) return@with serverMessage
        return@with when (response.status.value) {
            401 -> "La sesión venció. Vuelve a iniciar sesión."
            404 -> "No se encontró la información solicitada."
            in 500..599 -> "El servidor no pudo completar la operación."
            else -> "No se pudo completar la operación (${response.status.value})."
        }
    }
    return@with message?.takeIf { it.isNotBlank() }
        ?: "No se pudo conectar con el sistema empresarial."
}

override fun treasury(error: Throwable): String = with(error) { when (this) {
    is ClientRequestException -> when (response.status.value) {
        400 -> "El servidor rechazo los datos. Revise los campos e intentelo de nuevo."
        401 -> "La sesion vencio. Ingrese nuevamente."
        403 -> "Su usuario no tiene permiso para esta operacion."
        404 -> "El registro solicitado ya no existe."
        409 -> "La operacion entra en conflicto con un registro existente."
        else -> "Solicitud rechazada (${response.status.value})."
    }
    is ServerResponseException -> "El servidor no pudo completar la operacion (${response.status.value})."
    is HttpRequestTimeoutException -> "La consulta tardo demasiado. Revise su conexion."
    is IOException -> "No se pudo conectar con el sistema empresarial."
    else -> message?.takeIf { it.isNotBlank() } ?: "Ocurrio un error inesperado."
}

}
}
