package pe.edu.hzg.tesoreria.data.sync

import pe.edu.hzg.tesoreria.domain.repository.SyncScheduler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "tesoreria-offline-sync"
    }
}
