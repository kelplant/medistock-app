package com.medistock.data.sync

import com.medistock.data.entities.*
import com.medistock.shared.data.dto.CategoryDto
import com.medistock.shared.data.dto.CustomerDto
import com.medistock.shared.data.dto.PackagingTypeDto
import com.medistock.shared.data.dto.ProductDto
import com.medistock.shared.data.dto.ProductTransferDto
import com.medistock.shared.data.dto.PurchaseBatchDto
import com.medistock.shared.data.dto.SaleDto
import com.medistock.shared.data.dto.SaleItemDto
import com.medistock.shared.data.dto.SaleBatchAllocationDto
import com.medistock.shared.data.dto.SiteDto
import com.medistock.shared.data.dto.StockMovementDto
import com.medistock.shared.data.dto.UserDto
import com.medistock.shared.data.dto.UserPermissionDto

// Typealias for backward compatibility
typealias AppUserDto = UserDto

/**
 * Mapper pour convertir entre les entités Room (local) et les DTOs Supabase (remote)
 */
object SyncMapper {

    // ==================== Product ====================

    fun Product.toDto(): ProductDto = ProductDto(
        id = id,
        name = name,
        unit = unit,
        unitVolume = unitVolume,
        packagingTypeId = packagingTypeId,
        selectedLevel = selectedLevel,
        conversionFactor = conversionFactor,
        categoryId = categoryId,
        marginType = marginType,
        marginValue = marginValue,
        description = description,
        siteId = siteId,
        minStock = minStock,
        maxStock = maxStock,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun ProductDto.toEntity(): Product = Product(
        id = id,
        name = name,
        unit = unit,
        unitVolume = unitVolume,
        packagingTypeId = packagingTypeId,
        selectedLevel = selectedLevel,
        conversionFactor = conversionFactor,
        categoryId = categoryId,
        marginType = marginType,
        marginValue = marginValue,
        description = description,
        siteId = siteId,
        minStock = minStock,
        maxStock = maxStock,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== Category ====================

    fun Category.toDto(): CategoryDto = CategoryDto(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun CategoryDto.toEntity(): Category = Category(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== Site ====================

    fun Site.toDto(): SiteDto = SiteDto(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun SiteDto.toEntity(): Site = Site(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== Customer ====================

    fun Customer.toDto(): CustomerDto = CustomerDto(
        id = id,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        siteId = siteId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun CustomerDto.toEntity(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        siteId = siteId ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== PackagingType ====================

    fun PackagingType.toDto(): PackagingTypeDto = PackagingTypeDto(
        id = id,
        name = name,
        level1Name = level1Name,
        level2Name = level2Name,
        defaultConversionFactor = defaultConversionFactor,
        isActive = isActive,
        displayOrder = displayOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun PackagingTypeDto.toEntity(): PackagingType = PackagingType(
        id = id,
        name = name,
        level1Name = level1Name,
        level2Name = level2Name,
        defaultConversionFactor = defaultConversionFactor,
        isActive = isActive,
        displayOrder = displayOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== User ====================

    fun User.toDto(): AppUserDto = AppUserDto(
        id = id,
        username = username,
        password = password,
        fullName = fullName,
        isAdmin = isAdmin,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun AppUserDto.toEntity(): User = User(
        id = id,
        username = username,
        password = password,
        fullName = fullName,
        isAdmin = isAdmin,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== UserPermission ====================

    fun UserPermission.toDto(): UserPermissionDto = UserPermissionDto(
        id = id,
        userId = userId,
        module = module,
        canView = canView,
        canCreate = canCreate,
        canEdit = canEdit,
        canDelete = canDelete,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun UserPermissionDto.toEntity(): UserPermission = UserPermission(
        id = id,
        userId = userId,
        module = module,
        canView = canView,
        canCreate = canCreate,
        canEdit = canEdit,
        canDelete = canDelete,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== Sale ====================

    fun Sale.toDto(): SaleDto = SaleDto(
        id = id,
        customerName = customerName,
        customerId = customerId,
        date = date,
        totalAmount = totalAmount,
        siteId = siteId,
        createdAt = createdAt,
        createdBy = createdBy
    )

    fun SaleDto.toEntity(): Sale = Sale(
        id = id,
        customerName = customerName,
        customerId = customerId,
        date = date,
        totalAmount = totalAmount,
        siteId = siteId,
        createdAt = createdAt,
        createdBy = createdBy
    )

    // ==================== SaleItem ====================

    fun SaleItem.toDto(): SaleItemDto = SaleItemDto(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName,
        unit = unit,
        quantity = quantity,
        unitPrice = pricePerUnit,
        totalPrice = subtotal,
        createdAt = createdAt,
        createdBy = createdBy
    )

    fun SaleItemDto.toEntity(): SaleItem = SaleItem(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName ?: "",
        unit = unit ?: "",
        quantity = quantity,
        pricePerUnit = unitPrice,
        subtotal = totalPrice,
        createdAt = createdAt ?: System.currentTimeMillis(),
        createdBy = createdBy ?: ""
    )

    // ==================== PurchaseBatch ====================

    fun PurchaseBatch.toDto(): PurchaseBatchDto = PurchaseBatchDto(
        id = id,
        productId = productId,
        siteId = siteId,
        batchNumber = batchNumber,
        purchaseDate = purchaseDate,
        initialQuantity = initialQuantity,
        remainingQuantity = remainingQuantity,
        purchasePrice = purchasePrice,
        supplierName = supplierName,
        expiryDate = expiryDate,
        isExhausted = isExhausted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    fun PurchaseBatchDto.toEntity(): PurchaseBatch = PurchaseBatch(
        id = id,
        productId = productId,
        siteId = siteId,
        batchNumber = batchNumber,
        purchaseDate = purchaseDate,
        initialQuantity = initialQuantity,
        remainingQuantity = remainingQuantity,
        purchasePrice = purchasePrice,
        supplierName = supplierName,
        expiryDate = expiryDate,
        isExhausted = isExhausted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    // ==================== StockMovement ====================

    fun StockMovement.toDto(): StockMovementDto = StockMovementDto(
        id = id,
        productId = productId,
        siteId = siteId,
        quantity = quantity,
        movementType = type,
        purchasePriceAtMovement = purchasePriceAtMovement,
        sellingPriceAtMovement = sellingPriceAtMovement,
        date = date,
        createdAt = createdAt,
        createdBy = createdBy
    )

    fun StockMovementDto.toEntity(): StockMovement = StockMovement(
        id = id,
        productId = productId,
        siteId = siteId,
        quantity = quantity,
        type = movementType,
        purchasePriceAtMovement = purchasePriceAtMovement ?: 0.0,
        sellingPriceAtMovement = sellingPriceAtMovement ?: 0.0,
        date = date ?: createdAt,
        createdAt = createdAt,
        createdBy = createdBy
    )
}
