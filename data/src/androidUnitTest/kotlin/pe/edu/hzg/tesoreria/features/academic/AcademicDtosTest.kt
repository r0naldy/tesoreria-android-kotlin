package pe.edu.hzg.tesoreria.features.academic

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.hzg.tesoreria.features.academic.data.ComparisonDto
import pe.edu.hzg.tesoreria.features.academic.data.CreateStudentRequest
import pe.edu.hzg.tesoreria.features.academic.data.DniLookupDto
import pe.edu.hzg.tesoreria.features.academic.data.OpenPeriodRequest

class AcademicDtosTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `consulta DNI conserva carreras y tolera campos futuros`() {
        val dto = json.decodeFromString<DniLookupDto>(
            """
            {
              "dni":"46027897",
              "existe":true,
              "encontradoReniec":false,
              "alumnoId":17,
              "nombres":"EDUARDO",
              "apellidos":"QUISPE",
              "nombreCompleto":"QUISPE EDUARDO",
              "campoFuturo":"no rompe",
              "carreras":[{
                "alumnoId":17,
                "programaId":2,
                "programa":"APOYO ADMINISTRATIVO",
                "turnoId":1,
                "turno":"MAÑANA",
                "seccion":"U",
                "periodo":"2026",
                "fechaRegistro":"2026-03-01T12:00:00Z"
              }]
            }
            """.trimIndent(),
        )

        assertTrue(dto.existe)
        assertEquals(17, dto.carreras.single().alumnoId)
        assertEquals("APOYO ADMINISTRATIVO", dto.carreras.single().programa)
    }

    @Test
    fun `alta de alumno usa los nombres exactos del contrato REST`() {
        val body = json.encodeToString(
            CreateStudentRequest(
                dni = "46027897",
                nombres = "EDUARDO",
                apellidos = "QUISPE",
                programaId = 2,
                turnoId = 1,
                condicion = "G",
            ),
        )

        assertTrue(body.contains("\"programaId\":2"))
        assertTrue(body.contains("\"turnoId\":1"))
        assertTrue(body.contains("\"condicion\":\"G\""))
        assertFalse(body.contains("program_id"))
    }

    @Test
    fun `apertura anual construye las fechas exigidas por backend`() {
        val request = OpenPeriodRequest(nombre = "2027")
        assertEquals("2027-01-01", request.fechaInicio)
        assertEquals("2027-12-31", request.fechaFin)
    }

    @Test
    fun `comparativa decodifica serie grupos y movimientos`() {
        val dto = json.decodeFromString<ComparisonDto>(
            """
            {
              "totalIngresos":100.0,
              "totalEgresos":35.0,
              "balance":65.0,
              "cantidadIngresos":2,
              "cantidadEgresos":1,
              "serie":[{"fecha":"2026-10-27","ingresos":100.0,"egresos":35.0}],
              "egresosPorCategoria":[{"nombre":"Movilidad","cantidad":1,"monto":35.0}],
              "ingresosPorConcepto":[{"nombre":"Matrícula","cantidad":2,"monto":100.0}],
              "movimientos":[{
                "tipo":"Ingreso","fecha":"2026-10-27T10:00:00",
                "descripcion":"001/001","detalle":"ALUMNO","monto":50.0,"usuario":"admin"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(65.0, dto.balance, 0.001)
        assertEquals("Movilidad", dto.egresosPorCategoria.single().nombre)
        assertEquals("Ingreso", dto.movimientos.single().tipo)
    }
}
