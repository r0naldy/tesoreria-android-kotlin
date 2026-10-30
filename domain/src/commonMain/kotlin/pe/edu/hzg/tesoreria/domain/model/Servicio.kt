package pe.edu.hzg.tesoreria.domain.model

data class Servicio(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val estado: String,
    val syncPending: Boolean = false,
    val syncError: String? = null,
) {
    val activo: Boolean get() = estado.equals("Activo", ignoreCase = true)
}
