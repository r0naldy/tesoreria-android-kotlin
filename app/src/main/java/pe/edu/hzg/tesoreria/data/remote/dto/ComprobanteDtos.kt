package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ComprobanteDto(
    val id: Int,
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
    val imagenRuta: String? = null,
    val imagenUrl: String? = null,
    val fechaRegistro: String,
    val usuario: String = "",
    val estado: String = "",
    val cantidadConceptos: Int = 0,
    val productos: List<ComprobanteProductoDto> = emptyList(),
)

@Serializable
data class ComprobanteListItemDto(
    val id: Int,
    val proveedor: String? = null,
    val ruc: String? = null,
    val tipoDocumento: String? = null,
    val numeroComprobante: String? = null,
    val fechaEmision: String? = null,
    val fechaRegistro: String,
    val total: Double,
    val moneda: String = "PEN",
    val categoria: String? = null,
    val concepto: String? = null,
    val confianza: Int = 0,
    val esDuplicadoProbable: Boolean = false,
    val usuario: String = "",
    val estado: String = "",
)

@Serializable
data class ComprobanteProductoDto(
    val orden: Int = 0,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val precioUnitario: Double? = null,
    val importe: Double? = null,
)
