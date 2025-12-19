package com.medistock.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.medistock.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to migrate plain text passwords to hashed passwords
 */
object PasswordMigration {

    private const val MIGRATION_KEY = "password_hashing_migration_completed"

    /**
     * Checks if the password migration has been completed
     */
    fun isMigrationCompleted(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(MIGRATION_KEY, false)
    }

    /**
     * Marks the migration as completed
     */
    private fun markMigrationCompleted(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(MIGRATION_KEY, true).apply()
    }

    /**
     * Migrates all plain text passwords to hashed passwords
     * This should be called once when the app starts
     */
    suspend fun migratePasswordsIfNeeded(context: Context) {
        if (isMigrationCompleted(context)) {
            return // Migration already completed
        }

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val users = db.userDao().getAllUsers()

            users.forEach { user ->
                // Check if password is already hashed
                if (!PasswordHasher.isHashed(user.password)) {
                    // Hash the plain text password
                    val hashedPassword = PasswordHasher.hashPassword(user.password)
                    val updatedUser = user.copy(
                        password = hashedPassword,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = "password_migration"
                    )
                    db.userDao().updateUser(updatedUser)
                }
            }

            // Mark migration as completed
            markMigrationCompleted(context)
        }
    }
}
