package pe.edu.hzg.tesoreria.domain.model

data class Programa(
    val id: Int,
    val nombre: String,
    val estado: String,
    val fechaCreacion: String,
    val syncPending: Boolean = false,
    val syncError: String? = null,
) {
    val activo: Boolean get() = estado.equals("Activo", ignoreCase = true)
}
