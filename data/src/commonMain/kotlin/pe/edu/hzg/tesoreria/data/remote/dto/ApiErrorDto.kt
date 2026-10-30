package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val mensaje: String? = null,
    val errores: List<ValidationErrorDto> = emptyList(),
)

@Serializable
data class ValidationErrorDto(
    val campo: String? = null,
    val error: String? = null,
)
