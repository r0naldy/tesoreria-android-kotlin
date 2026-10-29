package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgramaDto(
    val id: Int,
    val nombre: String,
    val estado: String,
    val fechaCreacion: String,
)

@Serializable
data class CreateProgramaRequestDto(val nombre: String)

@Serializable
data class UpdateProgramaRequestDto(
    val id: Int,
    val nombre: String,
    val activo: Boolean,
)

@Serializable
data class CreatedIdDto(val id: Int)
