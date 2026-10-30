package pe.edu.hzg.tesoreria.features.academic

import pe.edu.hzg.tesoreria.platform.PlatformContext
import pe.edu.hzg.tesoreria.features.academic.domain.model.EnrollmentRoster
import pe.edu.hzg.tesoreria.features.academic.domain.model.PeriodSummary

expect object AcademicPdfDocuments {
    fun rosterBytes(roster: EnrollmentRoster): ByteArray
    fun rosterFileName(roster: EnrollmentRoster): String
    fun closingActBytes(summary: PeriodSummary): ByteArray
    fun closingActFileName(summary: PeriodSummary): String
}

expect object AcademicDocumentOutput {
    fun printRoster(context: PlatformContext, roster: EnrollmentRoster)
    fun printClosingAct(context: PlatformContext, summary: PeriodSummary)
}
