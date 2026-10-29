package pe.edu.hzg.tesoreria.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {
    @Test
    fun `session expires at the exact configured boundary`() {
        val session = AuthSession(
            token = "jwt-for-test",
            username = "tesoreria",
            nombreCompleto = "Usuario de prueba",
            rol = "Administrador",
            expiresAtEpochMillis = 10_000L,
        )

        assertFalse(session.isExpired(9_999L))
        assertTrue(session.isExpired(10_000L))
        assertTrue(session.isExpired(10_001L))
    }

    @Test
    fun `catalog active flag is case insensitive and rejects other states`() {
        val programa = Programa(1, "Administración", "aCtIvO", "2026-10-27")
        val servicio = Servicio(1, "Constancia", 10.0, "Inactivo")

        assertTrue(programa.activo)
        assertFalse(servicio.activo)
    }
}
