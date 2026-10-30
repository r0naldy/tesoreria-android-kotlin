package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class SsoLoginRequestDto(
    val token: String,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val expiraEnMinutos: Int,
)

@Serializable
data class PublicConfigurationDto(
    val nombreInstitucion: String = "",
    val ciudad: String = "",
    val logoBase64: String? = null,
)
