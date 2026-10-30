package pe.edu.hzg.tesoreria.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPresentationTest {
    @Test fun `search ignores case accents and surrounding spaces`() {
        assertTrue(matchesCatalogQuery("MECÁNICA DE MOTORES", "  mecanica "))
        assertTrue(matchesCatalogQuery("Gastronomía", ""))
        assertFalse(matchesCatalogQuery("Gastronomía", "electrónica"))
    }
    @Test fun `filters include the intended states`() {
        assertTrue(CatalogStateFilter.ALL.includes(true))
        assertTrue(CatalogStateFilter.ALL.includes(false))
        assertTrue(CatalogStateFilter.ACTIVE.includes(true))
        assertFalse(CatalogStateFilter.ACTIVE.includes(false))
        assertTrue(CatalogStateFilter.INACTIVE.includes(false))
        assertFalse(CatalogStateFilter.INACTIVE.includes(true))
    }
    @Test fun `price accepts Peruvian comma or decimal point and free services`() {
        assertEquals(15.5, parseCatalogPrice("15,50")!!, 0.0)
        assertEquals(15.5, parseCatalogPrice(" 15.50 ")!!, 0.0)
        assertEquals(0.0, parseCatalogPrice("0")!!, 0.0)
        assertEquals(99999999.99, parseCatalogPrice("99999999.99")!!, 0.0)
    }
    @Test fun `price rejects invalid unsafe or silently rounded amounts`() {
        listOf("", "NaN", "Infinity", "-1", "1e5", "100000000", "2.999", "1,000.00", ".50").forEach {
            assertNull("Should reject $it", parseCatalogPrice(it))
        }
    }
    @Test fun `currency uses consistent decimal separator independent of device locale`() {
        assertEquals("S/ 15.50", formatCatalogPrice(15.5))
        assertEquals("0.00", formatCatalogPriceInput(0.0))
    }
}
