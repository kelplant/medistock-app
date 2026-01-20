package com.medistock.shared

import com.medistock.shared.data.repository.ProductRepository
import com.medistock.shared.data.repository.SiteRepository
import com.medistock.shared.db.MedistockDatabase

/**
 * Main entry point for the MediStock shared SDK
 * This class provides access to all repositories and shared business logic
 */
class MedistockSDK(driverFactory: DatabaseDriverFactory) {

    private val database: MedistockDatabase = MedistockDatabase(driverFactory.createDriver())

    // Repositories
    val siteRepository: SiteRepository by lazy { SiteRepository(database) }
    val productRepository: ProductRepository by lazy { ProductRepository(database) }

    // Platform info
    val platformName: String = getPlatform().name

    companion object {
        const val VERSION = "1.0.0"
    }
}

/**
 * Greeting function for testing the SDK
 */
class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        return "Hello, MediStock on ${platform.name}!"
    }
}
