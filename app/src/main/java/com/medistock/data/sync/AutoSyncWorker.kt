package com.medistock.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.util.NetworkStatus

class AutoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!SupabaseClientProvider.isConfigured(applicationContext)) {
            return Result.success()
        }

        if (!NetworkStatus.isOnline(applicationContext)) {
            return Result.retry()
        }

        SupabaseClientProvider.reinitialize(applicationContext)

        val syncManager = SyncManager(applicationContext)
        var hasError = false
        syncManager.fullSync(
            onError = { _, _ ->
                hasError = true
            }
        )

        return if (hasError) Result.retry() else Result.success()
    }
}
