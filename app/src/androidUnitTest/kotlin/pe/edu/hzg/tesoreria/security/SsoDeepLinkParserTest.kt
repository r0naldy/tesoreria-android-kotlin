package pe.edu.hzg.tesoreria.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsoDeepLinkParserTest {
    private val baseUrl = "https://sistema.cetprohzgj.edu.pe"
    private val jwt = "headerheader.payloadpayload.signaturesignature"

    @Test
    fun `accepts signed-token shape only in fragment on expected HTTPS origin`() {
        assertEquals(
            SsoDeepLinkResult.Valid(jwt),
            SsoDeepLinkParser.parse("$baseUrl/sso#token=$jwt", baseUrl),
        )
    }

    @Test
    fun `rejects token in query because it would reach infrastructure logs`() {
        val result = SsoDeepLinkParser.parse("$baseUrl/sso?token=$jwt", baseUrl)
        assertTrue(result is SsoDeepLinkResult.Invalid)
    }

    @Test
    fun `ignores lookalike host path and insecure scheme`() {
        listOf(
            "https://evil.example/sso#token=$jwt",
            "https://sistema.cetprohzgj.edu.pe.evil.example/sso#token=$jwt",
            "http://sistema.cetprohzgj.edu.pe/sso#token=$jwt",
            "$baseUrl/sso-falso#token=$jwt",
        ).forEach { uri ->
            assertEquals(SsoDeepLinkResult.NotSsoLink, SsoDeepLinkParser.parse(uri, baseUrl))
        }
    }

    @Test
    fun `rejects missing duplicate malformed and oversized tokens`() {
        val oversized = "a.${"b".repeat(8_193)}.c"
        listOf(
            "$baseUrl/sso",
            "$baseUrl/sso#token=$jwt&token=$jwt",
            "$baseUrl/sso#token=not-a-jwt",
            "$baseUrl/sso#token=$oversized",
        ).forEach { uri ->
            assertTrue(SsoDeepLinkParser.parse(uri, baseUrl) is SsoDeepLinkResult.Invalid)
        }
    }
}
