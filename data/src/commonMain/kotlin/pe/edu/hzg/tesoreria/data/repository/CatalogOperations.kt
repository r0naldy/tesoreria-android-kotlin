package pe.edu.hzg.tesoreria.data.repository

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import pe.edu.hzg.tesoreria.data.remote.dto.ApiErrorDto
import pe.edu.hzg.tesoreria.data.sync.ComprobanteUploadRetryPolicy

internal fun validatedCatalogName(value: String): String = value.trim().also {
    require(it.isNotBlank()) { "El nombre es obligatorio." }
    require(it.length <= 200) { "El nombre no puede superar 200 caracteres." }
}

internal fun validatedCatalogPrice(value: Double): Double {
    require(value.isFinite() && value >= 0 && value <= 99_999_999.99) {
        "El precio debe estar entre S/ 0.00 y S/ 99,999,999.99."
    }
    val cents = value.toBigDecimal().stripTrailingZeros()
    require(cents.scale() <= 2) { "El precio admite como máximo 2 decimales." }
    return value
}

internal suspend fun <T> catalogCall(isCreate: Boolean = false, block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(CatalogOperationException(catalogErrorMessage(error, isCreate), error))
}

internal suspend fun catalogErrorMessage(error: Throwable, isCreate: Boolean = false): String {
    if (error is ClientRequestException) {
        val body = runCatching { error.response.body<ApiErrorDto>() }.getOrNull()
        val validation = body?.errores.orEmpty().mapNotNull { it.error?.takeIf(String::isNotBlank) }.joinToString(" · ")
        return when {
            error.response.status.value == 401 -> "La sesión venció. Vuelve a iniciar sesión."
            error.response.status.value == 403 -> "Solo un Administrador puede modificar este catálogo."
            validation.isNotEmpty() -> validation
            !body?.mensaje.isNullOrBlank() -> body?.mensaje.orEmpty()
            error.response.status.value == 404 -> "El registro ya no existe. Actualiza el listado."
            else -> "El servidor rechazó la operación (${error.response.status.value})."
        }
    }
    if (error is IllegalArgumentException || error is IllegalStateException || error is CatalogOperationException) {
        return error.message ?: "Revisa los datos ingresados."
    }
    if (ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(error)) {
        return "Sin conexión. Puedes consultar los datos guardados, pero crear, editar o eliminar requiere conexión. No se aplicó el cambio."
    }
    return if (isCreate) {
        "No se pudo confirmar la creación. Actualiza el listado y comprueba si ya existe antes de volver a guardar; no se reenviará automáticamente."
    } else if (error is ResponseException) {
        "El servidor no pudo confirmar el cambio. Actualiza el listado antes de intentarlo nuevamente."
    } else {
        "No se pudo confirmar el cambio. Revisa tu conexión y actualiza el listado."
    }
}

class CatalogOperationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
