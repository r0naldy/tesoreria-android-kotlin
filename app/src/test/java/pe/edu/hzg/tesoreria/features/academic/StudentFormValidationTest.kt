package pe.edu.hzg.tesoreria.features.academic

import java.time.LocalDate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.hzg.tesoreria.features.academic.data.studentPersonalDataError

class StudentFormValidationTest {
    private val today = LocalDate.of(2026, 10, 28)

    @Test fun acceptsOptionalEmptyFields() {
        assertNull(studentPersonalDataError("Ana", "Pérez", "", "", today))
    }

    @Test fun acceptsInternationalPhoneAndLeapDate() {
        assertNull(studentPersonalDataError("Ana", "Pérez", "2000-02-29", "+51 987 654 321", today))
    }

    @Test fun rejectsInvalidAndFutureDates() {
        listOf("2023-02-29", "2001-13-04", "2027-01-01", "1899-12-31").forEach {
            assertNotNull(studentPersonalDataError("Ana", "Pérez", it, "", today))
        }
    }

    @Test fun requiresNamesAndBoundsAllInputs() {
        assertNotNull(studentPersonalDataError(" ", "Pérez", "", "", today))
        assertNotNull(studentPersonalDataError("Ana", "", "", "", today))
        assertNotNull(studentPersonalDataError("a".repeat(151), "Pérez", "", "", today))
        assertNotNull(studentPersonalDataError("Ana", "Pérez", "", "abcdefghi", today))
    }
}
