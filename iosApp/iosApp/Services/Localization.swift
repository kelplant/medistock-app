import Foundation
import shared

/// Swift wrapper for the shared LocalizationManager.
///
/// Usage:
/// ```swift
/// // Get a localized string
/// Text(Localized.save)  // "Save" in current locale
///
/// // Change language
/// Localized.setLanguage(.french)
///
/// // Format with parameters
/// let welcome = Localized.format(
///     Localized.welcomeBack,
///     "name", "John"
/// )
/// ```
enum Localized {
    /// Get the current strings object for accessing all localized strings
    static var strings: Strings {
        return LocalizationManager.shared.strings
    }

    // MARK: - Locale Management

    /// Set the current language
    static func setLanguage(_ locale: AppLanguage) {
        LocalizationManager.shared.setLocale(locale: locale.supportedLocale)
        // Persist preference
        UserDefaults.standard.set(locale.code, forKey: "app_language")
    }

    /// Set language by code (e.g., "en", "fr", "de", "es")
    @discardableResult
    static func setLanguageByCode(_ code: String) -> Bool {
        let result = LocalizationManager.shared.setLocaleByCode(code: code)
        if result {
            UserDefaults.standard.set(code, forKey: "app_language")
        }
        return result
    }

    /// Get the current language code
    static var currentLanguageCode: String {
        return LocalizationManager.shared.getCurrentLocaleCode()
    }

    /// Get the current language display name (in native language)
    static var currentLanguageDisplayName: String {
        return LocalizationManager.shared.getCurrentLocaleDisplayName()
    }

    /// Get all available languages
    static var availableLanguages: [AppLanguage] {
        return AppLanguage.allCases
    }

    /// Load saved language preference or use system language
    static func loadSavedLanguage() {
        if let savedCode = UserDefaults.standard.string(forKey: "app_language") {
            _ = setLanguageByCode(savedCode)
        } else {
            // Try to use system language (compatible with iOS 15+)
            let preferredLanguages = Locale.preferredLanguages
            let systemLanguage = preferredLanguages.first?.prefix(2).lowercased() ?? "en"
            _ = setLanguageByCode(String(systemLanguage))
        }
    }

    /// Load language from user profile with fallback chain:
    /// 1. User profile (user.language)
    /// 2. Local cache (UserDefaults)
    /// 3. System language
    /// 4. English (default)
    static func loadLanguageFromProfile(user: User?) {
        let code: String
        if let userLang = user?.language {
            code = userLang
            // Cache from profile for offline access
            UserDefaults.standard.set(userLang, forKey: "app_language")
            SessionManager.shared.language = userLang
        } else if let cached = UserDefaults.standard.string(forKey: "app_language") {
            code = cached
        } else {
            let preferredLanguages = Locale.preferredLanguages
            code = String(preferredLanguages.first?.prefix(2).lowercased() ?? "en")
        }
        _ = setLanguageByCode(code)
    }

    /// Set language and sync to user profile (local DB + Supabase)
    static func setLanguageAndSync(_ locale: AppLanguage, sdk: MedistockSDK) async {
        // 1. Update LocalizationManager
        LocalizationManager.shared.setLocale(locale: locale.supportedLocale)

        // 2. Cache locally and get userId on MainActor for thread-safety
        UserDefaults.standard.set(locale.code, forKey: "app_language")
        let userId = await MainActor.run {
            SessionManager.shared.language = locale.code
            return SessionManager.shared.userId
        }
        guard !userId.isEmpty else { return }

        let now = Int64(Date().timeIntervalSince1970 * 1000)

        do {
            // Update local database
            try await sdk.userRepository.updateLanguage(
                userId: userId,
                language: locale.code,
                updatedAt: now,
                updatedBy: userId
            )

            // Sync to Supabase if configured and online
            if SupabaseService.shared.isConfigured && SyncStatusManager.shared.isOnline {
                if let user = try await sdk.userRepository.getById(id: userId) {
                    let dto = UserDTO(from: user)
                    try await SupabaseService.shared.upsert(into: "app_users", record: dto)
                }
            } else if !SyncStatusManager.shared.isOnline {
                // Queue for sync when back online
                if let user = try await sdk.userRepository.getById(id: userId) {
                    SyncQueueHelper.shared.enqueueUserUpdate(user, userId: userId, lastKnownRemoteUpdatedAt: user.updatedAt)
                }
            }
        } catch {
            print("Failed to sync language to profile: \(error.localizedDescription)")
        }
    }

    // MARK: - String Formatting

    /// Format a string with named parameters
    /// Example: format(strings.welcomeBack, "name", "John")
    static func format(_ template: String, _ params: Any...) -> String {
        var pairs: [KotlinPair<NSString, AnyObject>] = []
        var i = 0
        while i < params.count - 1 {
            if let key = params[i] as? String {
                let value = params[i + 1]
                pairs.append(KotlinPair(first: key as NSString, second: value as AnyObject))
            }
            i += 2
        }
        // Note: Direct format call with vararg isn't easily accessible from Swift
        // We'll do it manually
        var result = template
        i = 0
        while i < params.count - 1 {
            if let key = params[i] as? String {
                let value = params[i + 1]
                result = result.replacingOccurrences(of: "{\(key)}", with: "\(value)")
            }
            i += 2
        }
        return result
    }

    // MARK: - Quick Access to Common Strings

    static var appName: String { strings.appName }
    static var ok: String { strings.ok }
    static var delete_: String { strings.delete_ }
    static var cancel: String { strings.cancel }
    static var save: String { strings.save }
    static var deleteText: String { strings.delete_ }
    static var edit: String { strings.edit }
    static var add: String { strings.add }
    static var search: String { strings.search }
    static var loading: String { strings.loading }
    static var error: String { strings.error }
    static var success: String { strings.success }
    static var warning: String { strings.warning }
    static var confirm: String { strings.confirm }
    static var yes: String { strings.yes }
    static var no: String { strings.no }
    static var close: String { strings.close }
    static var back: String { strings.back }
    static var next: String { strings.next }
    static var retry: String { strings.retry }
    static var noData: String { strings.noData }
    static var required: String { strings.required }

    // Auth
    static var loginTitle: String { strings.loginTitle }
    static var username: String { strings.username }
    static var password: String { strings.password }
    static var login: String { strings.login }
    static var logout: String { strings.logout }
    static var logoutConfirm: String { strings.logoutConfirm }
    static var changePassword: String { strings.changePassword }
    static var loginError: String { strings.loginError }
    static var loginErrorInvalidCredentials: String { strings.loginErrorInvalidCredentials }
    static var welcomeBack: String { strings.welcomeBack }

    // Profile
    static var profile: String { strings.profile }
    static var myProfile: String { strings.myProfile }
    static var information: String { strings.information }
    static var currentPassword: String { strings.currentPassword }
    static var newPassword: String { strings.newPassword }
    static var confirmPassword: String { strings.confirmPassword }
    static var passwordsDoNotMatch: String { strings.passwordsDoNotMatch }
    static var passwordChangedSuccessfully: String { strings.passwordChangedSuccessfully }
    static var userNotFound: String { strings.userNotFound }
    static var incorrectPassword: String { strings.incorrectPassword }

    // Sync Status
    static var synced: String { strings.synced }
    static var pendingChanges: String { strings.pendingChanges }
    static var conflictsToResolve: String { strings.conflictsToResolve }
    static var online: String { strings.online }
    static var offline: String { strings.offline }
    static var realtimeConnected: String { strings.realtimeConnected }
    static var realtimeDisconnected: String { strings.realtimeDisconnected }
    static var lastError: String { strings.lastError }
    static var offlineMode: String { strings.offlineMode }
    static var syncing: String { strings.syncing }
    static var syncNow: String { strings.syncNow }

    // Settings
    static var settings: String { strings.settings }
    static var language: String { strings.language }
    static var selectLanguage: String { strings.selectLanguage }

    // Users
    static var users: String { strings.users }
    static var fullName: String { strings.fullName }
    static var role: String { strings.role }
    static var admin: String { strings.admin }
    static var user: String { strings.user }
    static var addUser: String { strings.addUser }
    static var editUser: String { strings.editUser }
    static var deleteUser: String { strings.deleteUser }
    static var permissions: String { strings.permissions }
    static var canView: String { strings.canView }
    static var canCreate: String { strings.canCreate }
    static var canEdit: String { strings.canEdit }
    static var canDelete: String { strings.canDelete }

    // Referential Integrity / Status
    static var active: String { strings.active }
    static var inactive: String { strings.inactive }
    static var deactivate: String { strings.deactivate }
    static var reactivate: String { strings.reactivate }

    // Notifications
    static var notificationSettings: String { strings.notificationSettings }
    static var notificationExpiryAlerts: String { strings.notificationExpiryAlerts }
    static var notificationEnableExpiry: String { strings.notificationEnableExpiry }
    static var notificationWarningDays: String { strings.notificationWarningDays }
    static var notificationExpiryDescription: String { strings.notificationExpiryDescription }
    static var notificationLowStockAlerts: String { strings.notificationLowStockAlerts }
    static var notificationEnableLowStock: String { strings.notificationEnableLowStock }
    static var notificationLowStockDescription: String { strings.notificationLowStockDescription }
    static var notificationInvalidDays: String { strings.notificationInvalidDays }
    static var settingsSaved: String { strings.settingsSaved }
    static var supabaseNotConfigured: String { strings.supabaseNotConfigured }
    static var notifications: String { strings.notifications }
    static var noNotifications: String { strings.noNotifications }
    static var dismissAll: String { strings.dismissAll }

    // Sites
    static var sites: String { strings.sites }
    static var site: String { strings.site }
    static var siteName: String { strings.siteName }
    static var addSite: String { strings.addSite }
    static var editSite: String { strings.editSite }
    static var deleteSite: String { strings.deleteSite }
    static var noSites: String { strings.noSites }
    static var selectSite: String { strings.selectSite }
    static var allSites: String { strings.allSites }
    static var currentSite: String { strings.currentSite }

    // Categories
    static var categories: String { strings.categories }
    static var category: String { strings.category }
    static var categoryName: String { strings.categoryName }
    static var addCategory: String { strings.addCategory }
    static var editCategory: String { strings.editCategory }
    static var deleteCategory: String { strings.deleteCategory }
    static var noCategories: String { strings.noCategories }
    static var selectCategory: String { strings.selectCategory }

    // Products
    static var products: String { strings.products }
    static var product: String { strings.product }
    static var productName: String { strings.productName }
    static var addProduct: String { strings.addProduct }
    static var editProduct: String { strings.editProduct }
    static var deleteProduct: String { strings.deleteProduct }
    static var noProducts: String { strings.noProducts }
    static var selectProduct: String { strings.selectProduct }
    static var unit: String { strings.unit }
    static var description_: String { strings.description_ }
    static var minStock: String { strings.minStock }
    static var maxStock: String { strings.maxStock }
    static var currentStock: String { strings.currentStock }
    static var price: String { strings.price }
    static var purchasePrice: String { strings.purchasePrice }
    static var sellingPrice: String { strings.sellingPrice }

    // Customers
    static var customers: String { strings.customers }
    static var customer: String { strings.customer }
    static var customerName: String { strings.customerName }
    static var addCustomer: String { strings.addCustomer }
    static var editCustomer: String { strings.editCustomer }
    static var deleteCustomer: String { strings.deleteCustomer }
    static var noCustomers: String { strings.noCustomers }
    static var selectCustomer: String { strings.selectCustomer }
    static var phone: String { strings.phone }
    static var email: String { strings.email }
    static var address: String { strings.address }
    static var notes: String { strings.notes }
    static var walkInCustomer: String { strings.walkInCustomer }

    // Suppliers
    static var suppliers: String { strings.suppliers }
    static var addSupplier: String { strings.addSupplier }
    static var editSupplier: String { strings.editSupplier }
    static var deleteSupplier: String { strings.deleteSupplier }
    static var noSuppliers: String { strings.noSuppliers }
    static var selectSupplier: String { strings.selectSupplier }
    static var manageSuppliers: String { strings.manageSuppliers }

    // Purchases
    static var purchases: String { strings.purchases }
    static var purchase: String { strings.purchase }
    static var addPurchase: String { strings.addPurchase }
    static var newPurchase: String { strings.newPurchase }
    static var purchaseHistory: String { strings.purchaseHistory }
    static var supplier: String { strings.supplier }
    static var supplierName: String { strings.supplierName }
    static var batchNumber: String { strings.batchNumber }
    static var purchaseDate: String { strings.purchaseDate }
    static var expiryDate: String { strings.expiryDate }
    static var quantity: String { strings.quantity }
    static var totalAmount: String { strings.totalAmount }
    static var purchaseRecorded: String { strings.purchaseRecorded }
    static var exhausted: String { strings.exhausted }
    static var remainingQty: String { strings.remainingQty }
    static var noPurchases: String { strings.noPurchases }

    // Sales
    static var sales: String { strings.sales }
    static var sale: String { strings.sale }
    static var newSale: String { strings.newSale }
    static var saleHistory: String { strings.saleHistory }
    static var saleDate: String { strings.saleDate }
    static var saleTotal: String { strings.saleTotal }
    static var saleItems: String { strings.saleItems }
    static var addItem: String { strings.addItem }
    static var removeItem: String { strings.removeItem }
    static var unitPrice: String { strings.unitPrice }
    static var subtotal: String { strings.subtotal }
    static var discount: String { strings.discount }
    static var grandTotal: String { strings.grandTotal }
    static var completeSale: String { strings.completeSale }
    static var saleCompleted: String { strings.saleCompleted }
    static var noSaleItems: String { strings.noSaleItems }
    static var noSales: String { strings.noSales }
    static var saleDetails: String { strings.saleDetails }
    static var items: String { strings.items }
    static var total: String { strings.total }
    static var date: String { strings.date }

    // Inventory
    static var inventory: String { strings.inventory }
    static var inventoryCount: String { strings.inventoryCount }
    static var startInventory: String { strings.startInventory }
    static var completeInventory: String { strings.completeInventory }
    static var theoreticalQuantity: String { strings.theoreticalQuantity }
    static var countedQuantity: String { strings.countedQuantity }
    static var discrepancy: String { strings.discrepancy }
    static var reason: String { strings.reason }
    static var inventoryCompleted: String { strings.inventoryCompleted }
    static var inventories: String { strings.inventories }
    static var noInventories: String { strings.noInventories }
    static var inProgress: String { strings.inProgress }
    static var completed: String { strings.completed }
    static var pending: String { strings.pending }
    static var newInventory: String { strings.newInventory }
    static var start: String { strings.start }

    // Transfers
    static var transfers: String { strings.transfers }
    static var transfer: String { strings.transfer }
    static var newTransfer: String { strings.newTransfer }
    static var transferHistory: String { strings.transferHistory }
    static var fromSite: String { strings.fromSite }
    static var toSite: String { strings.toSite }
    static var transferStatus: String { strings.transferStatus }
    static var transferPending: String { strings.transferPending }
    static var transferCompleted: String { strings.transferCompleted }
    static var transferCancelled: String { strings.transferCancelled }
    static var noTransfers: String { strings.noTransfers }
    static var sourceSite: String { strings.sourceSite }
    static var destinationSite: String { strings.destinationSite }
    static var quantityToTransfer: String { strings.quantityToTransfer }
    static var completeTransfer: String { strings.completeTransfer }
    static var create: String { strings.create }

    // Stock
    static var stock: String { strings.stock }
    static var stockMovements: String { strings.stockMovements }
    static var stockIn: String { strings.stockIn }
    static var stockOut: String { strings.stockOut }
    static var stockAdjustment: String { strings.stockAdjustment }
    static var movementType: String { strings.movementType }
    static var movementDate: String { strings.movementDate }
    static var noStock: String { strings.noStock }
    static var summary: String { strings.summary }
    static var outOfStock: String { strings.outOfStock }
    static var lowStock: String { strings.lowStock }
    static var stockByProduct: String { strings.stockByProduct }
    static var noMovements: String { strings.noMovements }
    static var availableStock: String { strings.availableStock }
    static var preview: String { strings.preview }

    // Packaging Types
    static var packagingTypes: String { strings.packagingTypes }
    static var packagingType: String { strings.packagingType }
    static var addPackagingType: String { strings.addPackagingType }
    static var editPackagingType: String { strings.editPackagingType }
    static var noPackagingTypes: String { strings.noPackagingTypes }
    static var level1Name: String { strings.level1Name }
    static var level2Name: String { strings.level2Name }
    static var level2Quantity: String { strings.level2Quantity }
    static var addLevel2: String { strings.addLevel2 }

    // Audit
    static var auditHistory: String { strings.auditHistory }
    static var noHistory: String { strings.noHistory }
    static var filterBy: String { strings.filterBy }
    static var all: String { strings.all }
    static var created: String { strings.created }
    static var updated: String { strings.updated }
    static var deleted: String { strings.deleted }
    static var details: String { strings.details }

    // Administration
    static var administration: String { strings.administration }
    static var management: String { strings.management }
    static var siteManagement: String { strings.siteManagement }
    static var manageProducts: String { strings.manageProducts }
    static var manageCategories: String { strings.manageCategories }
    static var manageCustomers: String { strings.manageCustomers }
    static var userManagement: String { strings.userManagement }
    static var history: String { strings.history }
    static var configuration: String { strings.configuration }

    // Home Operations
    static var operations: String { strings.operations }
    static var viewStock: String { strings.viewStock }
    static var inventoryStock: String { strings.inventoryStock }

    // Supabase
    static var supabaseConfiguration: String { strings.supabaseConfiguration }
    static var projectUrl: String { strings.projectUrl }
    static var anonKey: String { strings.anonKey }
    static var synchronization: String { strings.synchronization }
    static var syncData: String { strings.syncData }
    static var lastSync: String { strings.lastSync }
    static var currentStatus: String { strings.currentStatus }
    static var configured: String { strings.configured }
    static var connection: String { strings.connection }
    static var testConnection: String { strings.testConnection }
    static var clearConfiguration: String { strings.clearConfiguration }
    static var configSaved: String { strings.configSaved }
    static var syncCompleted: String { strings.syncCompleted }
    static var connectionSuccessful: String { strings.connectionSuccessful }
    static var howToGetInfo: String { strings.howToGetInfo }

    // Auth extended
    static var configureSupabase: String { strings.configureSupabase }
    static var authentication: String { strings.authentication }
    static var enterCredentials: String { strings.enterCredentials }
    static var invalidPassword: String { strings.invalidPassword }
    static var accountDisabled: String { strings.accountDisabled }
    static var connectionError: String { strings.connectionError }
    static var firstLoginRequiresInternet: String { strings.firstLoginRequiresInternet }

    // View/Edit/Create labels
    static var view: String { strings.view }

    // Selection
    static var select: String { strings.select }
    static var chooseProduct: String { strings.chooseProduct }
    static var orSelect: String { strings.orSelect }

    // Misc
    static var enable: String { strings.enable }
    static var later: String { strings.later }
    static var alertsDescription: String { strings.alertsDescription }
    static var justNow: String { strings.justNow }
    static var minutesAgo: String { strings.minutesAgo }
    static var hoursAgo: String { strings.hoursAgo }
    static var daysAgo: String { strings.daysAgo }
    static var critical: String { strings.critical }
    static var urgent: String { strings.urgent }
    static var info: String { strings.info }
    static var low: String { strings.low }
    static var nearestExpiry: String { strings.nearestExpiry }
    static var lots: String { strings.lots }
    static var addNote: String { strings.addNote }
    static var saving: String { strings.saving }
    static var `continue`: String { strings.continue_ }
    static var unknownSite: String { strings.unknownSite }
    static var unknownProduct: String { strings.unknownProduct }
}

/// Available app languages
enum AppLanguage: String, CaseIterable, Identifiable {
    case english = "en"
    case french = "fr"
    case german = "de"
    case spanish = "es"
    case italian = "it"
    case russian = "ru"
    case bemba = "bem"
    case nyanja = "ny"

    var id: String { rawValue }
    var code: String { rawValue }

    var displayName: String {
        switch self {
        case .english: return "English"
        case .french: return "Français"
        case .german: return "Deutsch"
        case .spanish: return "Español"
        case .italian: return "Italiano"
        case .russian: return "Русский"
        case .bemba: return "Ichibemba"
        case .nyanja: return "Chinyanja"
        }
    }

    var supportedLocale: SupportedLocale {
        switch self {
        case .english: return .english
        case .french: return .french
        case .german: return .german
        case .spanish: return .spanish
        case .italian: return .italian
        case .russian: return .russian
        case .bemba: return .bemba
        case .nyanja: return .nyanja
        }
    }

    static func from(code: String) -> AppLanguage {
        return AppLanguage(rawValue: code) ?? .english
    }
}
