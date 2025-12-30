package com.medistock.data.realtime

import android.content.Context
import android.util.Log
import com.medistock.data.db.AppDatabase
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.dto.CategoryDto
import com.medistock.data.remote.dto.CustomerDto
import com.medistock.data.remote.dto.PackagingTypeDto
import com.medistock.data.remote.dto.ProductDto
import com.medistock.data.remote.dto.SiteDto
import com.medistock.data.sync.SyncMapper.toEntity
import com.medistock.util.SupabasePreferences
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object RealtimeSyncService {

    private const val TAG = "RealtimeSyncService"

    private val channels = mutableListOf<RealtimeChannel>()
    private var scope: CoroutineScope? = null
    private var isRunning = false

    fun start(context: Context) {
        val appContext = context.applicationContext
        val preferences = SupabasePreferences(appContext)

        if (!preferences.isRealtimeEnabled()) {
            Log.d(TAG, "Realtime sync disabled in preferences")
            return
        }

        if (isRunning) {
            Log.d(TAG, "Realtime sync already running")
            return
        }

        if (!SupabaseClientProvider.isConfigured(appContext)) {
            Log.d(TAG, "Supabase not configured, skipping realtime sync")
            return
        }

        val realtime = runCatching { SupabaseClientProvider.client.realtime }.getOrElse {
            Log.e(TAG, "Supabase client not available: ${it.message}", it)
            return
        }

        val database = AppDatabase.getInstance(appContext)
        channels.clear()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        isRunning = true

        subscribeToTable(realtime, "sites") { action ->
            handleSiteChange(action, database)
        }
        subscribeToTable(realtime, "packaging_types") { action ->
            handlePackagingTypeChange(action, database)
        }
        subscribeToTable(realtime, "categories") { action ->
            handleCategoryChange(action, database)
        }
        subscribeToTable(realtime, "products") { action ->
            handleProductChange(action, database)
        }
        subscribeToTable(realtime, "customers") { action ->
            handleCustomerChange(action, database)
        }
    }

    fun stop() {
        if (!isRunning && channels.isEmpty()) {
            return
        }

        isRunning = false
        scope?.cancel()
        scope = null

        val realtime = runCatching { SupabaseClientProvider.client.realtime }.getOrNull()
        if (realtime != null && channels.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                channels.forEach { channel ->
                    runCatching { realtime.removeChannel(channel) }.onFailure {
                        Log.e(TAG, "Error removing realtime channel ${channel.topic}", it)
                    }
                }
            }
        }
        channels.clear()
    }

    private fun subscribeToTable(
        realtime: Realtime,
        table: String,
        handler: suspend (PostgresAction) -> Unit
    ) {
        val currentScope = scope ?: return
        val channel = realtime.channel("realtime-$table")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
        }

        channels.add(channel)

        currentScope.launch {
            flow.collect { action ->
                try {
                    handler(action)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling realtime change for $table", e)
                }
            }
        }

        currentScope.launch {
            try {
                channel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to realtime changes for $table", e)
            }
        }
    }

    private fun handleSiteChange(action: PostgresAction, database: AppDatabase) {
        val dao = database.siteDao()
        when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<SiteDto>()?.let { dto ->
                runSafely("sites insert") { dao.insert(dto.toEntity()) }
            }

            is PostgresAction.Update -> action.decodeRecordOrNull<SiteDto>()?.let { dto ->
                runSafely("sites update") { dao.update(dto.toEntity()) }
            }

            is PostgresAction.Delete -> action.decodeOldRecordOrNull<SiteDto>()?.let { dto ->
                runSafely("sites delete") { dao.delete(dto.toEntity()) }
            }

            else -> Unit
        }
    }

    private fun handlePackagingTypeChange(action: PostgresAction, database: AppDatabase) {
        val dao = database.packagingTypeDao()
        when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<PackagingTypeDto>()?.let { dto ->
                runSafely("packaging_types insert") { dao.insert(dto.toEntity()) }
            }

            is PostgresAction.Update -> action.decodeRecordOrNull<PackagingTypeDto>()?.let { dto ->
                runSafely("packaging_types update") { dao.update(dto.toEntity()) }
            }

            is PostgresAction.Delete -> action.decodeOldRecordOrNull<PackagingTypeDto>()?.let { dto ->
                runSafely("packaging_types delete") { dao.delete(dto.toEntity()) }
            }

            else -> Unit
        }
    }

    private fun handleCategoryChange(action: PostgresAction, database: AppDatabase) {
        val dao = database.categoryDao()
        when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<CategoryDto>()?.let { dto ->
                runSafely("categories insert") { dao.insert(dto.toEntity()) }
            }

            is PostgresAction.Update -> action.decodeRecordOrNull<CategoryDto>()?.let { dto ->
                runSafely("categories update") { dao.update(dto.toEntity()) }
            }

            is PostgresAction.Delete -> action.decodeOldRecordOrNull<CategoryDto>()?.let { dto ->
                runSafely("categories delete") { dao.delete(dto.toEntity()) }
            }

            else -> Unit
        }
    }

    private fun handleProductChange(action: PostgresAction, database: AppDatabase) {
        val dao = database.productDao()
        when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<ProductDto>()?.let { dto ->
                runSafely("products insert") { dao.insert(dto.toEntity()) }
            }

            is PostgresAction.Update -> action.decodeRecordOrNull<ProductDto>()?.let { dto ->
                runSafely("products update") { dao.update(dto.toEntity()) }
            }

            is PostgresAction.Delete -> action.decodeOldRecordOrNull<ProductDto>()?.let { dto ->
                runSafely("products delete") { dao.delete(dto.toEntity()) }
            }

            else -> Unit
        }
    }

    private fun handleCustomerChange(action: PostgresAction, database: AppDatabase) {
        val dao = database.customerDao()
        when (action) {
            is PostgresAction.Insert -> action.decodeRecordOrNull<CustomerDto>()?.let { dto ->
                runSafely("customers insert") { dao.insert(dto.toEntity()) }
            }

            is PostgresAction.Update -> action.decodeRecordOrNull<CustomerDto>()?.let { dto ->
                runSafely("customers update") { dao.update(dto.toEntity()) }
            }

            is PostgresAction.Delete -> action.decodeOldRecordOrNull<CustomerDto>()?.let { dto ->
                runSafely("customers delete") { dao.delete(dto.toEntity()) }
            }

            else -> Unit
        }
    }

    private inline fun runSafely(operation: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Error during realtime $operation", e)
        }
    }
}
