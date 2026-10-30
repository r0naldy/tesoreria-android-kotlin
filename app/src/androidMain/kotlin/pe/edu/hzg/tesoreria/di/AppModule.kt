package pe.edu.hzg.tesoreria.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.edu.hzg.tesoreria.BuildConfig
import pe.edu.hzg.tesoreria.admin.di.adminModule
import pe.edu.hzg.tesoreria.data.di.dataModule
import pe.edu.hzg.tesoreria.db.AppDatabase
import pe.edu.hzg.tesoreria.domain.usecase.CreateProgramaUseCase
import pe.edu.hzg.tesoreria.domain.usecase.CreateServicioUseCase
import pe.edu.hzg.tesoreria.domain.usecase.DeleteProgramaUseCase
import pe.edu.hzg.tesoreria.domain.usecase.DeleteServicioUseCase
import pe.edu.hzg.tesoreria.domain.usecase.FindComprobanteUseCase
import pe.edu.hzg.tesoreria.domain.usecase.LoginUseCase
import pe.edu.hzg.tesoreria.domain.usecase.LogoutUseCase
import pe.edu.hzg.tesoreria.domain.usecase.ObserveComprobantesUseCase
import pe.edu.hzg.tesoreria.domain.usecase.ObservePendingCountUseCase
import pe.edu.hzg.tesoreria.domain.usecase.ObserveProgramasUseCase
import pe.edu.hzg.tesoreria.domain.usecase.ObserveServiciosUseCase
import pe.edu.hzg.tesoreria.domain.usecase.ObserveSessionUseCase
import pe.edu.hzg.tesoreria.domain.usecase.QueueComprobanteUseCase
import pe.edu.hzg.tesoreria.domain.usecase.RefreshComprobantesUseCase
import pe.edu.hzg.tesoreria.domain.usecase.RefreshProgramasUseCase
import pe.edu.hzg.tesoreria.domain.usecase.RefreshServiciosUseCase
import pe.edu.hzg.tesoreria.domain.usecase.SynchronizePendingUseCase
import pe.edu.hzg.tesoreria.domain.usecase.UpdateProgramaUseCase
import pe.edu.hzg.tesoreria.domain.usecase.UpdateServicioUseCase
import pe.edu.hzg.tesoreria.presentation.AppViewModel
import pe.edu.hzg.tesoreria.presentation.EgresosViewModel
import pe.edu.hzg.tesoreria.presentation.LoginViewModel
import pe.edu.hzg.tesoreria.presentation.ProgramasViewModel
import pe.edu.hzg.tesoreria.presentation.ServiciosViewModel
import pe.edu.hzg.tesoreria.features.academic.academicModule
import pe.edu.hzg.tesoreria.features.treasury.di.treasuryModule

val appModule = module {
    includes(
        dataModule(BuildConfig.API_BASE_URL),
        treasuryModule(BuildConfig.API_BASE_URL),
        academicModule(BuildConfig.API_BASE_URL),
        adminModule(BuildConfig.API_BASE_URL),
    )

    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
        }
    }

    single<pe.edu.hzg.tesoreria.domain.repository.ErrorMessages> { pe.edu.hzg.tesoreria.data.remote.DefaultErrorMessages() }
    single { pe.edu.hzg.tesoreria.domain.model.ApiConfiguration(BuildConfig.API_BASE_URL) }

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = get(),
            name = "tesoreria.db",
        )
    }
    single { AppDatabase(driver = get()) }

    factoryOf(::LoginUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ObserveProgramasUseCase)
    factoryOf(::RefreshProgramasUseCase)
    factoryOf(::CreateProgramaUseCase)
    factoryOf(::UpdateProgramaUseCase)
    factoryOf(::DeleteProgramaUseCase)
    factoryOf(::ObserveServiciosUseCase)
    factoryOf(::RefreshServiciosUseCase)
    factoryOf(::CreateServicioUseCase)
    factoryOf(::UpdateServicioUseCase)
    factoryOf(::DeleteServicioUseCase)
    factoryOf(::ObserveComprobantesUseCase)
    factoryOf(::RefreshComprobantesUseCase)
    factoryOf(::FindComprobanteUseCase)
    factoryOf(::QueueComprobanteUseCase)
    factoryOf(::ObservePendingCountUseCase)
    factoryOf(::SynchronizePendingUseCase)

    viewModelOf(::AppViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::ProgramasViewModel)
    viewModelOf(::ServiciosViewModel)
    viewModelOf(::EgresosViewModel)
}
