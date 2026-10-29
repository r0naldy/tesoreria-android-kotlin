package pe.edu.hzg.tesoreria.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class PendingProgramaPayload(
    val id: Int,
    val nombre: String,
    val activo: Boolean,
)

@Serializable
data class PendingServicioPayload(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val activo: Boolean,
)

@Serializable
data class PendingDeletePayload(val id: Int)

@Serializable
data class PendingComprobantePayload(
    val localId: String,
    val imagenUri: String,
)
