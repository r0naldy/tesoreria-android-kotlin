package pe.edu.hzg.tesoreria.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft
import pe.edu.hzg.tesoreria.domain.model.ComprobanteProducto

class ComprobanteDraftValidatorTest {
    @Test
    fun `minimal camera draft is valid because OCR fields are server supplied`() {
        val issues = ComprobanteDraftValidator.validate(
            ComprobanteDraft(imagenUri = "content://tesoreria/pending-receipts/receipt.jpg"),
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `blank image invalid RUC and negative total report their fields`() {
        val issues = ComprobanteDraftValidator.validate(
            ComprobanteDraft(
                imagenUri = " ",
                ruc = "20123A",
                total = -0.01,
            ),
        )

        assertEquals(setOf("imagenUri", "ruc", "total"), issues.map { it.field }.toSet())
    }

    @Test
    fun `each enumerated concept requires description and coherent numeric values`() {
        val issues = ComprobanteDraftValidator.validate(
            ComprobanteDraft(
                imagenUri = "content://tesoreria/receipt.jpg",
                productos = listOf(
                    ComprobanteProducto(
                        orden = 1,
                        descripcion = "",
                        cantidad = 0.0,
                        precioUnitario = Double.NaN,
                        importe = -4.0,
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(
                "productos[0].descripcion",
                "productos[0].cantidad",
                "productos[0].precioUnitario",
                "productos[0].importe",
            ),
            issues.map { it.field }.toSet(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requireValid prevents an invalid draft from entering the outbox`() {
        ComprobanteDraftValidator.requireValid(ComprobanteDraft(imagenUri = ""))
    }
}
