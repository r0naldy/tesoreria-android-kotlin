package pe.edu.hzg.tesoreria.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.edu.hzg.tesoreria.domain.repository.SyncRepository

class SyncWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters), KoinComponent {
    private val syncRepository: SyncRepository by inject()

    override suspend fun doWork(): Result {
        return syncRepository.synchronize().fold(
            onSuccess = { summary ->
                val output = Data.Builder()
                    .putInt("processed", summary.processed)
                    .putInt("succeeded", summary.succeeded)
                    .putInt("failed", summary.failed)
                    .build()
                if (summary.failed > 0) Result.retry() else Result.success(output)
            },
            onFailure = { Result.retry() },
        )
    }
}
