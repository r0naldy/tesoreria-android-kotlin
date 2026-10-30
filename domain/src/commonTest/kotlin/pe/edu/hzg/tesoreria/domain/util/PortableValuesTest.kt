package pe.edu.hzg.tesoreria.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class PortableValuesTest {
    @Test fun decimalAmountsDoNotDependOnBinaryMultiplicationOrLocale() {
        assertEquals("1.01", decimal(1.005))
        assertEquals("10.00", decimal(9.999))
        assertEquals("-12.35", decimal(-12.345))
        assertEquals("0.00", decimal(1e-8))
        assertEquals("100000000.00", decimal(1e8))
        assertEquals("0.1", decimal(0.05, 1))
    }

    @Test fun nonFiniteAmountsCannotBeExported() {
        assertFailsWith<IllegalArgumentException> { decimal(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { decimal(Double.POSITIVE_INFINITY) }
    }

    @Test fun calendarRangesWorkAcrossLeapDaysAndYearBoundaries() {
        assertEquals(LocalDate(2024, 2, 29), LocalDate(2024, 3, 1).minusDays(1))
        assertEquals(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1).minusDays(1))
        assertEquals(LocalDate(2026, 10, 1), LocalDate(2026, 10, 29).withDayOfMonth(1))
    }
}
