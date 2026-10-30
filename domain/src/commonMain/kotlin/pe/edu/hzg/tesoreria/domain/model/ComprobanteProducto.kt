package pe.edu.hzg.tesoreria.domain.model

data class ComprobanteProducto(
    val orden: Int,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val precioUnitario: Double? = null,
    val importe: Double? = null,
)
