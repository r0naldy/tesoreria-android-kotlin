package pe.edu.hzg.tesoreria.domain.model

data class ComprobanteDraft(
    val imagenUri: String,
    val proveedor: String? = null,
    val ruc: String? = null,
    val tipoDocumento: String? = null,
    val numeroComprobante: String? = null,
    val fechaEmision: String? = null,
    val horaEmision: String? = null,
    val moneda: String = "PEN",
    val subtotal: Double? = null,
    val igv: Double? = null,
    val total: Double? = null,
    val categoria: String? = null,
    val concepto: String? = null,
    val metodoPago: String? = null,
    val observaciones: String? = null,
    val productos: List<ComprobanteProducto> = emptyList(),
)
