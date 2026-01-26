import Foundation
import shared

// MARK: - DTOs for Supabase JSON encoding/decoding
//
// These Swift DTOs mirror the Kotlin DTOs in shared/src/commonMain/kotlin/com/medistock/shared/data/dto/
// They exist because:
// 1. Swift's Codable is required for Supabase JSON encoding on iOS
// 2. Kotlin's kotlinx.serialization doesn't automatically provide Swift Codable conformance
// 3. KMP interop doesn't support Swift Codable protocol conformance on Kotlin types
//
// AUTHORITATIVE SOURCE: The Kotlin DTOs in shared module are the source of truth.
// When updating field mappings or adding new DTOs:
// 1. First update the Kotlin DTOs in shared/src/commonMain/kotlin/com/medistock/shared/data/dto/
// 2. Then update these Swift DTOs to maintain consistency
//
// Available shared DTOs (Kotlin):
// - SiteDto, CategoryDto, ProductDto, CustomerDto, UserDto, UserPermissionDto
// - SaleDto, SaleItemDto, SaleBatchAllocationDto
// - PurchaseBatchDto, StockMovementDto, ProductTransferDto
// - InventoryDto, PackagingTypeDto
// - ProductPriceDto, CurrentStockDto, AuditHistoryDto (added in Phase 8)
//
// Note: The shared module's repositories, services, and business logic ARE used directly
// from iOS. Only the JSON encoding layer uses these Swift-specific DTOs.

struct SiteDTO: Codable {
    let id: String
    let name: String
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from site: Site) {
        self.id = site.id
        self.name = site.name
        self.isActive = site.isActive
        self.createdAt = site.createdAt
        self.updatedAt = site.updatedAt
        self.createdBy = site.createdBy
        self.updatedBy = site.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> Site {
        Site(
            id: id,
            name: name,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct CategoryDTO: Codable {
    let id: String
    let name: String
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from category: shared.Category) {
        self.id = category.id
        self.name = category.name
        self.isActive = category.isActive
        self.createdAt = category.createdAt
        self.updatedAt = category.updatedAt
        self.createdBy = category.createdBy
        self.updatedBy = category.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> shared.Category {
        shared.Category(
            id: id,
            name: name,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct PackagingTypeDTO: Codable {
    let id: String
    let name: String
    let level1Name: String
    let level2Name: String?
    let level2Quantity: Int32?
    let defaultConversionFactor: Double?
    let isActive: Bool
    let displayOrder: Int32
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name
        case level1Name = "level1_name"
        case level2Name = "level2_name"
        case level2Quantity = "level2_quantity"
        case defaultConversionFactor = "default_conversion_factor"
        case isActive = "is_active"
        case displayOrder = "display_order"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from packagingType: PackagingType) {
        self.id = packagingType.id
        self.name = packagingType.name
        self.level1Name = packagingType.level1Name
        self.level2Name = packagingType.level2Name
        self.level2Quantity = packagingType.level2Quantity?.int32Value
        self.defaultConversionFactor = packagingType.defaultConversionFactor?.doubleValue
        self.isActive = packagingType.isActive
        self.displayOrder = packagingType.displayOrder
        self.createdAt = packagingType.createdAt
        self.updatedAt = packagingType.updatedAt
        self.createdBy = packagingType.createdBy
        self.updatedBy = packagingType.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> PackagingType {
        PackagingType(
            id: id,
            name: name,
            level1Name: level1Name,
            level2Name: level2Name,
            level2Quantity: level2Quantity.map { KotlinInt(int: $0) },
            defaultConversionFactor: defaultConversionFactor.map { KotlinDouble(double: $0) },
            isActive: isActive,
            displayOrder: displayOrder,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct ProductDTO: Codable {
    let id: String
    let name: String
    let description: String?
    let unitVolume: Double
    let packagingTypeId: String
    let selectedLevel: Int32
    let conversionFactor: Double
    let categoryId: String?
    let marginType: String
    let marginValue: Double
    let siteId: String?
    let minStock: Double?
    let maxStock: Double?
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name, description
        case unitVolume = "unit_volume"
        case packagingTypeId = "packaging_type_id"
        case selectedLevel = "selected_level"
        case conversionFactor = "conversion_factor"
        case categoryId = "category_id"
        case marginType = "margin_type"
        case marginValue = "margin_value"
        case siteId = "site_id"
        case minStock = "min_stock"
        case maxStock = "max_stock"
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from product: Product) {
        self.id = product.id
        self.name = product.name
        self.description = product.description_
        self.unitVolume = product.unitVolume
        self.packagingTypeId = product.packagingTypeId ?? ""
        self.selectedLevel = product.selectedLevel
        self.conversionFactor = product.conversionFactor?.doubleValue ?? 1.0
        self.categoryId = product.categoryId
        self.marginType = product.marginType ?? "PERCENTAGE"
        self.marginValue = product.marginValue?.doubleValue ?? 0.0
        self.siteId = product.siteId
        self.minStock = product.minStock?.doubleValue
        self.maxStock = product.maxStock?.doubleValue
        self.isActive = product.isActive
        self.createdAt = product.createdAt
        self.updatedAt = product.updatedAt
        self.createdBy = product.createdBy
        self.updatedBy = product.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> Product {
        Product(
            id: id,
            name: name,
            unitVolume: unitVolume,
            packagingTypeId: packagingTypeId,
            selectedLevel: selectedLevel,
            conversionFactor: KotlinDouble(double: conversionFactor),
            categoryId: categoryId,
            marginType: marginType,
            marginValue: KotlinDouble(double: marginValue),
            description: description,
            siteId: siteId ?? "",
            minStock: minStock.map { KotlinDouble(double: $0) },
            maxStock: maxStock.map { KotlinDouble(double: $0) },
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct CustomerDTO: Codable {
    let id: String
    let name: String
    let phone: String?
    let email: String?
    let address: String?
    let notes: String?
    let siteId: String?
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name, phone, email, address, notes
        case siteId = "site_id"
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from customer: Customer) {
        self.id = customer.id
        self.name = customer.name
        self.phone = customer.phone
        self.email = customer.email
        self.address = customer.address
        self.notes = customer.notes
        self.siteId = customer.siteId
        self.isActive = customer.isActive
        self.createdAt = customer.createdAt
        self.updatedAt = customer.updatedAt
        self.createdBy = customer.createdBy
        self.updatedBy = customer.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> Customer {
        Customer(
            id: id,
            name: name,
            phone: phone,
            email: email,
            address: address,
            notes: notes,
            siteId: siteId,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct SupplierDTO: Codable {
    let id: String
    let name: String
    let phone: String?
    let email: String?
    let address: String?
    let notes: String?
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, name, phone, email, address, notes
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from supplier: Supplier) {
        self.id = supplier.id
        self.name = supplier.name
        self.phone = supplier.phone
        self.email = supplier.email
        self.address = supplier.address
        self.notes = supplier.notes
        self.isActive = supplier.isActive
        self.createdAt = supplier.createdAt
        self.updatedAt = supplier.updatedAt
        self.createdBy = supplier.createdBy
        self.updatedBy = supplier.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> Supplier {
        Supplier(
            id: id,
            name: name,
            phone: phone,
            email: email,
            address: address,
            notes: notes,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct UserDTO: Codable {
    let id: String
    let username: String
    let password: String
    let fullName: String
    let language: String?
    let isAdmin: Bool
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id, username, password, language
        case fullName = "full_name"
        case isAdmin = "is_admin"
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    /// Memberwise initializer for creating UserDTO directly
    init(
        id: String,
        username: String,
        password: String,
        fullName: String,
        language: String?,
        isAdmin: Bool,
        isActive: Bool,
        createdAt: Int64,
        updatedAt: Int64,
        createdBy: String,
        updatedBy: String,
        clientId: String? = SyncClientId.current
    ) {
        self.id = id
        self.username = username
        self.password = password
        self.fullName = fullName
        self.language = language
        self.isAdmin = isAdmin
        self.isActive = isActive
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.createdBy = createdBy
        self.updatedBy = updatedBy
        self.clientId = clientId
    }

    init(from user: User) {
        self.id = user.id
        self.username = user.username
        self.password = user.password
        self.fullName = user.fullName
        self.language = user.language
        self.isAdmin = user.isAdmin
        self.isActive = user.isActive
        self.createdAt = user.createdAt
        self.updatedAt = user.updatedAt
        self.createdBy = user.createdBy
        self.updatedBy = user.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> User {
        User(
            id: id,
            username: username,
            password: password,
            fullName: fullName,
            language: language,
            isAdmin: isAdmin,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct PurchaseBatchDTO: Codable {
    let id: String
    let productId: String
    let siteId: String
    let initialQuantity: Double
    let remainingQuantity: Double
    let purchasePrice: Double
    let sellingPrice: Double?
    let supplierName: String
    let supplierId: String?
    let batchNumber: String?
    let purchaseDate: Int64
    let expiryDate: Int64?
    let isExhausted: Bool
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case productId = "product_id"
        case siteId = "site_id"
        case initialQuantity = "initial_quantity"
        case remainingQuantity = "remaining_quantity"
        case purchasePrice = "purchase_price"
        case sellingPrice = "selling_price"
        case supplierName = "supplier_name"
        case supplierId = "supplier_id"
        case batchNumber = "batch_number"
        case purchaseDate = "purchase_date"
        case expiryDate = "expiry_date"
        case isExhausted = "is_exhausted"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from batch: PurchaseBatch) {
        self.id = batch.id
        self.productId = batch.productId
        self.siteId = batch.siteId
        self.initialQuantity = batch.initialQuantity
        self.remainingQuantity = batch.remainingQuantity
        self.purchasePrice = batch.purchasePrice
        self.sellingPrice = nil
        self.supplierName = batch.supplierName
        self.supplierId = batch.supplierId
        self.batchNumber = batch.batchNumber
        self.purchaseDate = batch.purchaseDate
        self.expiryDate = batch.expiryDate?.int64Value
        self.isExhausted = batch.isExhausted
        self.createdAt = batch.createdAt
        self.updatedAt = batch.updatedAt
        self.createdBy = batch.createdBy
        self.updatedBy = batch.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> PurchaseBatch {
        PurchaseBatch(
            id: id,
            productId: productId,
            siteId: siteId,
            batchNumber: batchNumber,
            purchaseDate: purchaseDate,
            initialQuantity: initialQuantity,
            remainingQuantity: remainingQuantity,
            purchasePrice: purchasePrice,
            supplierName: supplierName,
            supplierId: supplierId,
            expiryDate: expiryDate.map { KotlinLong(longLong: $0) },
            isExhausted: isExhausted,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

struct SaleDTO: Codable {
    let id: String
    let siteId: String
    let customerId: String?
    let customerName: String?
    let totalAmount: Double
    let discountAmount: Double
    let finalAmount: Double
    let paymentMethod: String
    let status: String
    let notes: String?
    let saleDate: Int64
    let createdAt: Int64
    let createdBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case siteId = "site_id"
        case customerId = "customer_id"
        case customerName = "customer_name"
        case totalAmount = "total_amount"
        case discountAmount = "discount_amount"
        case finalAmount = "final_amount"
        case paymentMethod = "payment_method"
        case status, notes
        case saleDate = "sale_date"
        case createdAt = "created_at"
        case createdBy = "created_by"
        case clientId = "client_id"
    }

    init(from sale: Sale) {
        self.id = sale.id
        self.siteId = sale.siteId
        self.customerId = sale.customerId
        self.customerName = sale.customerName
        self.totalAmount = sale.totalAmount
        self.discountAmount = 0
        self.finalAmount = sale.totalAmount
        self.paymentMethod = "CASH"
        self.status = "COMPLETED"
        self.notes = nil
        self.saleDate = sale.date
        self.createdAt = sale.createdAt
        self.createdBy = sale.createdBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> Sale {
        Sale(
            id: id,
            customerName: customerName ?? "",
            customerId: customerId,
            date: saleDate,
            totalAmount: finalAmount,
            siteId: siteId,
            createdAt: createdAt,
            createdBy: createdBy
        )
    }
}

struct SaleItemDTO: Codable {
    let id: String
    let saleId: String
    let productId: String
    let productName: String?
    let unit: String?
    let quantity: Double
    let unitPrice: Double
    let totalPrice: Double
    let createdAt: Int64
    let createdBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case saleId = "sale_id"
        case productId = "product_id"
        case productName = "product_name"
        case unit
        case quantity
        case unitPrice = "unit_price"
        case totalPrice = "total_price"
        case createdAt = "created_at"
        case createdBy = "created_by"
        case clientId = "client_id"
    }

    init(from item: SaleItem) {
        self.id = item.id
        self.saleId = item.saleId
        self.productId = item.productId
        self.productName = item.productName
        self.unit = item.unit
        self.quantity = item.quantity
        self.unitPrice = item.unitPrice
        self.totalPrice = item.totalPrice
        self.createdAt = item.createdAt
        self.createdBy = item.createdBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> SaleItem {
        SaleItem(
            id: id,
            saleId: saleId,
            productId: productId,
            productName: productName ?? "",
            unit: unit ?? "",
            quantity: quantity,
            unitPrice: unitPrice,
            totalPrice: totalPrice,
            createdAt: createdAt,
            createdBy: createdBy
        )
    }
}

struct StockMovementDTO: Codable {
    let id: String
    let productId: String
    let siteId: String
    let quantity: Double
    let type: String
    let date: Int64
    let purchasePriceAtMovement: Double
    let sellingPriceAtMovement: Double
    // Legacy field for backward compatibility
    let movementType: String?
    let referenceId: String?
    let notes: String?
    let createdAt: Int64
    let createdBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case productId = "product_id"
        case siteId = "site_id"
        case quantity
        case type
        case date
        case purchasePriceAtMovement = "purchase_price_at_movement"
        case sellingPriceAtMovement = "selling_price_at_movement"
        case movementType = "movement_type"
        case referenceId = "reference_id"
        case notes
        case createdAt = "created_at"
        case createdBy = "created_by"
        case clientId = "client_id"
    }

    init(from movement: StockMovement) {
        self.id = movement.id
        self.productId = movement.productId
        self.siteId = movement.siteId
        self.quantity = movement.quantity
        self.type = movement.type
        self.date = movement.date
        self.purchasePriceAtMovement = movement.purchasePriceAtMovement
        self.sellingPriceAtMovement = movement.sellingPriceAtMovement
        self.movementType = movement.movementType ?? movement.type
        self.referenceId = movement.referenceId
        self.notes = movement.notes
        self.createdAt = movement.createdAt
        self.createdBy = movement.createdBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> StockMovement {
        StockMovement(
            id: id,
            productId: productId,
            siteId: siteId,
            quantity: quantity,
            type: type,
            date: date,
            purchasePriceAtMovement: purchasePriceAtMovement,
            sellingPriceAtMovement: sellingPriceAtMovement,
            movementType: movementType,
            referenceId: referenceId,
            notes: notes,
            createdAt: createdAt,
            createdBy: createdBy
        )
    }
}

struct ProductTransferDTO: Codable {
    let id: String
    let productId: String
    let fromSiteId: String
    let toSiteId: String
    let quantity: Double
    let status: String
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
    var clientId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case productId = "product_id"
        case fromSiteId = "from_site_id"
        case toSiteId = "to_site_id"
        case quantity, status, notes
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case createdBy = "created_by"
        case updatedBy = "updated_by"
        case clientId = "client_id"
    }

    init(from transfer: ProductTransfer) {
        self.id = transfer.id
        self.productId = transfer.productId
        self.fromSiteId = transfer.fromSiteId
        self.toSiteId = transfer.toSiteId
        self.quantity = transfer.quantity
        self.status = transfer.status
        self.notes = transfer.notes
        self.createdAt = transfer.createdAt
        self.updatedAt = transfer.updatedAt
        self.createdBy = transfer.createdBy
        self.updatedBy = transfer.updatedBy
        self.clientId = SyncClientId.current
    }

    func toEntity() -> ProductTransfer {
        ProductTransfer(
            id: id,
            productId: productId,
            fromSiteId: fromSiteId,
            toSiteId: toSiteId,
            quantity: quantity,
            status: status,
            notes: notes,
            createdAt: createdAt,
            updatedAt: updatedAt,
            createdBy: createdBy,
            updatedBy: updatedBy
        )
    }
}

// MARK: - Client ID for Realtime filtering

struct SyncClientId {
    private static let key = "medistock_client_id"

    static var current: String {
        if let existing = UserDefaults.standard.string(forKey: key) {
            return existing
        }
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: key)
        return newId
    }
}
