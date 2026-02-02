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
    val changePassword: String
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
    // SUPPLIERS
    // ============================================
    val suppliers: String
    val addSupplier: String
    val editSupplier: String
    val deleteSupplier: String
    val deleteSupplierConfirm: String
    val supplierDeleted: String
    val supplierDeactivated: String
    val noSuppliers: String
    val selectSupplier: String
    val manageSuppliers: String
    val noSuppliersMessage: String

    // ============================================
    // PURCHASES
    // ============================================
    val purchases: String
    val purchase: String
    val newPurchase: String              // "New Purchase" / screen title
    val addPurchase: String
    val purchaseHistory: String
    val supplier: String
    val supplierName: String
    val batchNumber: String
    val purchaseDate: String
    val expiryDate: String
    val expiryDateOptional: String       // "Expiry Date (optional)"
    val quantity: String
    val initialQuantity: String
    val remainingQuantity: String
    val totalAmount: String
    val purchaseRecorded: String
    val unitPurchasePrice: String        // "Unit Purchase Price"
    val unitSellingPrice: String         // "Unit Selling Price"
    val marginCalculatedAuto: String     // "Margin: calculated automatically"
    val sellingPriceNote: String         // "The selling price is calculated automatically..."
    val savePurchase: String             // "Save Purchase"
    val enterSupplierName: String        // Placeholder: "Enter supplier name"
    val batchNumberExample: String       // Placeholder: "Ex: LOT2024001"

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
    val remainingQuantityNeeded: String  // "Remaining quantity needed: {quantity} units"
    val editSale: String                 // "Edit Sale"
    val editPurchase: String             // "Edit Purchase"
    val productsToSell: String           // "Products to Sell"
    val addProductToSale: String         // "+ Add Product"
    val enterCustomerName: String        // Placeholder: "Enter customer name"
    val pricePerUnit: String             // Placeholder: "Price per unit"
    val exampleQuantity: String          // Placeholder: "Ex: 10"

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
    // PROFILE
    // ============================================
    val profile: String
    val myProfile: String
    val information: String
    val currentPassword: String
    val newPassword: String
    val confirmPassword: String
    val passwordsDoNotMatch: String
    val passwordChangedSuccessfully: String
    val userNotFound: String
    val incorrectPassword: String

    // ============================================
    // SYNC STATUS
    // ============================================
    val synced: String
    val pendingChanges: String // Parameter: {count}
    val conflictsToResolve: String // Parameter: {count}
    val online: String
    val offline: String
    val realtimeConnected: String
    val realtimeDisconnected: String
    val lastError: String
    val offlineMode: String
    val conflictsDetected: String
    val changesWillSyncWhenOnline: String

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
    // PASSWORD COMPLEXITY
    // ============================================
    val passwordMinLength: String        // "Password must be at least 8 characters"
    val passwordNeedsUppercase: String   // "Must contain at least one uppercase letter"
    val passwordNeedsLowercase: String   // "Must contain at least one lowercase letter"
    val passwordNeedsDigit: String       // "Must contain at least one digit"
    val passwordNeedsSpecial: String     // "Must contain at least one special character"
    val passwordStrengthWeak: String     // "Weak"
    val passwordStrengthMedium: String   // "Medium"
    val passwordStrengthStrong: String   // "Strong"
    val passwordRequirements: String     // "Password requirements:"
    val passwordStrength: String         // "Password strength:"
    val passwordMustBeDifferent: String  // "New password must be different from current password"
    val usernameAlreadyExists: String    // "Username already exists"

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

    // ============================================
    // NOTIFICATIONS
    // ============================================
    val notificationSettings: String
    val notificationExpiryAlerts: String
    val notificationEnableExpiry: String
    val notificationWarningDays: String
    val notificationExpiryDescription: String
    val notificationLowStockAlerts: String
    val notificationEnableLowStock: String
    val notificationLowStockDescription: String
    val notificationInvalidDays: String
    val settingsSaved: String
    val supabaseNotConfigured: String
    val notifications: String
    val noNotifications: String
    val dismissAll: String
    val allNotificationsDismissed: String

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    val currentSite: String
    val operations: String
    val purchaseProducts: String
    val sellProducts: String
    val transferProducts: String
    val viewStock: String
    val inventoryStock: String
    val administration: String
    val management: String
    val siteManagement: String
    val manageProducts: String
    val manageCategories: String
    val manageCustomers: String
    val userManagement: String
    val history: String
    val configuration: String

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    val exhausted: String
    val remainingQty: String
    val noPurchases: String

    // ============================================
    // SALES EXTENDED
    // ============================================
    val noSales: String
    val saleDetails: String
    val items: String
    val total: String
    val date: String

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    val inventories: String
    val noInventories: String
    val inProgress: String
    val completed: String
    val pending: String
    val newInventory: String
    val start: String

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    val noTransfers: String
    val sourceSite: String
    val destinationSite: String
    val quantityToTransfer: String
    val create: String

    // ============================================
    // STOCK EXTENDED
    // ============================================
    val noStock: String
    val summary: String
    val outOfStock: String
    val stockByProduct: String
    val noMovements: String
    val availableStock: String
    val preview: String

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    val noPackagingTypes: String
    val addLevel2: String

    // ============================================
    // AUDIT
    // ============================================
    val auditHistory: String
    val noHistory: String
    val filterBy: String
    val all: String
    val created: String
    val updated: String
    val deleted: String
    val details: String

    // ============================================
    // SUPABASE
    // ============================================
    val supabaseConfiguration: String
    val projectUrl: String
    val anonKey: String
    val synchronization: String
    val syncData: String
    val currentStatus: String
    val configured: String
    val connection: String
    val testConnection: String
    val clearConfiguration: String
    val configSaved: String
    val syncCompleted: String
    val connectionSuccessful: String
    val howToGetInfo: String

    // ============================================
    // AUTH EXTENDED
    // ============================================
    val configureSupabase: String
    val authentication: String
    val enterCredentials: String
    val invalidPassword: String
    val accountDisabled: String
    val connectionError: String
    val firstLoginRequiresInternet: String

    // ============================================
    // UI LABELS
    // ============================================
    val view: String
    val select: String
    val chooseProduct: String
    val orSelect: String
    val enable: String
    val later: String
    val alertsDescription: String
    val justNow: String
    val minutesAgo: String
    val hoursAgo: String
    val daysAgo: String
    val critical: String
    val urgent: String
    val info: String
    val low: String
    val nearestExpiry: String
    val lots: String
    val addNote: String
    val saving: String
    val continue_: String
    val unknownSite: String
    val unknownProduct: String

    // ============================================
    // EMPTY STATE MESSAGES
    // ============================================
    val noProductsMessage: String
    val noCustomersMessage: String
    val noCategoriesMessage: String
    val noSitesMessage: String
    val noPackagingTypesMessage: String
    val noInventoriesMessage: String
    val noSalesMessage: String
    val noPurchasesMessage: String
    val noTransfersMessage: String
    val noStockMessage: String
    val noMovementsMessage: String
    val noUsersMessage: String
    val historyWillAppearHere: String

    // ============================================
    // ADDITIONAL UI STRINGS
    // ============================================
    val addSiteFirst: String
    val none: String
    val stockAlerts: String
    val stockAlertDescription: String
    val transferIn: String
    val transferOut: String
    val batches: String
    val noUsers: String
    val adminHasAllPermissions: String
    val create_: String
    val selectSourceSiteFirst: String
    val entries: String
    val optional: String
    val packagingTypeName: String
    val started: String
    val saleItem: String
    val purchaseBatch: String
    val stockMovement: String
    val supabaseStep1: String
    val supabaseStep2: String
    val supabaseStep3: String

    // ============================================
    // APP UPDATE
    // ============================================
    val updateRequired: String
    val updateAvailable: String
    val appVersionIncompatible: String
    val appVersion: String
    val minimumRequiredVersion: String
    val databaseVersion: String
    val toUpdate: String
    val contactAdminForUpdate: String
    val checkingCompatibility: String
    val download: String
    val newVersionAvailable: String
    val currentVersionLabel: String
    val newVersionLabel: String
    val whatsNew: String
    val unableToLoadNotifications: String

    // ============================================
    // APP SETTINGS
    // ============================================
    val appSettings: String                // "App Settings"
    val currencySymbolSetting: String      // "Currency Symbol"
    val currencySymbolDescription: String  // "Symbol used to display prices (e.g., F, $, EUR)"
    val settingsSavedSuccessfully: String  // "Settings saved successfully"
    val invalidCurrencySymbol: String      // "Please enter a valid currency symbol"
    val debugMode: String                  // "Debug Mode"
    val debugModeDescription: String       // "Enable verbose logging for troubleshooting"
}
