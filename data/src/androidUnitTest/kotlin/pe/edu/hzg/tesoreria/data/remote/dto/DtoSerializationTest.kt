package pe.edu.hzg.tesoreria.data.remote.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DtoSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `login response tolerates new backend fields without losing JWT metadata`() {
        val payload = """
            {
              "token": "header.payload.signature",
              "username": "admin",
              "nombreCompleto": "Administrador del Sistema",
              "rol": "Administrador",
              "expiraEnMinutos": 60,
              "campoFuturo": "compatible"
            }
        """.trimIndent()

        val response = json.decodeFromString<AuthResponseDto>(payload)

        assertEquals("header.payload.signature", response.token)
        assertEquals("Administrador del Sistema", response.nombreCompleto)
        assertEquals(60, response.expiraEnMinutos)
    }

    @Test
    fun `receipt detail preserves enumerated concepts and safe defaults`() {
        val payload = """
            {
              "id": 42,
              "proveedor": "COMERCIAL PRUEBA SAC",
              "ruc": "20123456789",
              "total": 15.0,
              "fechaRegistro": "2026-10-27T20:00:00Z",
              "cantidadConceptos": 2,
              "productos": [
                { "orden": 1, "descripcion": "Papel A4", "cantidad": 1.0, "importe": 10.0 },
                { "orden": 2, "descripcion": "Lapiceros", "cantidad": 2.0, "importe": 5.0 }
              ]
            }
        """.trimIndent()

        val receipt = json.decodeFromString<ComprobanteDto>(payload)

        assertEquals(2, receipt.cantidadConceptos)
        assertEquals(listOf(1, 2), receipt.productos.map { it.orden })
        assertEquals("Papel A4", receipt.productos.first().descripcion)
        assertEquals("PEN", receipt.moneda)
        assertFalse(receipt.esDuplicadoProbable)
    }

    @Test
    fun `login request serializes exact backend field names`() {
        val encoded = json.encodeToString(LoginRequestDto("admin", "secret"))

        assertEquals("{\"username\":\"admin\",\"password\":\"secret\"}", encoded)
    }
}
