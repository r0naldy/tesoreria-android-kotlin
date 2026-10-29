package pe.edu.hzg.tesoreria.features.academic

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.hzg.tesoreria.features.academic.data.RecordDto
import pe.edu.hzg.tesoreria.features.academic.data.UpdateRecordRequest
import pe.edu.hzg.tesoreria.features.academic.data.recordPersonalDataError

class RecordUpdateContractTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun `update only sends editable base fields`() {
        val request = UpdateRecordRequest(23, "Docente", "Módulo", "RD-123")
        val body = json.encodeToString(request)
        assertTrue(body.contains("\"id\":23"))
        assertTrue(body.contains("\"profesor\":\"Docente\""))
        assertTrue(body.contains("\"moduloFormativo\":\"Módulo\""))
        assertTrue(body.contains("\"resolucionPrograma\":\"RD-123\""))
        assertFalse(body.contains("semestre"))
        assertFalse(body.contains("programaId"))
        assertFalse(body.contains("periodoId"))
    }

    @Test fun `empty fields remain explicit to clear data`() {
        val body = json.encodeToString(UpdateRecordRequest(23, "", "", ""))
        assertTrue(body.contains("\"profesor\":\"\""))
        assertTrue(body.contains("\"moduloFormativo\":\"\""))
        assertTrue(body.contains("\"resolucionPrograma\":\"\""))
        assertNull(recordPersonalDataError(UpdateRecordRequest(23)))
    }

    @Test fun `validation matches database column limits`() {
        assertNull(recordPersonalDataError(UpdateRecordRequest(23, "d".repeat(150), "m".repeat(200), "r".repeat(150))))
        assertNotNull(recordPersonalDataError(UpdateRecordRequest(0)))
        assertNotNull(recordPersonalDataError(UpdateRecordRequest(23, profesor = "d".repeat(151))))
        assertNotNull(recordPersonalDataError(UpdateRecordRequest(23, moduloFormativo = "m".repeat(201))))
        assertNotNull(recordPersonalDataError(UpdateRecordRequest(23, resolucionPrograma = "r".repeat(151))))
    }

    @Test fun `list decodes resolution so editor cannot silently erase it`() {
        val dto = json.decodeFromString<RecordDto>("""{"id":23,"periodoId":1,"periodo":"2026","programaId":2,"programa":"Cocina","turnoId":1,"turno":"Mañana","seccion":"U","cantidad":0,"fechaCreacion":"2026-01-01","resolucionPrograma":"RD-123"}""")
        assertEquals("RD-123", dto.resolucionPrograma)
    }
}
