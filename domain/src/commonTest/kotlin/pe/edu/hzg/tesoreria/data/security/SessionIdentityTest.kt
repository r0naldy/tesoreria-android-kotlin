package pe.edu.hzg.tesoreria.domain.model

import kotlin.test.assertEquals
import kotlin.test.Test

class SessionIdentityTest {
    @Test
    fun `username identity is stable across casing and surrounding spaces`() {
        assertEquals("administrador", SessionIdentity.fromUsername("  AdMiNiStRaDoR "))
    }
}
