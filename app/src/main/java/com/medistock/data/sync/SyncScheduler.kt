package com.medistock.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.medistock.data.realtime.RealtimeSyncService
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.realtime.RealtimeSyncService
import com.medistock.util.NetworkStatus
import com.medistock.util.SupabasePreferences
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val PERIODIC_SYNC_WORK = "auto_sync_periodic"
    private const val IMMEDIATE_SYNC_WORK = "auto_sync_immediate"
    private const val TAG = "SyncScheduler"
    private const val SYNC_INTERVAL_SECONDS = 30L

    private var networkCallbackRegistered = false

    fun start(context: Context) {
        val appContext = context.applicationContext
        scheduleNext(appContext)
        updateSyncMode(appContext, NetworkStatus.isOnline(appContext))
        registerNetworkCallback(appContext)

        if (SupabaseClientProvider.isConfigured(appContext) && NetworkStatus.isOnline(appContext)) {
            triggerImmediate(appContext, "app-start")
        }
    }

    fun scheduleNext(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AutoSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            PERIODIC_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerImmediate(context: Context, reason: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AutoSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("reason" to reason))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun registerNetworkCallback(context: Context) {
        if (networkCallbackRegistered) {
            return
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateSyncMode(context, true)
                    if (SupabaseClientProvider.isConfigured(context)) {
                        SupabaseClientProvider.reinitialize(context)
                        triggerImmediate(context, "network-available")
                        com.medistock.data.realtime.RealtimeSyncService.start(context)
                    }
                }

                override fun onLost(network: Network) {
                    updateSyncMode(context, false)
                    com.medistock.data.realtime.RealtimeSyncService.stop()
                }
            }
        )


        networkCallbackRegistered = true
        Log.d(TAG, "Network callback registered for auto sync")
    }

    private fun updateSyncMode(context: Context, isOnline: Boolean) {
        val preferences = SupabasePreferences(context)
        val configured = SupabaseClientProvider.isConfigured(context)
        val mode = if (configured && isOnline && preferences.isRealtimeEnabled()) {
            SupabasePreferences.SyncMode.REALTIME
        } else {
            SupabasePreferences.SyncMode.LOCAL
        }
        preferences.setSyncMode(mode)
    }
}
