package pe.edu.hzg.tesoreria.features.academic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.hzg.tesoreria.features.academic.domain.model.EnrollmentItem
import pe.edu.hzg.tesoreria.features.academic.domain.model.EnrollmentRoster
import pe.edu.hzg.tesoreria.features.academic.domain.model.PeriodSummary

class AcademicDocumentFormatterTest {
    @Test
    fun `padron usa estudiantes reales en el mismo orden cargado`() {
        val roster = EnrollmentRoster(
            nombreCetpro = "CETPRO HÉROES DEL CENEPA",
            periodo = "2026",
            programa = "APOYO ADMINISTRATIVO",
            turno = "MAÑANA",
            seccion = "A",
            semestre = "II",
            profesor = "ANA TORRES",
            moduloFormativo = "OFIMÁTICA",
            cantidad = 2,
            hombres = 1,
            mujeres = 1,
            pagantes = 1,
            becados = 1,
            estudiantes = listOf(
                EnrollmentItem(
                    n = 1,
                    codigoMatricula = "46027897",
                    apellidosNombres = "QUISPE EDUARDO",
                    sexo = "M",
                    edad = 22,
                    condicion = "G",
                ),
                EnrollmentItem(
                    n = 2,
                    codigoMatricula = "70405174",
                    apellidosNombres = "MAMANI LUCÍA",
                    sexo = "F",
                    edad = 20,
                    condicion = "B",
                ),
            ),
        )

        val document = AcademicDocumentFormatter.roster(roster)

        assertEquals("QUISPE EDUARDO", document.tableRows[0][2])
        assertEquals("MAMANI LUCÍA", document.tableRows[1][2])
        assertEquals("70405174", document.tableRows[1][1])
        assertTrue(document.metadata.any { it.contains("Semestre: II") })
        assertTrue(document.closingLines.any { it.contains("Pagantes: 1 · Becados: 1") })
        assertTrue(document.fileName.endsWith(".pdf"))
        assertFalse(document.fileName.contains(' '))
        assertFalse(document.fileName.contains('Ñ'))
    }

    @Test
    fun `acta de cierre conserva cifras y responsables devueltos por API`() {
        val summary = PeriodSummary(
            id = 8,
            nombre = "2026-I",
            fechaInicio = "2026-01-01T00:00:00",
            fechaFin = "2026-06-30T23:59:59",
            estado = "Cerrado",
            tickets = 28,
            estudiantes = 21,
            ingresos = 15680.5,
            servicios = 35,
            usuarioApertura = "admin",
            usuarioCierre = "eduardo",
            fechaCierre = "2026-06-30T18:30:00",
        )

        val document = AcademicDocumentFormatter.closingAct(summary)

        assertTrue(document.heading.contains("2026-I"))
        assertTrue(document.metadata.contains("Tickets emitidos: 28"))
        assertTrue(document.metadata.contains("Estudiantes atendidos: 21"))
        assertTrue(document.metadata.contains("Total recaudado: S/ 15680.50"))
        assertTrue(document.metadata.contains("Cerrado por: eduardo"))
        assertTrue(document.metadata.contains("Fecha de cierre: 30/06/2026"))
    }
}
