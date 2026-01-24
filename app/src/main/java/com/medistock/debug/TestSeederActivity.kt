package com.medistock.debug

import android.app.Activity
import android.os.Bundle
import com.medistock.MedistockApplication
import com.medistock.util.TestUserSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only Activity for seeding/cleaning up test users via intents.
 *
 * Usage from Maestro:
 * - runScript: adb shell am start -n com.medistock/.debug.TestSeederActivity --es action seed
 * - runScript: adb shell am start -n com.medistock/.debug.TestSeederActivity --es action cleanup
 *
 * This Activity finishes immediately after launching the seed/cleanup operation.
 */
class TestSeederActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra("action") ?: "seed"
        val sdk = MedistockApplication.sdk

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "seed" -> {
                        val count = TestUserSeeder.seedTestUsers(sdk)
                        println("TestSeederActivity: Seeded $count test users")
                    }
                    "cleanup" -> {
                        val count = TestUserSeeder.removeTestUsers(sdk)
                        println("TestSeederActivity: Removed $count test users")
                    }
                    else -> {
                        println("TestSeederActivity: Unknown action '$action'")
                    }
                }
            } catch (e: Exception) {
                println("TestSeederActivity: Error - ${e.message}")
                e.printStackTrace()
            }
        }

        // Finish immediately - the coroutine runs in background
        finish()
    }
}
