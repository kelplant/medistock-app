package com.medistock.data.sync

import com.medistock.data.entities.*
import com.medistock.data.remote.dto.*

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
        createdBy = createdBy
    )

    fun CustomerDto.toEntity(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        siteId = siteId,
        createdAt = createdAt,
        createdBy = createdBy
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
}
