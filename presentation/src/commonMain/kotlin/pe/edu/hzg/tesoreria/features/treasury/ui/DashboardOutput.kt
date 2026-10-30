package pe.edu.hzg.tesoreria.features.treasury.ui

import pe.edu.hzg.tesoreria.platform.PlatformContext
import pe.edu.hzg.tesoreria.features.treasury.domain.model.IncomeReport
import pe.edu.hzg.tesoreria.features.treasury.domain.dashboardCsv

expect object DashboardOutput {
    fun share(context: PlatformContext, summary: String)
    fun exportCsv(context: PlatformContext, report: IncomeReport)
    fun print(context: PlatformContext, summary: String)
}
