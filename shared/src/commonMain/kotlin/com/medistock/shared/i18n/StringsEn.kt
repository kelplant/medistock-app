package com.medistock.shared.i18n

/**
 * English strings implementation (default language).
 */
object StringsEn : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val edit = "Edit"
    override val add = "Add"
    override val search = "Search"
    override val loading = "Loading..."
    override val error = "Error"
    override val success = "Success"
    override val warning = "Warning"
    override val confirm = "Confirm"
    override val yes = "Yes"
    override val no = "No"
    override val close = "Close"
    override val back = "Back"
    override val next = "Next"
    override val retry = "Retry"
    override val noData = "No data available"
    override val required = "Required"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Login"
    override val username = "Username"
    override val password = "Password"
    override val login = "Log in"
    override val logout = "Log out"
    override val logoutConfirm = "Are you sure you want to log out?"
    override val changePassword = "Change Password"
    override val loginError = "Login failed"
    override val loginErrorInvalidCredentials = "Invalid username or password"
    override val welcomeBack = "Welcome back, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Home"
    override val dashboard = "Dashboard"
    override val quickActions = "Quick Actions"
    override val recentActivity = "Recent Activity"
    override val todaySales = "Today's Sales"
    override val lowStock = "Low Stock"
    override val pendingTransfers = "Pending Transfers"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Sites"
    override val site = "Site"
    override val siteName = "Site Name"
    override val addSite = "Add Site"
    override val editSite = "Edit Site"
    override val deleteSite = "Delete Site"
    override val deleteSiteConfirm = "Are you sure you want to delete this site?"
    override val siteDeleted = "Site deleted"
    override val siteDeactivated = "Site deactivated"
    override val noSites = "No sites found"
    override val selectSite = "Select a site"
    override val allSites = "All Sites"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Categories"
    override val category = "Category"
    override val categoryName = "Category Name"
    override val addCategory = "Add Category"
    override val editCategory = "Edit Category"
    override val deleteCategory = "Delete Category"
    override val deleteCategoryConfirm = "Are you sure you want to delete this category?"
    override val categoryDeleted = "Category deleted"
    override val categoryDeactivated = "Category deactivated"
    override val noCategories = "No categories found"
    override val selectCategory = "Select a category"
    override val allCategories = "All Categories"
    override val uncategorized = "Uncategorized"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Products"
    override val product = "Product"
    override val productName = "Product Name"
    override val addProduct = "Add Product"
    override val editProduct = "Edit Product"
    override val deleteProduct = "Delete Product"
    override val deleteProductConfirm = "Are you sure you want to delete this product?"
    override val productDeleted = "Product deleted"
    override val productDeactivated = "Product deactivated"
    override val noProducts = "No products found"
    override val selectProduct = "Select a product"
    override val unit = "Unit"
    override val unitVolume = "Unit Volume"
    override val description = "Description"
    override val minStock = "Minimum Stock"
    override val maxStock = "Maximum Stock"
    override val currentStock = "Current Stock"
    override val price = "Price"
    override val purchasePrice = "Purchase Price"
    override val sellingPrice = "Selling Price"
    override val margin = "Margin"
    override val marginType = "Margin Type"
    override val marginValue = "Margin Value"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Customers"
    override val customer = "Customer"
    override val customerName = "Customer Name"
    override val addCustomer = "Add Customer"
    override val editCustomer = "Edit Customer"
    override val deleteCustomer = "Delete Customer"
    override val deleteCustomerConfirm = "Are you sure you want to delete this customer?"
    override val customerDeleted = "Customer deleted"
    override val customerDeactivated = "Customer deactivated"
    override val noCustomers = "No customers found"
    override val selectCustomer = "Select a customer"
    override val phone = "Phone"
    override val email = "Email"
    override val address = "Address"
    override val notes = "Notes"
    override val walkInCustomer = "Walk-in Customer"

    // ============================================
    // SUPPLIERS
    // ============================================
    override val suppliers = "Suppliers"
    override val addSupplier = "Add Supplier"
    override val editSupplier = "Edit Supplier"
    override val deleteSupplier = "Delete Supplier"
    override val deleteSupplierConfirm = "Are you sure you want to delete this supplier?"
    override val supplierDeleted = "Supplier deleted"
    override val supplierDeactivated = "Supplier deactivated"
    override val noSuppliers = "No suppliers found"
    override val selectSupplier = "Select a supplier"
    override val manageSuppliers = "Manage Suppliers"
    override val noSuppliersMessage = "Add your first supplier to get started"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Purchases"
    override val purchase = "Purchase"
    override val newPurchase = "New Purchase"
    override val addPurchase = "Add Purchase"
    override val purchaseHistory = "Purchase History"
    override val supplier = "Supplier"
    override val supplierName = "Supplier Name"
    override val batchNumber = "Batch Number"
    override val purchaseDate = "Purchase Date"
    override val expiryDate = "Expiry Date"
    override val expiryDateOptional = "Expiry Date (optional)"
    override val quantity = "Quantity"
    override val initialQuantity = "Initial Quantity"
    override val remainingQuantity = "Remaining Quantity"
    override val totalAmount = "Total Amount"
    override val purchaseRecorded = "Purchase recorded"
    override val unitPurchasePrice = "Unit Purchase Price"
    override val unitSellingPrice = "Unit Selling Price"
    override val marginCalculatedAuto = "Margin: calculated automatically"
    override val sellingPriceNote = "The selling price is calculated automatically based on the product's margin, but you can modify it."
    override val savePurchase = "Save Purchase"
    override val enterSupplierName = "Enter supplier name"
    override val batchNumberExample = "Ex: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Sales"
    override val sale = "Sale"
    override val newSale = "New Sale"
    override val saleHistory = "Sales History"
    override val saleDate = "Sale Date"
    override val saleTotal = "Sale Total"
    override val saleItems = "Sale Items"
    override val addItem = "Add Item"
    override val removeItem = "Remove Item"
    override val unitPrice = "Unit Price"
    override val itemTotal = "Item Total"
    override val subtotal = "Subtotal"
    override val discount = "Discount"
    override val grandTotal = "Grand Total"
    override val completeSale = "Complete Sale"
    override val saleCompleted = "Sale completed"
    override val noSaleItems = "No items in sale"
    override val insufficientStock = "Insufficient stock for {product}: {available} available, {requested} requested"
    override val remainingQuantityNeeded = "Remaining quantity needed: {quantity} units"
    override val editSale = "Edit Sale"
    override val editPurchase = "Edit Purchase"
    override val productsToSell = "Products to Sell"
    override val addProductToSale = "+ Add Product"
    override val enterCustomerName = "Enter customer name"
    override val pricePerUnit = "Price per unit"
    override val exampleQuantity = "Ex: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Inventory"
    override val inventoryCount = "Inventory Count"
    override val startInventory = "Start Inventory"
    override val completeInventory = "Complete Inventory"
    override val inventoryInProgress = "Inventory in Progress"
    override val theoreticalQuantity = "Theoretical Quantity"
    override val countedQuantity = "Counted Quantity"
    override val discrepancy = "Discrepancy"
    override val reason = "Reason"
    override val inventoryCompleted = "Inventory completed"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Transfers"
    override val transfer = "Transfer"
    override val newTransfer = "New Transfer"
    override val transferHistory = "Transfer History"
    override val fromSite = "From Site"
    override val toSite = "To Site"
    override val transferStatus = "Transfer Status"
    override val transferPending = "Pending"
    override val transferCompleted = "Completed"
    override val transferCancelled = "Cancelled"
    override val completeTransfer = "Complete Transfer"
    override val cancelTransfer = "Cancel Transfer"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Stock"
    override val stockMovements = "Stock Movements"
    override val stockIn = "Stock In"
    override val stockOut = "Stock Out"
    override val stockAdjustment = "Stock Adjustment"
    override val movementType = "Movement Type"
    override val movementDate = "Movement Date"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Reports"
    override val salesReport = "Sales Report"
    override val stockReport = "Stock Report"
    override val profitReport = "Profit Report"
    override val exportReport = "Export Report"
    override val dateRange = "Date Range"
    override val startDate = "Start Date"
    override val endDate = "End Date"
    override val generateReport = "Generate Report"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Profile"
    override val myProfile = "My Profile"
    override val information = "Information"
    override val currentPassword = "Current Password"
    override val newPassword = "New Password"
    override val confirmPassword = "Confirm Password"
    override val passwordsDoNotMatch = "Passwords do not match"
    override val passwordChangedSuccessfully = "Password changed successfully"
    override val userNotFound = "User not found"
    override val incorrectPassword = "Current password incorrect"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Synced"
    override val pendingChanges = "{count} pending change(s)"
    override val conflictsToResolve = "{count} conflict(s) to resolve"
    override val online = "Online"
    override val offline = "Offline"
    override val realtimeConnected = "Realtime connected"
    override val realtimeDisconnected = "Realtime disconnected"
    override val lastError = "Last error"
    override val offlineMode = "Offline mode"
    override val conflictsDetected = "Conflicts detected"
    override val changesWillSyncWhenOnline = "Changes will sync when you're back online"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Settings"
    override val language = "Language"
    override val selectLanguage = "Select Language"
    override val theme = "Theme"
    override val darkMode = "Dark Mode"
    override val lightMode = "Light Mode"
    override val systemDefault = "System Default"
    override val about = "About"
    override val version = "Version"
    override val syncSettings = "Sync Settings"
    override val lastSync = "Last Sync"
    override val syncNow = "Sync Now"
    override val syncing = "Syncing..."
    override val syncSuccess = "Sync completed"
    override val syncError = "Sync failed"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Users"
    override val user = "User"
    override val addUser = "Add User"
    override val editUser = "Edit User"
    override val deleteUser = "Delete User"
    override val fullName = "Full Name"
    override val role = "Role"
    override val admin = "Administrator"
    override val permissions = "Permissions"
    override val canView = "Can View"
    override val canCreate = "Can Create"
    override val canEdit = "Can Edit"
    override val canDelete = "Can Delete"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Packaging Types"
    override val packagingType = "Packaging Type"
    override val addPackagingType = "Add Packaging Type"
    override val editPackagingType = "Edit Packaging Type"
    override val level1Name = "Level 1 Name"
    override val level2Name = "Level 2 Name"
    override val level2Quantity = "Level 2 Quantity"
    override val conversionFactor = "Conversion Factor"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} is required"
    override val invalidEmail = "Invalid email address"
    override val invalidPhone = "Invalid phone number"
    override val valueTooShort = "{field} must be at least {min} characters"
    override val valueTooLong = "{field} must not exceed {max} characters"
    override val valueMustBePositive = "Value must be positive"
    override val passwordTooShort = "Password must be at least 8 characters"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "At least 8 characters"
    override val passwordNeedsUppercase = "At least one uppercase letter (A-Z)"
    override val passwordNeedsLowercase = "At least one lowercase letter (a-z)"
    override val passwordNeedsDigit = "At least one digit (0-9)"
    override val passwordNeedsSpecial = "At least one special character (!@#\$%...)"
    override val passwordStrengthWeak = "Weak"
    override val passwordStrengthMedium = "Medium"
    override val passwordStrengthStrong = "Strong"
    override val passwordRequirements = "Password requirements:"
    override val passwordStrength = "Password strength:"
    override val passwordMustBeDifferent = "New password must be different from current password"
    override val usernameAlreadyExists = "Username already exists"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Cannot delete"
    override val entityInUse = "This {entity} is used in {count} records"
    override val deactivateInstead = "Would you like to deactivate it instead?"
    override val deactivate = "Deactivate"
    override val reactivate = "Reactivate"
    override val showInactive = "Show Inactive"
    override val hideInactive = "Hide Inactive"
    override val inactive = "Inactive"
    override val active = "Active"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val thisWeek = "This Week"
    override val thisMonth = "This Month"
    override val thisYear = "This Year"
    override val dateFormat = "MM/dd/yyyy"
    override val timeFormat = "HH:mm"

    // ============================================
    // NUMBERS & CURRENCY
    // ============================================
    override val currencySymbol = "$"
    override val currencyFormat = "{symbol}{amount}"
    override val decimalSeparator = "."
    override val thousandsSeparator = ","

    // ============================================
    // NOTIFICATIONS
    // ============================================
    override val notificationSettings = "Notification Settings"
    override val notificationExpiryAlerts = "Expiry Alerts"
    override val notificationEnableExpiry = "Enable expiry notifications"
    override val notificationWarningDays = "Warning days before expiry"
    override val notificationExpiryDescription = "Receive alerts when products are about to expire"
    override val notificationLowStockAlerts = "Low Stock Alerts"
    override val notificationEnableLowStock = "Enable low stock notifications"
    override val notificationLowStockDescription = "Receive alerts when stock falls below minimum threshold"
    override val notificationInvalidDays = "Please enter a valid number of days (1-365)"
    override val settingsSaved = "Settings saved successfully"
    override val supabaseNotConfigured = "Supabase is not configured"
    override val notifications = "Notifications"
    override val noNotifications = "No notifications"
    override val dismissAll = "Dismiss All"
    override val allNotificationsDismissed = "All notifications have been dismissed"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Current Site"
    override val operations = "Operations"
    override val purchaseProducts = "Purchase Products"
    override val sellProducts = "Sell Products"
    override val transferProducts = "Transfer Products"
    override val viewStock = "View Stock"
    override val inventoryStock = "Inventory Stock"
    override val administration = "Administration"
    override val management = "Management"
    override val siteManagement = "Site Management"
    override val manageProducts = "Manage Products"
    override val manageCategories = "Manage Categories"
    override val manageCustomers = "Manage Customers"
    override val userManagement = "User Management"
    override val history = "History"
    override val configuration = "Configuration"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Exhausted"
    override val remainingQty = "Remaining Qty"
    override val noPurchases = "No purchases found"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "No sales found"
    override val saleDetails = "Sale Details"
    override val items = "Items"
    override val total = "Total"
    override val date = "Date"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Inventories"
    override val noInventories = "No inventories found"
    override val inProgress = "In Progress"
    override val completed = "Completed"
    override val pending = "Pending"
    override val newInventory = "New Inventory"
    override val start = "Start"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "No transfers found"
    override val sourceSite = "Source Site"
    override val destinationSite = "Destination Site"
    override val quantityToTransfer = "Quantity to Transfer"
    override val create = "Create"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "No stock data"
    override val summary = "Summary"
    override val outOfStock = "Out of Stock"
    override val stockByProduct = "Stock by Product"
    override val noMovements = "No movements found"
    override val availableStock = "Available Stock"
    override val preview = "Preview"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "No packaging types found"
    override val addLevel2 = "Add Level 2"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Audit History"
    override val noHistory = "No history found"
    override val filterBy = "Filter by"
    override val all = "All"
    override val created = "Created"
    override val updated = "Updated"
    override val deleted = "Deleted"
    override val details = "Details"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Supabase Configuration"
    override val projectUrl = "Project URL"
    override val anonKey = "Anon Key"
    override val synchronization = "Synchronization"
    override val syncData = "Sync Data"
    override val currentStatus = "Current Status"
    override val configured = "Configured"
    override val connection = "Connection"
    override val testConnection = "Test Connection"
    override val clearConfiguration = "Clear Configuration"
    override val configSaved = "Configuration saved"
    override val syncCompleted = "Sync completed successfully"
    override val connectionSuccessful = "Connection successful"
    override val howToGetInfo = "How to get this information:"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Configure Supabase"
    override val authentication = "Authentication"
    override val enterCredentials = "Please enter your credentials"
    override val invalidPassword = "Invalid password"
    override val accountDisabled = "This account is disabled"
    override val connectionError = "Connection error"
    override val firstLoginRequiresInternet = "First login requires an internet connection"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "View"
    override val select = "Select"
    override val chooseProduct = "Choose a product"
    override val orSelect = "Or select"
    override val enable = "Enable"
    override val later = "Later"
    override val alertsDescription = "Notifications alert you about expired products and low stock."
    override val justNow = "Just now"
    override val minutesAgo = "{count} min ago"
    override val hoursAgo = "{count} h ago"
    override val daysAgo = "{count} day(s) ago"
    override val critical = "Critical"
    override val urgent = "Urgent"
    override val info = "Info"
    override val low = "Low"
    override val nearestExpiry = "Nearest Expiry"
    override val lots = "lot(s)"
    override val addNote = "Add a note..."
    override val saving = "Saving..."
    override val continue_ = "Continue"
    override val unknownSite = "Unknown site"
    override val unknownProduct = "Unknown product"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Add your first product to get started"
    override val noCustomersMessage = "Add your first customer to get started"
    override val noCategoriesMessage = "Add your first category to get started"
    override val noSitesMessage = "Add your first site to get started"
    override val noPackagingTypesMessage = "Add your first packaging type to get started"
    override val noInventoriesMessage = "No inventory counts have been performed yet"
    override val noSalesMessage = "No sales have been recorded yet"
    override val noPurchasesMessage = "No purchases have been recorded yet"
    override val noTransfersMessage = "No transfers have been recorded yet"
    override val noStockMessage = "No stock available"
    override val noMovementsMessage = "No stock movements recorded"
    override val noUsersMessage = "Add your first user to get started"
    override val historyWillAppearHere = "Audit history will appear here"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Add a site first"
    override val none = "None"
    override val stockAlerts = "Stock Alerts"
    override val stockAlertDescription = "Set minimum and maximum stock levels to receive alerts"
    override val transferIn = "Transfer In"
    override val transferOut = "Transfer Out"
    override val batches = "batches"
    override val noUsers = "No Users"
    override val adminHasAllPermissions = "Administrators have all permissions"
    override val create_ = "Create"
    override val selectSourceSiteFirst = "Select source site first"
    override val entries = "entries"
    override val optional = "optional"
    override val packagingTypeName = "Packaging type name"
    override val started = "Started"
    override val saleItem = "Sale Item"
    override val purchaseBatch = "Purchase Batch"
    override val stockMovement = "Stock Movement"
    override val supabaseStep1 = "1. Go to supabase.com and create an account"
    override val supabaseStep2 = "2. Create a new project"
    override val supabaseStep3 = "3. Go to Project Settings > API to find your URL and anon key"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Update Required"
    override val updateAvailable = "Update Available"
    override val appVersionIncompatible = "Your app version is not compatible with the database. Please update the app to continue."
    override val appVersion = "App version"
    override val minimumRequiredVersion = "Minimum required version"
    override val databaseVersion = "Database version"
    override val toUpdate = "To update"
    override val contactAdminForUpdate = "Contact your administrator to get the latest version of the app."
    override val checkingCompatibility = "Checking compatibility..."
    override val download = "Download"
    override val newVersionAvailable = "A new version of MediStock is available."
    override val currentVersionLabel = "Current version"
    override val newVersionLabel = "New version"
    override val whatsNew = "What's new"
    override val unableToLoadNotifications = "Unable to load notifications"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "App Settings"
    override val currencySymbolSetting = "Currency Symbol"
    override val currencySymbolDescription = "Symbol used to display prices (e.g., F, $, EUR)"
    override val settingsSavedSuccessfully = "Settings saved successfully"
    override val invalidCurrencySymbol = "Please enter a valid currency symbol"
    override val debugMode = "Debug Mode"
    override val debugModeDescription = "Enable verbose logging for troubleshooting"
}
