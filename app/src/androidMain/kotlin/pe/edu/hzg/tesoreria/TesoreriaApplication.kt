package pe.edu.hzg.tesoreria

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pe.edu.hzg.tesoreria.di.appModule

class TesoreriaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TesoreriaApplication)
            modules(appModule)
        }
    }
}
