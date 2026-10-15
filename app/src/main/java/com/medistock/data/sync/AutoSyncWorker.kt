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
        // Always chain the next run, whatever happens below. Before, an early return
        // (no session yet, not configured) skipped scheduleNext() and the periodic
        // sync stopped until the next app start or network change.
        try {
            return runSync()
        } finally {
            SyncScheduler.scheduleNext(applicationContext)
        }
    }

    private suspend fun runSync(): Result {
        if (!SupabaseClientProvider.isConfigured(applicationContext)) {
            return Result.success()
        }

        if (!NetworkStatus.isOnline(applicationContext)) {
            return Result.success()
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

        return if (hasError) Result.retry() else Result.success()
    }
}
