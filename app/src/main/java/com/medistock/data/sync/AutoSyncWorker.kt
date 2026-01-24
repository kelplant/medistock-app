package com.medistock.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medistock.data.remote.SupabaseAuthService
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.util.AuthManager
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

        // Initialize client if not already (don't reinitialize - that would lose the session)
        if (!SupabaseClientProvider.isInitialized()) {
            SupabaseClientProvider.initialize(applicationContext)
        }

        // Restore Supabase Auth session from stored tokens before syncing
        // This ensures RLS policies work properly
        val authManager = AuthManager.getInstance(applicationContext)
        val authService = SupabaseAuthService()
        val sessionRestored = authService.restoreSessionIfNeeded(authManager)

        if (!sessionRestored) {
            // No session is a normal state (user not logged in), not an error
            // Return success to avoid infinite retries
            return Result.success()
        }

        val syncManager = SyncManager(applicationContext)
        var hasError = false
        syncManager.fullSync(
            onError = { _, _ ->
                hasError = true
            }
        )

        SyncScheduler.scheduleNext(applicationContext)

        return if (hasError) Result.retry() else Result.success()
    }
}
