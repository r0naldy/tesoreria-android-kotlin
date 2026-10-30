package pe.edu.hzg.tesoreria.features.academic

import pe.edu.hzg.tesoreria.features.academic.data.AcademicRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.edu.hzg.tesoreria.features.academic.data.AcademicApi
import pe.edu.hzg.tesoreria.features.academic.domain.repository.AcademicRepository
import pe.edu.hzg.tesoreria.features.academic.presentation.ComparisonViewModel
import pe.edu.hzg.tesoreria.features.academic.presentation.PeriodsViewModel
import pe.edu.hzg.tesoreria.features.academic.presentation.RecordsViewModel
import pe.edu.hzg.tesoreria.features.academic.presentation.StudentsViewModel

/** Se incluye desde AppModule con `includes(academicModule(BuildConfig.API_BASE_URL))`. */
fun academicModule(baseUrl: String) = module {
    single { AcademicApi(get(), baseUrl) }
    single<AcademicRepository> { AcademicRepositoryImpl(get()) }
    single<AcademicExportManager> { AndroidAcademicExportManager(androidContext()) }

    viewModelOf(::StudentsViewModel)
    viewModelOf(::PeriodsViewModel)
    viewModelOf(::RecordsViewModel)
    viewModelOf(::ComparisonViewModel)
}
