package pe.edu.hzg.tesoreria.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionIdentityTest {
    @Test
    fun `username identity is stable across casing and surrounding spaces`() {
        assertEquals("administrador", SessionIdentity.fromUsername("  AdMiNiStRaDoR "))
    }
}
