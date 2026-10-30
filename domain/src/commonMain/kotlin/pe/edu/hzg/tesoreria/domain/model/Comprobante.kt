package pe.edu.hzg.tesoreria.domain.model

data class Comprobante(
    val localId: String,
    val id: Int? = null,
    val proveedor: String? = null,
    val ruc: String? = null,
    val tipoDocumento: String? = null,
    val numeroComprobante: String? = null,
    val fechaEmision: String? = null,
    val horaEmision: String? = null,
    val moneda: String = "PEN",
    val subtotal: Double? = null,
    val igv: Double? = null,
    val total: Double,
    val categoria: String? = null,
    val concepto: String? = null,
    val metodoPago: String? = null,
    val confianza: Int = 0,
    val esDuplicadoProbable: Boolean = false,
    val observaciones: String? = null,
    val imagenUri: String? = null,
    val imagenUrl: String? = null,
    val fechaRegistro: String? = null,
    val usuario: String = "",
    val estado: String = "Pendiente",
    val productos: List<ComprobanteProducto> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val syncError: String? = null,
)

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    ERROR,
}
