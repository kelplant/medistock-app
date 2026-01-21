package com.medistock.shared

import com.medistock.shared.data.repository.ProductRepository
import com.medistock.shared.data.repository.SiteRepository
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.Site
import kotlinx.datetime.Clock
import kotlin.random.Random

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

    fun createSite(name: String, userId: String = "ios"): Site {
        val now = Clock.System.now().toEpochMilliseconds()
        return Site(
            id = generateId(prefix = "site"),
            name = name,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createProduct(
        name: String,
        siteId: String,
        unit: String = "unité",
        unitVolume: Double = 1.0,
        userId: String = "ios"
    ): Product {
        val now = Clock.System.now().toEpochMilliseconds()
        return Product(
            id = generateId(prefix = "product"),
            name = name,
            unit = unit,
            unitVolume = unitVolume,
            siteId = siteId,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    companion object {
        const val VERSION = "1.0.0"
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
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
