package pe.edu.hzg.tesoreria.security

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteReceiptImagePolicyTest {
    private val apiBase = "https://sistema.cetprohzgj.edu.pe"

    @Test
    fun `accepts only https URLs from the configured API origin`() {
        assertEquals(
            URI("https://sistema.cetprohzgj.edu.pe/uploads/receipt.jpg"),
            RemoteReceiptImagePolicy.resolveInitial("/uploads/receipt.jpg", apiBase),
        )
        assertNull(RemoteReceiptImagePolicy.resolveInitial("http://sistema.cetprohzgj.edu.pe/a.jpg", apiBase))
        assertNull(RemoteReceiptImagePolicy.resolveInitial("https://evil.example/a.jpg", apiBase))
        assertNull(RemoteReceiptImagePolicy.resolveInitial("//evil.example/a.jpg", apiBase))
        assertNull(RemoteReceiptImagePolicy.resolveInitial("https://sistema.cetprohzgj.edu.pe:444/a.jpg", apiBase))
    }

    @Test
    fun `allows same-origin relative redirect and rejects external redirect`() {
        val current = URI("https://sistema.cetprohzgj.edu.pe/uploads/old.jpg")
        assertEquals(
            URI("https://sistema.cetprohzgj.edu.pe/images/new.jpg"),
            RemoteReceiptImagePolicy.resolveRedirect("../images/new.jpg", current, apiBase),
        )
        assertNull(RemoteReceiptImagePolicy.resolveRedirect("https://cdn.example/new.jpg", current, apiBase))
        assertNull(RemoteReceiptImagePolicy.resolveRedirect("http://sistema.cetprohzgj.edu.pe/new.jpg", current, apiBase))
    }
}
