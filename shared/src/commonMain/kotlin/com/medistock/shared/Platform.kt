package com.medistock.shared

/**
 * Platform-specific interface for KMM
 * Each platform provides its own implementation
 */
interface Platform {
    val name: String
}

/**
 * Expected function to get platform-specific implementation
 */
expect fun getPlatform(): Platform

/**
 * Database driver factory - each platform provides its own SQLDelight driver
 */
expect class DatabaseDriverFactory {
    fun createDriver(): app.cash.sqldelight.db.SqlDriver
}
