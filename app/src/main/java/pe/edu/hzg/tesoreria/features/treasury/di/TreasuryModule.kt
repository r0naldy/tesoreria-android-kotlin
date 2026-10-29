package pe.edu.hzg.tesoreria.features.treasury.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryApi
import pe.edu.hzg.tesoreria.features.treasury.data.TreasuryRepository
import pe.edu.hzg.tesoreria.features.treasury.presentation.CashierViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.ExpenseBrowserViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.IncomeReportViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.TicketHistoryViewModel
import pe.edu.hzg.tesoreria.features.treasury.presentation.TreasuryDashboardViewModel

/** Incluir con `includes(treasuryModule(BuildConfig.API_BASE_URL))` desde AppModule. */
fun treasuryModule(baseUrl: String) = module {
    single { TreasuryApi(get(), baseUrl) }
    single { TreasuryRepository(get()) }
    viewModelOf(::TreasuryDashboardViewModel)
    viewModelOf(::CashierViewModel)
    viewModelOf(::TicketHistoryViewModel)
    viewModelOf(::IncomeReportViewModel)
    viewModelOf(::ExpenseBrowserViewModel)
}
