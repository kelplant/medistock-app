 package com.medistock.shared.i18n

/**
 * Bemba strings implementation.
 * Note: Bemba terminology can vary by region; these are practical UI translations.
 */
object StringsBm : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Lekesha"
    override val save = "Sunga"
    override val delete = "Fumyapo"
    override val edit = "Lunganya"
    override val add = "Onjelela"
    override val search = "Fwaya"
    override val loading = "Kuleeta..."
    override val error = "Ifyaposa"
    override val success = "Bwino"
    override val warning = "Cinshi ca kusamala"
    override val confirm = "Kufikila"
    override val yes = "Ee"
    override val no = "Awe"
    override val close = "Funga"
    override val back = "Bwelako"
    override val next = "Londoloka"
    override val retry = "Yesha na kabili"
    override val noData = "Takuli data"
    override val required = "Fyafunika"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Ukwingila"
    override val username = "Ishina lya mukonshi"
    override val password = "Akapasiwedi"
    override val login = "Ingila"
    override val logout = "Fuma"
    override val logoutConfirm = "Bushe ulefwaya ukufuma?"
    override val changePassword = "Cinja akapasiwedi"
    override val loginError = "Ukwingila kwalipona"
    override val loginErrorInvalidCredentials = "Ishina nangu akapasiwedi fyalilubana"
    override val welcomeBack = "Mwaiseni nakabili, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Pa kubala"
    override val dashboard = "Dashboard"
    override val quickActions = "Ifyakucita bwangu"
    override val recentActivity = "Ifyacitika nomba line"
    override val todaySales = "Ifyakushitisha lero"
    override val lowStock = "Ifintu fyanaka"
    override val pendingTransfers = "Ifyo kulindila ukutwala"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Ama site"
    override val site = "Site"
    override val siteName = "Ishina lya site"
    override val addSite = "Onjelela site"
    override val editSite = "Lunganya site"
    override val deleteSite = "Fumyapo site"
    override val deleteSiteConfirm = "Bushe ulefwaya ukufumyapo iyi site?"
    override val siteDeleted = "Site yafuminwapo"
    override val siteDeactivated = "Site yalekeshiwe"
    override val noSites = "Tapali ama site"
    override val selectSite = "Sankapo site"
    override val allSites = "Ama site yonse"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Imipasho"
    override val category = "Umupasho"
    override val categoryName = "Ishina lya mupasho"
    override val addCategory = "Onjelela umupasho"
    override val editCategory = "Lunganya umupasho"
    override val deleteCategory = "Fumyapo umupasho"
    override val deleteCategoryConfirm = "Bushe ulefwaya ukufumyapo uyu mupasho?"
    override val categoryDeleted = "Umupasho wafuminwapo"
    override val categoryDeactivated = "Umupasho walekeshiwe"
    override val noCategories = "Tapali imipasho"
    override val selectCategory = "Sankapo umupasho"
    override val allCategories = "Imipasho yonse"
    override val uncategorized = "Tafile mu mupasho"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Ifintu"
    override val product = "Chintu"
    override val productName = "Ishina lya chintu"
    override val addProduct = "Onjelela chintu"
    override val editProduct = "Lunganya chintu"
    override val deleteProduct = "Fumyapo chintu"
    override val deleteProductConfirm = "Bushe ulefwaya ukufumyapo ichi chintu?"
    override val productDeleted = "Chintu chafuminwapo"
    override val productDeactivated = "Chintu chalekeshiwe"
    override val noProducts = "Takuli ifintu"
    override val selectProduct = "Sankapo chintu"
    override val unit = "Unit"
    override val unitVolume = "Ubunini bwa unit"
    override val description = "Ukulondolola"
    override val minStock = "Ifintu fyacepa sana"
    override val maxStock = "Ifintu fyafula sana"
    override val currentStock = "Ifintu ifilipo"
    override val price = "Mutengo"
    override val purchasePrice = "Mutengo wakugula"
    override val sellingPrice = "Mutengo wakushitisha"
    override val margin = "Margin"
    override val marginType = "Ubwina bwa margin"
    override val marginValue = "Umubala wa margin"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Abashita"
    override val customer = "Uwanshita"
    override val customerName = "Ishina lya uwanshita"
    override val addCustomer = "Onjelela uwanshita"
    override val editCustomer = "Lunganya uwanshita"
    override val deleteCustomer = "Fumyapo uwanshita"
    override val deleteCustomerConfirm = "Bushe ulefwaya ukufumyapo uyu uwanshita?"
    override val customerDeleted = "Uwanshita afuminwapo"
    override val customerDeactivated = "Uwanshita alekeshilwe"
    override val noCustomers = "Tapali abashita"
    override val selectCustomer = "Sankapo uwanshita"
    override val phone = "Foni"
    override val email = "Email"
    override val address = "Address"
    override val notes = "Ifyo walemba"
    override val walkInCustomer = "Uwanshita wa ku njira"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Ifyo wagula"
    override val purchase = "Ukugula"
    override val newPurchase = "Ukugula ukupya"
    override val addPurchase = "Onjelela ukugula"
    override val purchaseHistory = "Mbiri ya kugula"
    override val supplier = "Uupereka ifintu"
    override val supplierName = "Ishina lya uupereka ifintu"
    override val batchNumber = "Nombala ya batch"
    override val purchaseDate = "Ubushiku bwa kugula"
    override val expiryDate = "Ubushiku bwo kufwa"
    override val expiryDateOptional = "Ubushiku bwo kufwa (optional)"
    override val quantity = "Ubwingi"
    override val initialQuantity = "Ubwingi bwo kutendeka"
    override val remainingQuantity = "Ubwingi ubushele"
    override val totalAmount = "Total"
    override val purchaseRecorded = "Ukugula kwandikwe"
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
    override val sales = "Ifyakushitisha"
    override val sale = "Ukushitisha"
    override val newSale = "Ukushitisha ukupya"
    override val saleHistory = "Mbiri ya kushitisha"
    override val saleDate = "Ubushiku bwa kushitisha"
    override val saleTotal = "Total ya kushitisha"
    override val saleItems = "Ifintu mu kushitisha"
    override val addItem = "Onjelela chintu"
    override val removeItem = "Fumyapo chintu"
    override val unitPrice = "Mutengo pa unit"
    override val itemTotal = "Total ya chintu"
    override val subtotal = "Subtotal"
    override val discount = "Discount"
    override val grandTotal = "Total yonse"
    override val completeSale = "Malizya ukushitisha"
    override val saleCompleted = "Ukushitisha kwamalizya"
    override val noSaleItems = "Tapali ifintu mu kushitisha"
    override val insufficientStock = "Ifintu ficepe pali {product}: {available} ilipo, {requested} ilefwayika"
    override val remainingQuantityNeeded = "Ubwingi ubufwile: {quantity} units"
    override val editSale = "Lungika ukushitisha"
    override val editPurchase = "Lungika ukugula"
    override val productsToSell = "Ifintu fya kushitisha"
    override val addProductToSale = "+ Onjelela chintu"
    override val enterCustomerName = "Lemba ishina lya customer"
    override val pricePerUnit = "Mutengo pa unit"
    override val exampleQuantity = "Ex: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Ukubala ifintu"
    override val inventoryCount = "Ukupenda ifintu"
    override val startInventory = "Tendeka ukupenda"
    override val completeInventory = "Malizya ukupenda"
    override val inventoryInProgress = "Ukupenda kuli mu mulimo"
    override val theoreticalQuantity = "Ubwingi bwa mu mabuku"
    override val countedQuantity = "Ubwingi bwabalilwe"
    override val discrepancy = "Ubulubilo"
    override val reason = "Insambu"
    override val inventoryCompleted = "Ukupenda kwamalizya"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Ukutwala"
    override val transfer = "Ukutwala"
    override val newTransfer = "Ukutwala ukupya"
    override val transferHistory = "Mbiri ya kutwala"
    override val fromSite = "Ukufuma ku site"
    override val toSite = "Ukutwala ku site"
    override val transferStatus = "Status ya kutwala"
    override val transferPending = "Ilikalolela"
    override val transferCompleted = "Yamalizya"
    override val transferCancelled = "Yalekeshiwe"
    override val completeTransfer = "Malizya ukutwala"
    override val cancelTransfer = "Lekesha ukutwala"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Ifintu ilipo"
    override val stockMovements = "Ifyakushintilila ifintu"
    override val stockIn = "Ifintu ukwingila"
    override val stockOut = "Ifintu ukufuma"
    override val stockAdjustment = "Ukulunganya ifintu"
    override val movementType = "Ubwina bwa movement"
    override val movementDate = "Ubushiku bwa movement"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Amalipoti"
    override val salesReport = "Lipoti lya kushitisha"
    override val stockReport = "Lipoti lya ifintu"
    override val profitReport = "Lipoti lya profit"
    override val exportReport = "Lipoti lya export"
    override val dateRange = "Inshiku shisankidwe"
    override val startDate = "Ubushiku bwo kutendeka"
    override val endDate = "Ubushiku bwo kumalilila"
    override val generateReport = "Panga lipoti"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Ubumi"
    override val myProfile = "Ubumi bwandi"
    override val information = "Imfundisho"
    override val currentPassword = "Akapasiwedi ka nomba"
    override val newPassword = "Akapasiwedi akashya"
    override val confirmPassword = "Tsimikishapo akapasiwedi"
    override val passwordsDoNotMatch = "Amapasiwedi tayalinga bwino"
    override val passwordChangedSuccessfully = "Akapasiwedi kaalulwa bwino"
    override val userNotFound = "User tabasangwike"
    override val incorrectPassword = "Akapasiwedi ka nomba takali bwino"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Yasynca"
    override val pendingChanges = "{count} fyo kulindila ukushintilila"
    override val conflictsToResolve = "{count} ubulubilo bwo kulondolola"
    override val online = "Online"
    override val offline = "Offline"
    override val realtimeConnected = "Realtime yalipangwa"
    override val realtimeDisconnected = "Realtime tayalipangwa"
    override val lastError = "Ifyaposa fya nyuma"
    override val offlineMode = "Offline mode"
    override val conflictsDetected = "Conflicts detected"
    override val changesWillSyncWhenOnline = "Changes will sync when you're back online"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Ifyopangwa"
    override val language = "Ululimi"
    override val selectLanguage = "Sankapo ululimi"
    override val theme = "Theme"
    override val darkMode = "Dark mode"
    override val lightMode = "Light mode"
    override val systemDefault = "Ifyo foni ipanga"
    override val about = "Pa fintu"
    override val version = "Version"
    override val syncSettings = "Ifyopangwa fya sync"
    override val lastSync = "Sync ya kulekelesha"
    override val syncNow = "Sync none"
    override val syncing = "Kusynca..."
    override val syncSuccess = "Sync yamalizya"
    override val syncError = "Sync yalipona"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Aba user"
    override val user = "User"
    override val addUser = "Onjelela user"
    override val editUser = "Lunganya user"
    override val deleteUser = "Fumyapo user"
    override val fullName = "Ishina lyonse"
    override val role = "Role"
    override val admin = "Administrator"
    override val permissions = "Insambu"
    override val canView = "Akwata insambu shakumona"
    override val canCreate = "Akwata insambu shakupanga"
    override val canEdit = "Akwata insambu shakulunganya"
    override val canDelete = "Akwata insambu shakufumyapo"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Ubwina bwa packaging"
    override val packagingType = "Packaging type"
    override val addPackagingType = "Onjelela packaging type"
    override val editPackagingType = "Lunganya packaging type"
    override val level1Name = "Ishina lya level 1"
    override val level2Name = "Ishina lya level 2"
    override val level2Quantity = "Ubwingi bwa level 2"
    override val conversionFactor = "Conversion factor"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} fyafunika"
    override val invalidEmail = "Email tayalondoka"
    override val invalidPhone = "Nombala ya foni tayalondoka"
    override val valueTooShort = "{field} fyalefwaya nibumbi {min}"
    override val valueTooLong = "{field} nayipa sana {max}"
    override val valueMustBePositive = "Ubulamba bufwile ukuba bwabwino"
    override val passwordTooShort = "Akapasiwedi kafwile ukufika 6 characters"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Nibumbi 8 characters"
    override val passwordNeedsUppercase = "Kalata konse akakulu (A-Z)"
    override val passwordNeedsLowercase = "Kalata konse akanono (a-z)"
    override val passwordNeedsDigit = "Inombala imo (0-9)"
    override val passwordNeedsSpecial = "Icishibilo icisoselwe imo (!@#\$%...)"
    override val passwordStrengthWeak = "Ukufisama"
    override val passwordStrengthMedium = "Pakati"
    override val passwordStrengthStrong = "Ukosa"
    override val passwordRequirements = "Ifyafwaikwa ku password:"
    override val passwordStrength = "Amaka ya password:"
    override val passwordMustBeDifferent = "Password ipya ikafwile ukupusana ne ya kale"
    override val usernameAlreadyExists = "Username yalipo kale"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Tafilapalwa ukufumyapo"
    override val entityInUse = "Iyi {entity} ilakolwa mu {count} records"
    override val deactivateInstead = "Mwalefwaya ukuti ilekeshwe (deactivate)?"
    override val deactivate = "Lekesha"
    override val reactivate = "Bweleshapo"
    override val showInactive = "Langa ifyalekeshiwe"
    override val hideInactive = "Fisa ifyalekeshiwe"
    override val inactive = "Yalekeshiwe"
    override val active = "Ilikola"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Lelo"
    override val yesterday = "Mailo"
    override val thisWeek = "Iyi mpungu"
    override val thisMonth = "Uyu mwezi"
    override val thisYear = "Uyu mwaka"
    override val dateFormat = "dd/MM/yyyy"
    override val timeFormat = "HH:mm"

    // ============================================
    // NUMBERS & CURRENCY
    // ============================================
    override val currencySymbol = "ZK"
    override val currencyFormat = "{symbol}{amount}"
    override val decimalSeparator = "."
    override val thousandsSeparator = ","

    // ============================================
    // NOTIFICATIONS
    // ============================================
    override val notificationSettings = "Amashiwi ya notification"
    override val notificationExpiryAlerts = "Amashiwi ya kupwa"
    override val notificationEnableExpiry = "Senda amashiwi ya kupwa"
    override val notificationWarningDays = "Inshiku sha kushibilikisha kabla ya kupwa"
    override val notificationExpiryDescription = "Tambula amashiwi ilyo ifyuma filelela kupwa"
    override val notificationLowStockAlerts = "Amashiwi ya stock inono"
    override val notificationEnableLowStock = "Senda amashiwi ya stock inono"
    override val notificationLowStockDescription = "Tambula amashiwi ilyo stock yaponena panshi pa minimum"
    override val notificationInvalidDays = "Lemba inshiku ishisuma (1-365)"
    override val settingsSaved = "Amashiwi yasungwa bwino"
    override val supabaseNotConfigured = "Supabase taishilongeshiwe"
    override val notifications = "Amashiwi"
    override val noNotifications = "Tapali amashiwi"
    override val dismissAll = "Funga yonse"
    override val allNotificationsDismissed = "Amashiwi yonse yafungwa"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Site ya nomba"
    override val operations = "Imilimo"
    override val purchaseProducts = "Gula ifintu"
    override val sellProducts = "Shitisha ifintu"
    override val transferProducts = "Twala ifintu"
    override val viewStock = "Lola ifintu ilipo"
    override val inventoryStock = "Penda ifintu"
    override val administration = "Ubuntungwa"
    override val management = "Ubukonshi"
    override val siteManagement = "Ubukonshi bwa site"
    override val manageProducts = "Konsha ifintu"
    override val manageCategories = "Konsha imipasho"
    override val manageCustomers = "Konsha abashita"
    override val userManagement = "Ubukonshi bwa ba user"
    override val history = "Mbiri"
    override val configuration = "Ifyopangwa"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Fyapwa"
    override val remainingQty = "Ubwingi ubushele"
    override val noPurchases = "Tapali ifyo wagula"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "Tapali ifyakushitisha"
    override val saleDetails = "Ukulondolola ukushitisha"
    override val items = "Ifintu"
    override val total = "Total"
    override val date = "Ubushiku"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Ukupenda"
    override val noInventories = "Tapali ukupenda"
    override val inProgress = "Muli mu mulimo"
    override val completed = "Kwamalizya"
    override val pending = "Kulindila"
    override val newInventory = "Ukupenda ukupya"
    override val start = "Tendeka"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "Tapali ukutwala"
    override val sourceSite = "Site ya kufumako"
    override val destinationSite = "Site ya kutwala"
    override val quantityToTransfer = "Ubwingi bwo kutwala"
    override val create = "Panga"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "Tapali ifintu"
    override val summary = "Ukulondolola"
    override val outOfStock = "Fyapwa"
    override val stockByProduct = "Ifintu pa chintu"
    override val noMovements = "Tapali ifyakushintilila"
    override val availableStock = "Ifintu ifilipo"
    override val preview = "Lola kubalilapo"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "Tapali ubwina bwa packaging"
    override val addLevel2 = "Onjelela level 2"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Mbiri ya audit"
    override val noHistory = "Tapali mbiri"
    override val filterBy = "Salunganya na"
    override val all = "Fyonse"
    override val created = "Fyapangwa"
    override val updated = "Fyalulwa"
    override val deleted = "Fyafuminwapo"
    override val details = "Ukulondolola"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Ifyopangwa fya Supabase"
    override val projectUrl = "URL ya project"
    override val anonKey = "Anon key"
    override val synchronization = "Ukusynca"
    override val syncData = "Synca data"
    override val currentStatus = "Status ya nomba"
    override val configured = "Yapangwa"
    override val connection = "Ukulumikisha"
    override val testConnection = "Yesha ukulumikisha"
    override val clearConfiguration = "Fumyapo ifyopangwa"
    override val configSaved = "Ifyopangwa fyasungwa"
    override val syncCompleted = "Ukusynca kwamalizya"
    override val connectionSuccessful = "Ukulumikisha kwamalizya"
    override val howToGetInfo = "Ukusanga ifyi fyonse"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Panga Supabase"
    override val authentication = "Ukwingila"
    override val enterCredentials = "Lemba ifyakwingila"
    override val invalidPassword = "Akapasiwedi takali bwino"
    override val accountDisabled = "Account yalekeshiwe"
    override val connectionError = "Ifyaposa fya kulumikisha"
    override val firstLoginRequiresInternet = "Ukwingila kwa ntanshi kulefwaya internet"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "Lola"
    override val select = "Sankapo"
    override val chooseProduct = "Sankapo chintu"
    override val orSelect = "nangu sankapo"
    override val enable = "Suminisha"
    override val later = "Panuma"
    override val alertsDescription = "Tambula amashiwi pa fintu fyalefwaikwa ukulondolwako"
    override val justNow = "Paba line"
    override val minutesAgo = "minutes yapita"
    override val hoursAgo = "ma hour yapita"
    override val daysAgo = "inshiku shapita"
    override val critical = "Ifya mweo"
    override val urgent = "Ifya bwangu"
    override val info = "Imfundisho"
    override val low = "Icepeshe"
    override val nearestExpiry = "Ubushiku bwa kufwa bwa pepi"
    override val lots = "Ama lot"
    override val addNote = "Onjelela ifyo walemba"
    override val saving = "Kusunga..."
    override val continue_ = "Londoloka"
    override val unknownSite = "Site itaishibilwe"
    override val unknownProduct = "Chintu chitaishibilwe"

    // EMPTY STATE MESSAGES (English fallback for Bemba)
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
    override val updateRequired = "Mufwile ukucinja (Update)"
    override val updateAvailable = "Kuli update"
    override val appVersionIncompatible = "Iyi version tayalingana na database. Cinjeni pulogalamu."
    override val appVersion = "Version ya pulogalamu"
    override val minimumRequiredVersion = "Version ilefwaikwa"
    override val databaseVersion = "Version ya database"
    override val toUpdate = "Pa kucinja"
    override val contactAdminForUpdate = "Ebeni abakalamba ba pulogalamu pa version ipya."
    override val checkingCompatibility = "Iletala yamona..."
    override val download = "Download"
    override val newVersionAvailable = "Ipulogalamu ipya ya MediStock nailoneka."
    override val currentVersionLabel = "Version ulebomfya"
    override val newVersionLabel = "Version ipya"
    override val whatsNew = "Ifipya"
    override val unableToLoadNotifications = "Notifications fyafilwa"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Ifyopangwa fya pulogalamu"
    override val currencySymbolSetting = "Icizindikiro ca ndalama"
    override val currencySymbolDescription = "Icizindikiro ca kulangilapo imiteengo (naba: ZK, $)"
    override val settingsSavedSuccessfully = "Ifyopangwa fyasungwa bwino"
    override val invalidCurrencySymbol = "Lembeni icizindikiro calondoka"
}