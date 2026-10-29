package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServicioDto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val estado: String,
)

@Serializable
data class CreateServicioRequestDto(
    val nombre: String,
    val precio: Double,
)

@Serializable
data class UpdateServicioRequestDto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val activo: Boolean,
)
