package pe.edu.hzg.tesoreria.features.treasury.ui

import pe.edu.hzg.tesoreria.platform.PlatformContext
import pe.edu.hzg.tesoreria.features.treasury.domain.model.IncomeReport
import pe.edu.hzg.tesoreria.features.treasury.domain.model.ReportSummary

expect object IncomeReportOutput {
    fun shareSummary(context: PlatformContext, report: IncomeReport)
    fun exportCsv(context: PlatformContext, report: IncomeReport)
}
