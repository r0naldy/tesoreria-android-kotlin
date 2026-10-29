package pe.edu.hzg.tesoreria.data.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.hzg.tesoreria.data.local.ReceiptImageReader
import pe.edu.hzg.tesoreria.data.remote.ApiClient
import pe.edu.hzg.tesoreria.data.remote.CatalogApi
import pe.edu.hzg.tesoreria.data.remote.TesoreriaApi
import pe.edu.hzg.tesoreria.data.repository.AuthRepositoryImpl
import pe.edu.hzg.tesoreria.data.repository.ComprobanteLocalWriter
import pe.edu.hzg.tesoreria.data.repository.ComprobanteRepositoryImpl
import pe.edu.hzg.tesoreria.data.repository.InstitutionalRepositoryImpl
import pe.edu.hzg.tesoreria.data.repository.ProgramaRepositoryImpl
import pe.edu.hzg.tesoreria.data.repository.ServicioRepositoryImpl
import pe.edu.hzg.tesoreria.data.security.KeystoreTokenStore
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.data.sync.OfflineSyncRepository
import pe.edu.hzg.tesoreria.data.sync.PendingOperationStore
import pe.edu.hzg.tesoreria.data.sync.SyncScheduler
import pe.edu.hzg.tesoreria.data.sync.WorkManagerSyncScheduler
import pe.edu.hzg.tesoreria.domain.repository.AuthRepository
import pe.edu.hzg.tesoreria.domain.repository.ComprobanteRepository
import pe.edu.hzg.tesoreria.domain.repository.InstitutionalRepository
import pe.edu.hzg.tesoreria.domain.repository.ProgramaRepository
import pe.edu.hzg.tesoreria.domain.repository.ServicioRepository
import pe.edu.hzg.tesoreria.domain.repository.SyncRepository

fun dataModule(baseUrl: String) = module {
    single<TokenStore> { KeystoreTokenStore(androidContext(), get()) }
    single { ApiClient(baseUrl, get(), get()) }
    single { TesoreriaApi(get(), baseUrl) }
    single<CatalogApi> { get<TesoreriaApi>() }

    single { ReceiptImageReader(androidContext().contentResolver) }
    single { PendingOperationStore(get(), get()) }
    single<SyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
    single { ComprobanteLocalWriter(get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<InstitutionalRepository> { InstitutionalRepositoryImpl(get()) }
    single<ProgramaRepository> { ProgramaRepositoryImpl(get(), get(), get()) }
    single<ServicioRepository> { ServicioRepositoryImpl(get(), get(), get()) }
    single<ComprobanteRepository> {
        ComprobanteRepositoryImpl(get(), get(), get(), get(), get(), get())
    }
    single<SyncRepository> { OfflineSyncRepository(get(), get(), get(), get(), get(), get()) }
}
