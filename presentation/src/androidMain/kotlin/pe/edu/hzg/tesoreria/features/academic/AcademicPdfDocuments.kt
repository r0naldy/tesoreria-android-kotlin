package pe.edu.hzg.tesoreria.features.academic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale
import pe.edu.hzg.tesoreria.features.academic.domain.model.EnrollmentRoster
import pe.edu.hzg.tesoreria.features.academic.domain.model.PeriodSummary

/**
 * Modelo intermedio sin dependencias de Android. Mantiene separados los datos reales del API de
 * su representación en PDF y permite verificar el contenido con pruebas JVM puras.
 */
internal data class AcademicDocumentContent(
    val fileName: String,
    val jobName: String,
    val heading: String,
    val institution: String,
    val metadata: List<String>,
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val columnWidths: List<Float> = emptyList(),
    val closingLines: List<String> = emptyList(),
    val signatureLabels: List<String> = emptyList(),
)

internal object AcademicDocumentFormatter {
    fun roster(roster: EnrollmentRoster): AcademicDocumentContent {
        val institution = roster.nombreCetpro.ifBlank {
            roster.nombreInstitucion.ifBlank { "Institución educativa" }
        }
        val semester = roster.semestre?.takeIf(String::isNotBlank) ?: "No especificado"
        val metadata = buildList {
            add("Período: ${roster.periodo.ifBlank { "No especificado" }} · Semestre: $semester")
            add("Programa: ${roster.programa.ifBlank { "No especificado" }}")
            add("Turno: ${roster.turno.ifBlank { "No especificado" }} · Sección: ${roster.seccion.ifBlank { "—" }}")
            add("Docente: ${roster.profesor?.takeIf(String::isNotBlank) ?: "Sin asignar"}")
            add("Módulo formativo: ${roster.moduloFormativo?.takeIf(String::isNotBlank) ?: "Sin asignar"}")
            roster.codigoModular.takeIf(String::isNotBlank)?.let { add("Código modular: $it") }
            val location = listOf(roster.direccion, roster.ciudad).filter(String::isNotBlank).joinToString(" · ")
            if (location.isNotBlank()) add("Ubicación: $location")
            val jurisdiction = listOf(
                roster.dreGre.takeIf(String::isNotBlank)?.let { "DRE/GRE: $it" },
                roster.ugel.takeIf(String::isNotBlank)?.let { "UGEL: $it" },
            ).filterNotNull().joinToString(" · ")
            if (jurisdiction.isNotBlank()) add(jurisdiction)
            val academic = listOf(
                roster.modalidadServicio.takeIf(String::isNotBlank),
                roster.nivelFormativo.takeIf(String::isNotBlank),
                roster.tipoPlan.takeIf(String::isNotBlank),
            ).filterNotNull().joinToString(" · ")
            if (academic.isNotBlank()) add(academic)
        }
        val suffix = listOf(roster.periodo, semester, roster.programa, roster.turno, roster.seccion)
            .filter(String::isNotBlank)
            .joinToString("_")
        return AcademicDocumentContent(
            fileName = "padron_${fileToken(suffix)}.pdf",
            jobName = "Padrón ${roster.programa} ${roster.seccion}".trim(),
            heading = "PADRÓN / REGISTRO DE MATRÍCULA",
            institution = institution,
            metadata = metadata,
            tableHeaders = listOf("N°", "DNI / CÓDIGO", "APELLIDOS Y NOMBRES", "SEXO", "EDAD", "CONDICIÓN"),
            tableRows = roster.estudiantes.map { student ->
                listOf(
                    student.n.toString(),
                    student.codigoMatricula,
                    student.apellidosNombres,
                    student.sexo?.takeIf(String::isNotBlank) ?: "—",
                    student.edad?.toString() ?: "—",
                    student.condicion.ifBlank { "—" },
                )
            },
            columnWidths = listOf(28f, 72f, 250f, 40f, 43f, 90f),
            closingLines = listOf(
                "Total: ${roster.cantidad} estudiante(s) · Hombres: ${roster.hombres} · Mujeres: ${roster.mujeres}",
                "Pagantes: ${roster.pagantes} · Becados: ${roster.becados}",
                "Documento generado con los datos vigentes cargados por el sistema empresarial.",
            ),
            signatureLabels = listOf("Docente responsable", "Director(a) / Responsable"),
        )
    }

    fun closingAct(summary: PeriodSummary): AcademicDocumentContent = AcademicDocumentContent(
        fileName = "acta_cierre_${fileToken(summary.nombre)}.pdf",
        jobName = "Acta de cierre ${summary.nombre}",
        heading = "ACTA DE CIERRE DEL PERÍODO ${summary.nombre}",
        institution = "Sistema de Tesorería",
        metadata = buildList {
            add("Rango: ${shortDate(summary.fechaInicio)} – ${shortDate(summary.fechaFin)}")
            add("Estado: ${summary.estado}")
            add("Tickets emitidos: ${summary.tickets}")
            add("Estudiantes atendidos: ${summary.estudiantes}")
            add("Servicios cobrados: ${summary.servicios}")
            add("Total recaudado: ${money(summary.ingresos)}")
            summary.usuarioApertura?.takeIf(String::isNotBlank)?.let { add("Abierto por: $it") }
            summary.usuarioCierre?.takeIf(String::isNotBlank)?.let { add("Cerrado por: $it") }
            summary.fechaCierre?.takeIf(String::isNotBlank)?.let { add("Fecha de cierre: ${shortDate(it)}") }
        },
        closingLines = listOf(
            "Se deja constancia de que las cifras precedentes corresponden al resumen devuelto por el sistema empresarial para el período indicado.",
        ),
        signatureLabels = listOf("Responsable de Tesorería", "Administrador(a)"),
    )

    private fun money(value: Double): String = String.format(Locale.US, "S/ %.2f", value)

    private fun shortDate(value: String): String = value
        .substringBefore('T')
        .split('-')
        .takeIf { it.size == 3 }
        ?.let { "${it[2]}/${it[1]}/${it[0]}" }
        ?: value

    private fun fileToken(value: String): String {
        val ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return ascii
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .take(80)
            .ifBlank { "documento" }
    }
}

/** Genera el mismo documento para compartir por FileProvider o enviarlo a Android PrintManager. */
actual object AcademicPdfDocuments {
    actual fun rosterBytes(roster: EnrollmentRoster): ByteArray =
        AcademicPdfRenderer(AcademicDocumentFormatter.roster(roster)).render()

    actual fun rosterFileName(roster: EnrollmentRoster): String =
        AcademicDocumentFormatter.roster(roster).fileName

    actual fun closingActBytes(summary: PeriodSummary): ByteArray =
        AcademicPdfRenderer(AcademicDocumentFormatter.closingAct(summary)).render()

    actual fun closingActFileName(summary: PeriodSummary): String =
        AcademicDocumentFormatter.closingAct(summary).fileName
}

actual object AcademicDocumentOutput {
    actual fun printRoster(context: Context, roster: EnrollmentRoster) {
        val content = AcademicDocumentFormatter.roster(roster)
        print(context, content) { AcademicPdfRenderer(content).render() }
    }

    actual fun printClosingAct(context: Context, summary: PeriodSummary) {
        val content = AcademicDocumentFormatter.closingAct(summary)
        print(context, content) { AcademicPdfRenderer(content).render() }
    }

    private fun print(context: Context, content: AcademicDocumentContent, bytes: () -> ByteArray) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins(250, 250, 250, 250))
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        manager.print(
            content.jobName,
            AcademicPdfAdapter(content.fileName, bytes),
            attributes,
        )
    }
}

private class AcademicPdfAdapter(
    private val fileName: String,
    private val bytes: () -> ByteArray,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal.isCanceled) return callback.onWriteCancelled()
        try {
            FileOutputStream(destination.fileDescriptor).use { it.write(bytes()) }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled()
            else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            callback.onWriteFailed(error.message ?: "No se pudo preparar el documento")
        }
    }
}

private class AcademicPdfRenderer(private val content: AcademicDocumentContent) {
    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36f
        const val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN - 20f
        const val TABLE_LINE_HEIGHT = 10.5f
    }

    private val document = PdfDocument()
    private val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val bold = Paint(normal).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private val title = Paint(bold).apply { textSize = 15f }
    private val subtitle = Paint(bold).apply { textSize = 11f }
    private val small = Paint(normal).apply { textSize = 7.5f }
    private val table = Paint(normal).apply { textSize = 7.2f }
    private val tableBold = Paint(table).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(135, 135, 135)
        style = Paint.Style.STROKE
        strokeWidth = .55f
    }
    private val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 232, 246)
        style = Paint.Style.FILL
    }

    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = MARGIN

    fun render(): ByteArray {
        try {
            startPage()
            drawMetadata()
            if (content.tableHeaders.isNotEmpty()) drawTable()
            drawClosing()
            finishPage()
            return ByteArrayOutputStream().use { output ->
                document.writeTo(output)
                output.toByteArray()
            }
        } finally {
            runCatching { finishPage() }
            document.close()
        }
    }

    private fun startPage() {
        finishPage()
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = requireNotNull(page).canvas
        y = MARGIN
        drawCentered(content.institution.uppercase(Locale.getDefault()), title)
        y += 3f
        drawCentered(content.heading, subtitle)
        canvas.drawText("Página $pageNumber", PAGE_WIDTH - MARGIN - small.measureText("Página $pageNumber"), MARGIN, small)
        y += 7f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, grid)
        y += 11f
    }

    private fun finishPage() {
        page?.let(document::finishPage)
        page = null
    }

    private fun drawMetadata() {
        content.metadata.forEach { line ->
            drawWrapped(line, normal, 12f)
        }
        y += 5f
    }

    private fun drawTable() {
        drawTableHeader()
        if (content.tableRows.isEmpty()) {
            drawWrapped("Sin estudiantes registrados para el filtro seleccionado.", normal, 13f)
            return
        }
        content.tableRows.forEach { cells -> drawTableRow(cells) }
        y += 7f
    }

    private fun drawTableHeader() {
        val height = 24f
        ensureSpace(height, repeatTableHeader = false)
        var x = MARGIN
        content.tableHeaders.forEachIndexed { index, header ->
            val width = content.columnWidths.getOrElse(index) { 60f }
            canvas.drawRect(x, y, x + width, y + height, headerFill)
            canvas.drawRect(x, y, x + width, y + height, grid)
            val lines = wrap(header, tableBold, width - 6f).take(2)
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x + 3f, y + 9f + lineIndex * TABLE_LINE_HEIGHT, tableBold)
            }
            x += width
        }
        y += height
    }

    private fun drawTableRow(cells: List<String>) {
        val wrapped = content.columnWidths.mapIndexed { index, width ->
            wrap(cells.getOrElse(index) { "" }, table, width - 6f).take(4)
        }
        val height = maxOf(20f, (wrapped.maxOfOrNull { it.size } ?: 1) * TABLE_LINE_HEIGHT + 7f)
        ensureSpace(height, repeatTableHeader = true)
        var x = MARGIN
        content.columnWidths.forEachIndexed { index, width ->
            canvas.drawRect(x, y, x + width, y + height, grid)
            wrapped[index].forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x + 3f, y + 10f + lineIndex * TABLE_LINE_HEIGHT, table)
            }
            x += width
        }
        y += height
    }

    private fun drawClosing() {
        if (content.closingLines.isNotEmpty()) {
            y += 4f
            content.closingLines.forEach { drawWrapped(it, normal, 12f) }
        }
        if (content.signatureLabels.isNotEmpty()) {
            ensureSpace(75f, repeatTableHeader = false)
            y += 40f
            val available = PAGE_WIDTH - 2f * MARGIN
            val slot = available / content.signatureLabels.size
            content.signatureLabels.forEachIndexed { index, label ->
                val center = MARGIN + slot * index + slot / 2f
                val lineHalf = minOf(70f, slot / 2f - 12f)
                canvas.drawLine(center - lineHalf, y, center + lineHalf, y, grid)
                val textX = center - small.measureText(label) / 2f
                canvas.drawText(label, textX, y + 12f, small)
            }
        }
    }

    private fun ensureSpace(height: Float, repeatTableHeader: Boolean) {
        if (y + height <= CONTENT_BOTTOM) return
        startPage()
        if (repeatTableHeader) drawTableHeader()
    }

    private fun drawCentered(text: String, paint: Paint) {
        val maxWidth = PAGE_WIDTH - 2f * MARGIN
        val lines = wrap(text, paint, maxWidth)
        lines.forEach { line ->
            canvas.drawText(line, (PAGE_WIDTH - paint.measureText(line)) / 2f, y, paint)
            y += paint.textSize + 4f
        }
    }

    private fun drawWrapped(text: String, paint: Paint, lineHeight: Float) {
        val maxWidth = PAGE_WIDTH - 2f * MARGIN
        val lines = wrap(text, paint, maxWidth)
        ensureSpace(lines.size * lineHeight + 2f, repeatTableHeader = false)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN, y, paint)
            y += lineHeight
        }
    }

    private fun wrap(value: String, paint: Paint, maxWidth: Float): List<String> {
        val text = value.trim().ifBlank { "—" }
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val lines = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).forEach { word ->
            if (paint.measureText(word) > maxWidth) {
                if (current.isNotBlank()) {
                    lines += current
                    current = ""
                }
                var remaining = word
                while (remaining.isNotEmpty()) {
                    var end = 1
                    while (end < remaining.length && paint.measureText(remaining.substring(0, end + 1)) <= maxWidth) end++
                    lines += remaining.substring(0, end)
                    remaining = remaining.substring(end)
                }
            } else {
                val candidate = if (current.isBlank()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) current = candidate
                else {
                    lines += current
                    current = word
                }
            }
        }
        if (current.isNotBlank()) lines += current
        return lines.ifEmpty { listOf("—") }
    }
}
