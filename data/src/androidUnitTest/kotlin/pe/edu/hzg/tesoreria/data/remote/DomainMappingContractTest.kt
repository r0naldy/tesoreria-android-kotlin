package pe.edu.hzg.tesoreria.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import pe.edu.hzg.tesoreria.features.academic.data.DniLookupDto
import pe.edu.hzg.tesoreria.features.academic.data.toDomain
import pe.edu.hzg.tesoreria.features.academic.data.toDto
import pe.edu.hzg.tesoreria.features.academic.domain.model.UpdateRecordCommand
import pe.edu.hzg.tesoreria.features.treasury.data.ExpenseDto
import pe.edu.hzg.tesoreria.features.treasury.data.toDomain
import pe.edu.hzg.tesoreria.features.treasury.data.toDto

class DomainMappingContractTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun nestedStudentCareersSurviveTheDomainBoundary() {
        val wire = json.decodeFromString<DniLookupDto>("""{
          "dni":"12345678","existe":true,"nombreCompleto":"Persona de prueba",
          "carreras":[{"alumnoId":7,"programaId":2,"programa":"Cocina",
            "turnoId":1,"turno":"Mañana","seccion":"A","fechaRegistro":"2026-10-29"}],
          "campoFuturo":true
        }""")
        val domain = wire.toDomain()
        assertEquals(7, domain.carreras.single().alumnoId)
        assertNull(domain.carreras.single().periodo)
        assertEquals(wire, domain.toDto())
    }

    @Test fun domainUpdatePreservesExplicitClearsAndDoesNotSendSemesterFields() {
        val body = json.encodeToString(UpdateRecordCommand(23, "", "", "").toDto())
        assertTrue(body.contains("\"profesor\":\"\""))
        assertTrue(body.contains("\"moduloFormativo\":\"\""))
        assertTrue(body.contains("\"resolucionPrograma\":\"\""))
        assertFalse(body.contains("semestre"))
        assertFalse(body.contains("periodoId"))
    }

    @Test fun expenseProductsAndReviewFlagsSurviveMapping() {
        val wire = json.decodeFromString<ExpenseDto>("""{
          "id":9,"total":12.50,"fechaRegistro":"2026-10-29",
          "esDuplicadoProbable":true,"confianza":62,"imagenRuta":"receipt.jpg",
          "productos":[{"orden":1,"descripcion":"Material","cantidad":2,"importe":12.50}]
        }""")
        val domain = wire.toDomain()
        assertTrue(domain.esDuplicadoProbable)
        assertEquals(12.50, domain.productos.single().importe!!, 0.0)
        assertEquals(wire, domain.toDto())
    }
}
