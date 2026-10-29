package pe.edu.hzg.tesoreria.admin.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.edu.hzg.tesoreria.admin.data.AdminApi
import pe.edu.hzg.tesoreria.admin.data.AdminRepository
import pe.edu.hzg.tesoreria.admin.data.OnlineAdminRepository
import pe.edu.hzg.tesoreria.admin.presentation.AuditoriaViewModel
import pe.edu.hzg.tesoreria.admin.presentation.ConfiguracionViewModel
import pe.edu.hzg.tesoreria.admin.presentation.UsuariosViewModel

fun adminModule(baseUrl: String) = module {
    single { AdminApi(apiClient = get(), baseUrl = baseUrl) }
    single<AdminRepository> { OnlineAdminRepository(get()) }

    viewModelOf(::UsuariosViewModel)
    viewModelOf(::ConfiguracionViewModel)
    viewModelOf(::AuditoriaViewModel)
}
