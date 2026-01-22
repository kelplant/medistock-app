package com.medistock.shared

import com.medistock.shared.data.repository.*
import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.auth.AuthService
import com.medistock.shared.domain.auth.PasswordVerifier
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.permission.PermissionService
import com.medistock.shared.domain.sync.ConflictResolver
import com.medistock.shared.domain.sync.RetryConfiguration
import com.medistock.shared.domain.sync.SyncOrchestrator
import com.medistock.shared.domain.usecase.*
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Main entry point for the MediStock shared SDK
 * This class provides access to all repositories, UseCases, and shared business logic
 */
class MedistockSDK(driverFactory: DatabaseDriverFactory) {

    private val database: MedistockDatabase = MedistockDatabase(driverFactory.createDriver())

    // Repositories
    val siteRepository: SiteRepository by lazy { SiteRepository(database) }
    val productRepository: ProductRepository by lazy { ProductRepository(database) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database) }
    val userRepository: UserRepository by lazy { UserRepository(database) }
    val purchaseBatchRepository: PurchaseBatchRepository by lazy { PurchaseBatchRepository(database) }
    val saleRepository: SaleRepository by lazy { SaleRepository(database) }
    val customerRepository: CustomerRepository by lazy { CustomerRepository(database) }
    val stockMovementRepository: StockMovementRepository by lazy { StockMovementRepository(database) }
    val productTransferRepository: ProductTransferRepository by lazy { ProductTransferRepository(database) }
    val packagingTypeRepository: PackagingTypeRepository by lazy { PackagingTypeRepository(database) }
    val inventoryRepository: InventoryRepository by lazy { InventoryRepository(database) }
    val inventoryItemRepository: InventoryItemRepository by lazy { InventoryItemRepository(database) }
    val auditRepository: AuditRepository by lazy { AuditRepository(database) }
    val stockRepository: StockRepository by lazy { StockRepository(database) }
    val saleBatchAllocationRepository: SaleBatchAllocationRepository by lazy { SaleBatchAllocationRepository(database) }
    val userPermissionRepository: UserPermissionRepository by lazy { UserPermissionRepository(database) }
    val productPriceRepository: ProductPriceRepository by lazy { ProductPriceRepository(database) }

    // Services - Shared business services
    val permissionService: PermissionService by lazy { PermissionService(userPermissionRepository) }
    val syncOrchestrator: SyncOrchestrator by lazy { SyncOrchestrator() }
    val conflictResolver: ConflictResolver by lazy { ConflictResolver() }
    val retryConfiguration: RetryConfiguration by lazy { RetryConfiguration.DEFAULT }

    /**
     * Create an AuthService with a platform-specific PasswordVerifier.
     * Call this method once at app startup with your platform's BCrypt implementation.
     *
     * Example (Android):
     * ```
     * val authService = sdk.createAuthService(object : PasswordVerifier {
     *     override fun verify(plain: String, hashed: String) = BCrypt.verify(plain, hashed)
     * })
     * ```
     */
    fun createAuthService(passwordVerifier: PasswordVerifier): AuthService {
        return AuthService(userRepository, passwordVerifier)
    }

    // UseCases - Business logic layer
    val purchaseUseCase: PurchaseUseCase by lazy {
        PurchaseUseCase(
            purchaseBatchRepository = purchaseBatchRepository,
            stockMovementRepository = stockMovementRepository,
            productRepository = productRepository,
            siteRepository = siteRepository,
            auditRepository = auditRepository
        )
    }

    val saleUseCase: SaleUseCase by lazy {
        SaleUseCase(
            saleRepository = saleRepository,
            purchaseBatchRepository = purchaseBatchRepository,
            stockMovementRepository = stockMovementRepository,
            productRepository = productRepository,
            siteRepository = siteRepository,
            auditRepository = auditRepository,
            saleBatchAllocationRepository = saleBatchAllocationRepository
        )
    }

    val transferUseCase: TransferUseCase by lazy {
        TransferUseCase(
            productTransferRepository = productTransferRepository,
            purchaseBatchRepository = purchaseBatchRepository,
            stockMovementRepository = stockMovementRepository,
            productRepository = productRepository,
            siteRepository = siteRepository,
            auditRepository = auditRepository
        )
    }

    val inventoryUseCase: InventoryUseCase by lazy {
        InventoryUseCase(
            inventoryRepository = inventoryRepository,
            purchaseBatchRepository = purchaseBatchRepository,
            stockMovementRepository = stockMovementRepository,
            productRepository = productRepository,
            siteRepository = siteRepository,
            auditRepository = auditRepository,
            stockRepository = stockRepository
        )
    }

    // Platform info
    val platformName: String = getPlatform().name

    // Factory methods
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
        categoryId: String? = null,
        userId: String = "ios"
    ): Product {
        val now = Clock.System.now().toEpochMilliseconds()
        return Product(
            id = generateId(prefix = "product"),
            name = name,
            unit = unit,
            unitVolume = unitVolume,
            siteId = siteId,
            categoryId = categoryId,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createCategory(name: String, userId: String = "ios"): Category {
        val now = Clock.System.now().toEpochMilliseconds()
        return Category(
            id = generateId(prefix = "category"),
            name = name,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createUser(
        username: String,
        password: String,
        fullName: String,
        isAdmin: Boolean = false,
        userId: String = "ios"
    ): User {
        val now = Clock.System.now().toEpochMilliseconds()
        return User(
            id = generateId(prefix = "user"),
            username = username,
            password = password,
            fullName = fullName,
            isAdmin = isAdmin,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createCustomer(
        name: String,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        notes: String? = null,
        siteId: String? = null,
        userId: String = "ios"
    ): Customer {
        val now = Clock.System.now().toEpochMilliseconds()
        return Customer(
            id = generateId(prefix = "customer"),
            name = name,
            phone = phone,
            email = email,
            address = address,
            notes = notes,
            siteId = siteId,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createPurchaseBatch(
        productId: String,
        siteId: String,
        quantity: Double,
        purchasePrice: Double,
        supplierName: String = "",
        batchNumber: String? = null,
        expiryDate: Long? = null,
        userId: String = "ios"
    ): PurchaseBatch {
        val now = Clock.System.now().toEpochMilliseconds()
        return PurchaseBatch(
            id = generateId(prefix = "batch"),
            productId = productId,
            siteId = siteId,
            batchNumber = batchNumber,
            purchaseDate = now,
            initialQuantity = quantity,
            remainingQuantity = quantity,
            purchasePrice = purchasePrice,
            supplierName = supplierName,
            expiryDate = expiryDate,
            isExhausted = false,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createSale(
        customerName: String,
        siteId: String,
        totalAmount: Double,
        customerId: String? = null,
        userId: String = "ios"
    ): Sale {
        val now = Clock.System.now().toEpochMilliseconds()
        return Sale(
            id = generateId(prefix = "sale"),
            customerName = customerName,
            customerId = customerId,
            date = now,
            totalAmount = totalAmount,
            siteId = siteId,
            createdAt = now,
            createdBy = userId
        )
    }

    fun createSaleItem(
        saleId: String,
        productId: String,
        productName: String = "",
        unit: String = "",
        quantity: Double,
        unitPrice: Double,
        userId: String = "ios"
    ): SaleItem {
        val now = Clock.System.now().toEpochMilliseconds()
        return SaleItem(
            id = generateId(prefix = "saleitem"),
            saleId = saleId,
            productId = productId,
            productName = productName,
            unit = unit,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = quantity * unitPrice,
            createdAt = now,
            createdBy = userId
        )
    }

    fun createProductTransfer(
        productId: String,
        fromSiteId: String,
        toSiteId: String,
        quantity: Double,
        notes: String? = null,
        userId: String = "ios"
    ): ProductTransfer {
        val now = Clock.System.now().toEpochMilliseconds()
        return ProductTransfer(
            id = generateId(prefix = "transfer"),
            productId = productId,
            fromSiteId = fromSiteId,
            toSiteId = toSiteId,
            quantity = quantity,
            status = "pending",
            notes = notes,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createStockMovement(
        productId: String,
        siteId: String,
        quantity: Double,
        movementType: String,
        purchasePriceAtMovement: Double = 0.0,
        sellingPriceAtMovement: Double = 0.0,
        referenceId: String? = null,
        notes: String? = null,
        userId: String = "ios"
    ): StockMovement {
        val now = Clock.System.now().toEpochMilliseconds()
        return StockMovement(
            id = generateId(prefix = "movement"),
            productId = productId,
            siteId = siteId,
            quantity = quantity,
            type = movementType,
            date = now,
            purchasePriceAtMovement = purchasePriceAtMovement,
            sellingPriceAtMovement = sellingPriceAtMovement,
            movementType = movementType,
            referenceId = referenceId,
            notes = notes,
            createdAt = now,
            createdBy = userId
        )
    }

    fun createPackagingType(
        name: String,
        level1Name: String,
        level2Name: String? = null,
        level2Quantity: Int? = null,
        defaultConversionFactor: Double? = null,
        isActive: Boolean = true,
        displayOrder: Int = 0,
        userId: String = "ios"
    ): PackagingType {
        val now = Clock.System.now().toEpochMilliseconds()
        return PackagingType(
            id = generateId(prefix = "packaging"),
            name = name,
            level1Name = level1Name,
            level2Name = level2Name,
            level2Quantity = level2Quantity,
            defaultConversionFactor = defaultConversionFactor,
            isActive = isActive,
            displayOrder = displayOrder,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
    }

    fun createInventory(
        siteId: String,
        notes: String? = null,
        userId: String = "ios"
    ): Inventory {
        val now = Clock.System.now().toEpochMilliseconds()
        return Inventory(
            id = generateId(prefix = "inventory"),
            siteId = siteId,
            status = "in_progress",
            startedAt = now,
            completedAt = null,
            notes = notes,
            createdBy = userId
        )
    }

    fun createAuditEntry(
        tableName: String,
        recordId: String,
        action: String,
        oldValues: String? = null,
        newValues: String? = null,
        userId: String
    ): AuditEntry {
        val now = Clock.System.now().toEpochMilliseconds()
        return AuditEntry(
            id = generateId(prefix = "audit"),
            tableName = tableName,
            recordId = recordId,
            action = action,
            oldValues = oldValues,
            newValues = newValues,
            userId = userId,
            timestamp = now
        )
    }

    fun createUserPermission(
        userId: String,
        module: Module,
        canView: Boolean = false,
        canCreate: Boolean = false,
        canEdit: Boolean = false,
        canDelete: Boolean = false,
        createdBy: String = "system"
    ): UserPermission {
        val now = Clock.System.now().toEpochMilliseconds()
        return UserPermission(
            id = generateId(prefix = "permission"),
            userId = userId,
            module = module.name,
            canView = canView,
            canCreate = canCreate,
            canEdit = canEdit,
            canDelete = canDelete,
            createdAt = now,
            updatedAt = now,
            createdBy = createdBy,
            updatedBy = createdBy
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
