package pe.edu.hzg.tesoreria.domain.validation

import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft

data class ValidationIssue(
    val field: String,
    val message: String,
)

/**
 * Pure domain validation for a receipt before it enters the offline outbox.
 *
 * OCR fields are deliberately optional because the server fills them after the
 * image is uploaded. Values supplied manually, however, must be coherent.
 */
object ComprobanteDraftValidator {
    fun validate(draft: ComprobanteDraft): List<ValidationIssue> = buildList {
        if (draft.imagenUri.isBlank()) {
            add(ValidationIssue("imagenUri", "La imagen del comprobante es obligatoria."))
        }

        draft.ruc?.takeIf(String::isNotBlank)?.let { ruc ->
            if (ruc.length != 11 || ruc.any { !it.isDigit() }) {
                add(ValidationIssue("ruc", "El RUC debe contener exactamente 11 dígitos."))
            }
        }

        if (draft.moneda.length != 3 || draft.moneda.any { !it.isLetter() }) {
            add(ValidationIssue("moneda", "La moneda debe usar un código ISO de 3 letras."))
        }

        validateMoney("subtotal", draft.subtotal, this)
        validateMoney("igv", draft.igv, this)
        validateMoney("total", draft.total, this)

        draft.productos.forEachIndexed { index, product ->
            val position = index + 1
            if (product.descripcion.isNullOrBlank()) {
                add(
                    ValidationIssue(
                        "productos[$index].descripcion",
                        "El concepto $position debe tener una descripción.",
                    ),
                )
            }
            product.cantidad?.let { quantity ->
                if (!quantity.isFinite() || quantity <= 0.0) {
                    add(
                        ValidationIssue(
                            "productos[$index].cantidad",
                            "La cantidad del concepto $position debe ser mayor que cero.",
                        ),
                    )
                }
            }
            validateMoney("productos[$index].precioUnitario", product.precioUnitario, this)
            validateMoney("productos[$index].importe", product.importe, this)
        }
    }

    fun requireValid(draft: ComprobanteDraft) {
        val issues = validate(draft)
        require(issues.isEmpty()) {
            issues.joinToString(separator = " ") { "${it.field}: ${it.message}" }
        }
    }

    private fun validateMoney(
        field: String,
        value: Double?,
        issues: MutableList<ValidationIssue>,
    ) {
        if (value != null && (!value.isFinite() || value < 0.0)) {
            issues += ValidationIssue(field, "El importe debe ser un número mayor o igual a cero.")
        }
    }
}
