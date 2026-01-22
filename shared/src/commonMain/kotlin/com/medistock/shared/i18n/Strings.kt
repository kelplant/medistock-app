package com.medistock.shared.i18n

/**
 * Interface defining all localizable strings in the app.
 * Each supported language implements this interface.
 *
 * To add a new language:
 * 1. Create a new object implementing Strings (e.g., StringsIt for Italian)
 * 2. Register it in SupportedLocale enum
 * 3. Add the JSON file for translators: strings_it.json
 */
interface Strings {
    // ============================================
    // COMMON
    // ============================================
    val appName: String
    val ok: String
    val cancel: String
    val save: String
    val delete: String
    val edit: String
    val add: String
    val search: String
    val loading: String
    val error: String
    val success: String
    val warning: String
    val confirm: String
    val yes: String
    val no: String
    val close: String
    val back: String
    val next: String
    val retry: String
    val noData: String
    val required: String

    // ============================================
    // AUTH
    // ============================================
    val loginTitle: String
    val username: String
    val password: String
    val login: String
    val logout: String
    val logoutConfirm: String
    val loginError: String
    val loginErrorInvalidCredentials: String
    val welcomeBack: String // Parameter: {name}

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    val home: String
    val dashboard: String
    val quickActions: String
    val recentActivity: String
    val todaySales: String
    val lowStock: String
    val pendingTransfers: String

    // ============================================
    // SITES
    // ============================================
    val sites: String
    val site: String
    val siteName: String
    val addSite: String
    val editSite: String
    val deleteSite: String
    val deleteSiteConfirm: String
    val siteDeleted: String
    val siteDeactivated: String
    val noSites: String
    val selectSite: String
    val allSites: String

    // ============================================
    // CATEGORIES
    // ============================================
    val categories: String
    val category: String
    val categoryName: String
    val addCategory: String
    val editCategory: String
    val deleteCategory: String
    val deleteCategoryConfirm: String
    val categoryDeleted: String
    val categoryDeactivated: String
    val noCategories: String
    val selectCategory: String
    val allCategories: String
    val uncategorized: String

    // ============================================
    // PRODUCTS
    // ============================================
    val products: String
    val product: String
    val productName: String
    val addProduct: String
    val editProduct: String
    val deleteProduct: String
    val deleteProductConfirm: String
    val productDeleted: String
    val productDeactivated: String
    val noProducts: String
    val selectProduct: String
    val unit: String
    val unitVolume: String
    val description: String
    val minStock: String
    val maxStock: String
    val currentStock: String
    val price: String
    val purchasePrice: String
    val sellingPrice: String
    val margin: String
    val marginType: String
    val marginValue: String

    // ============================================
    // CUSTOMERS
    // ============================================
    val customers: String
    val customer: String
    val customerName: String
    val addCustomer: String
    val editCustomer: String
    val deleteCustomer: String
    val deleteCustomerConfirm: String
    val customerDeleted: String
    val customerDeactivated: String
    val noCustomers: String
    val selectCustomer: String
    val phone: String
    val email: String
    val address: String
    val notes: String
    val walkInCustomer: String

    // ============================================
    // PURCHASES
    // ============================================
    val purchases: String
    val purchase: String
    val addPurchase: String
    val purchaseHistory: String
    val supplier: String
    val supplierName: String
    val batchNumber: String
    val purchaseDate: String
    val expiryDate: String
    val quantity: String
    val initialQuantity: String
    val remainingQuantity: String
    val totalAmount: String
    val purchaseRecorded: String

    // ============================================
    // SALES
    // ============================================
    val sales: String
    val sale: String
    val newSale: String
    val saleHistory: String
    val saleDate: String
    val saleTotal: String
    val saleItems: String
    val addItem: String
    val removeItem: String
    val unitPrice: String
    val itemTotal: String
    val subtotal: String
    val discount: String
    val grandTotal: String
    val completeSale: String
    val saleCompleted: String
    val noSaleItems: String
    val insufficientStock: String // Parameter: {product}, {available}, {requested}

    // ============================================
    // INVENTORY
    // ============================================
    val inventory: String
    val inventoryCount: String
    val startInventory: String
    val completeInventory: String
    val inventoryInProgress: String
    val theoreticalQuantity: String
    val countedQuantity: String
    val discrepancy: String
    val reason: String
    val inventoryCompleted: String

    // ============================================
    // TRANSFERS
    // ============================================
    val transfers: String
    val transfer: String
    val newTransfer: String
    val transferHistory: String
    val fromSite: String
    val toSite: String
    val transferStatus: String
    val transferPending: String
    val transferCompleted: String
    val transferCancelled: String
    val completeTransfer: String
    val cancelTransfer: String

    // ============================================
    // STOCK
    // ============================================
    val stock: String
    val stockMovements: String
    val stockIn: String
    val stockOut: String
    val stockAdjustment: String
    val movementType: String
    val movementDate: String

    // ============================================
    // REPORTS
    // ============================================
    val reports: String
    val salesReport: String
    val stockReport: String
    val profitReport: String
    val exportReport: String
    val dateRange: String
    val startDate: String
    val endDate: String
    val generateReport: String

    // ============================================
    // SETTINGS
    // ============================================
    val settings: String
    val language: String
    val selectLanguage: String
    val theme: String
    val darkMode: String
    val lightMode: String
    val systemDefault: String
    val about: String
    val version: String
    val syncSettings: String
    val lastSync: String
    val syncNow: String
    val syncing: String
    val syncSuccess: String
    val syncError: String

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    val users: String
    val user: String
    val addUser: String
    val editUser: String
    val deleteUser: String
    val fullName: String
    val role: String
    val admin: String
    val permissions: String
    val canView: String
    val canCreate: String
    val canEdit: String
    val canDelete: String

    // ============================================
    // PACKAGING TYPES
    // ============================================
    val packagingTypes: String
    val packagingType: String
    val addPackagingType: String
    val editPackagingType: String
    val level1Name: String
    val level2Name: String
    val level2Quantity: String
    val conversionFactor: String

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    val fieldRequired: String // Parameter: {field}
    val invalidEmail: String
    val invalidPhone: String
    val valueTooShort: String // Parameter: {field}, {min}
    val valueTooLong: String // Parameter: {field}, {max}
    val valueMustBePositive: String
    val passwordTooShort: String

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    val cannotDelete: String
    val entityInUse: String // Parameter: {entity}, {count}
    val deactivateInstead: String
    val deactivate: String
    val reactivate: String
    val showInactive: String
    val hideInactive: String
    val inactive: String
    val active: String

    // ============================================
    // DATE & TIME
    // ============================================
    val today: String
    val yesterday: String
    val thisWeek: String
    val thisMonth: String
    val thisYear: String
    val dateFormat: String // e.g., "MM/dd/yyyy" or "dd/MM/yyyy"
    val timeFormat: String // e.g., "HH:mm" or "hh:mm a"

    // ============================================
    // NUMBERS & CURRENCY
    // ============================================
    val currencySymbol: String
    val currencyFormat: String // e.g., "{symbol}{amount}" or "{amount} {symbol}"
    val decimalSeparator: String
    val thousandsSeparator: String
}
