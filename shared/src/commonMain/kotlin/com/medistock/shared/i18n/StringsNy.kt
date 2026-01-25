package com.medistock.shared.i18n

/**
 * Nyanja (Chichewa) strings implementation.
 * Note: wording can vary by country/region; these are practical UI translations.
 */
object StringsNy : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Lekani"
    override val save = "Sungani"
    override val delete = "Chotsani"
    override val edit = "Sinthani"
    override val add = "Onjezani"
    override val search = "Sakani"
    override val loading = "Ikutsegula..."
    override val error = "Vuto"
    override val success = "Zachita bwino"
    override val warning = "Chenjerani"
    override val confirm = "Tsimikizani"
    override val yes = "Inde"
    override val no = "Ayi"
    override val close = "Tsekani"
    override val back = "Bwererani"
    override val next = "Lotsatira"
    override val retry = "Yesaninso"
    override val noData = "Palibe deta"
    override val required = "Chofunika"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Lowani"
    override val username = "Dzina lolowera"
    override val password = "Mawu achinsinsi"
    override val login = "Lowani"
    override val logout = "Tulukani"
    override val logoutConfirm = "Mukutsimikiza kuti mukufuna kutuluka?"
    override val changePassword = "Sinthani mawu achinsinsi"
    override val loginError = "Kulephera kulowa"
    override val loginErrorInvalidCredentials = "Dzina lolowera kapena mawu achinsinsi si olondola"
    override val welcomeBack = "Takulandiraninso, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Kunyumba"
    override val dashboard = "Dashboard"
    override val quickActions = "Zochita mwachangu"
    override val recentActivity = "Zochitika posachedwa"
    override val todaySales = "Zogulitsa za lero"
    override val lowStock = "Katundu watsika"
    override val pendingTransfers = "Zosamutsa zikudikirira"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Malo"
    override val site = "Malo"
    override val siteName = "Dzina la malo"
    override val addSite = "Onjezani malo"
    override val editSite = "Sinthani malo"
    override val deleteSite = "Chotsani malo"
    override val deleteSiteConfirm = "Mukutsimikiza kuti mukufuna kuchotsa malo awa?"
    override val siteDeleted = "Malo achotsedwa"
    override val siteDeactivated = "Malo ayimitsidwa"
    override val noSites = "Palibe malo apezeka"
    override val selectSite = "Sankhani malo"
    override val allSites = "Malo onse"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Magulu"
    override val category = "Gulu"
    override val categoryName = "Dzina la gulu"
    override val addCategory = "Onjezani gulu"
    override val editCategory = "Sinthani gulu"
    override val deleteCategory = "Chotsani gulu"
    override val deleteCategoryConfirm = "Mukutsimikiza kuti mukufuna kuchotsa gulu ili?"
    override val categoryDeleted = "Gulu lachotsedwa"
    override val categoryDeactivated = "Gulu layimitsidwa"
    override val noCategories = "Palibe magulu apezeka"
    override val selectCategory = "Sankhani gulu"
    override val allCategories = "Magulu onse"
    override val uncategorized = "Osayikidwa m'gulu"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Zogulitsa"
    override val product = "Chinthu"
    override val productName = "Dzina la chinthu"
    override val addProduct = "Onjezani chinthu"
    override val editProduct = "Sinthani chinthu"
    override val deleteProduct = "Chotsani chinthu"
    override val deleteProductConfirm = "Mukutsimikiza kuti mukufuna kuchotsa chinthu ichi?"
    override val productDeleted = "Chinthu chachotsedwa"
    override val productDeactivated = "Chinthu chayimitsidwa"
    override val noProducts = "Palibe zinthu zapezeka"
    override val selectProduct = "Sankhani chinthu"
    override val unit = "Unit"
    override val unitVolume = "Voliyumu ya unit"
    override val description = "Kufotokozera"
    override val minStock = "Katundu wochepa"
    override val maxStock = "Katundu wochuluka"
    override val currentStock = "Katundu amene alipo"
    override val price = "Mtengo"
    override val purchasePrice = "Mtengo wogulira"
    override val sellingPrice = "Mtengo wogulitsa"
    override val margin = "Margin"
    override val marginType = "Mtundu wa margin"
    override val marginValue = "Mtengo wa margin"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Makasitomala"
    override val customer = "Kasitomala"
    override val customerName = "Dzina la kasitomala"
    override val addCustomer = "Onjezani kasitomala"
    override val editCustomer = "Sinthani kasitomala"
    override val deleteCustomer = "Chotsani kasitomala"
    override val deleteCustomerConfirm = "Mukutsimikiza kuti mukufuna kuchotsa kasitomala uyu?"
    override val customerDeleted = "Kasitomala wachotsedwa"
    override val customerDeactivated = "Kasitomala wayimitsidwa"
    override val noCustomers = "Palibe makasitomala apezeka"
    override val selectCustomer = "Sankhani kasitomala"
    override val phone = "Foni"
    override val email = "Imelo"
    override val address = "Adilesi"
    override val notes = "Zolemba"
    override val walkInCustomer = "Kasitomala wolowa"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Zogula"
    override val purchase = "Kugula"
    override val newPurchase = "Kugula kwatsopano"
    override val addPurchase = "Onjezani kugula"
    override val purchaseHistory = "Mbiri ya kugula"
    override val supplier = "Wopereka katundu"
    override val supplierName = "Dzina la wopereka katundu"
    override val batchNumber = "Nambala ya batch"
    override val purchaseDate = "Tsiku logulira"
    override val expiryDate = "Tsiku lotha ntchito"
    override val expiryDateOptional = "Tsiku lotha ntchito (osafunikira)"
    override val quantity = "Kuchuluka"
    override val initialQuantity = "Kuchuluka koyambirira"
    override val remainingQuantity = "Kuchuluka kotsala"
    override val totalAmount = "Chiwerengero chonse"
    override val purchaseRecorded = "Kugula kwalembedwa"
    override val unitPurchasePrice = "Mtengo wogulira pa unit"
    override val unitSellingPrice = "Mtengo wogulitsira pa unit"
    override val marginCalculatedAuto = "Margin: ikusankha yokha"
    override val sellingPriceNote = "Mtengo wogulitsira umasankha yokha malinga ndi margin ya katundu, koma mutha kusintha."
    override val savePurchase = "Sungani kugula"
    override val enterSupplierName = "Lembani dzina la wopereka katundu"
    override val batchNumberExample = "Mwachitsanzo: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Zogulitsa"
    override val sale = "Kugulitsa"
    override val newSale = "Kugulitsa kwatsopano"
    override val saleHistory = "Mbiri ya kugulitsa"
    override val saleDate = "Tsiku logulitsa"
    override val saleTotal = "Total ya kugulitsa"
    override val saleItems = "Zinthu zogulitsidwa"
    override val addItem = "Onjezani chinthu"
    override val removeItem = "Chotsani chinthu"
    override val unitPrice = "Mtengo pa unit"
    override val itemTotal = "Total ya chinthu"
    override val subtotal = "Subtotal"
    override val discount = "Discount"
    override val grandTotal = "Total yonse"
    override val completeSale = "Malizitsani kugulitsa"
    override val saleCompleted = "Kugulitsa kwamalizidwa"
    override val noSaleItems = "Palibe zinthu pa kugulitsa"
    override val insufficientStock = "Katundu sayokwanira pa {product}: {available} alipo, {requested} afunsidwa"
    override val remainingQuantityNeeded = "Kuchuluka kotsala kofunikira: {quantity} ma unit"
    override val editSale = "Sinthani kugulitsa"
    override val editPurchase = "Sinthani kugula"
    override val productsToSell = "Zinthu zogulitsa"
    override val addProductToSale = "+ Onjezani chinthu"
    override val enterCustomerName = "Lembani dzina la kasitomala"
    override val pricePerUnit = "Mtengo pa unit"
    override val exampleQuantity = "Mwachitsanzo: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Kuwerengera katundu"
    override val inventoryCount = "Mndandanda wa katundu"
    override val startInventory = "Yambani kuwerengera"
    override val completeInventory = "Malizitsani kuwerengera"
    override val inventoryInProgress = "Kuwerengera kukuchitika"
    override val theoreticalQuantity = "Kuchuluka koyembekezeka"
    override val countedQuantity = "Kuchuluka kowerengedwa"
    override val discrepancy = "Kusiyana"
    override val reason = "Chifukwa"
    override val inventoryCompleted = "Kuwerengera kwamalizidwa"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Zosamutsa"
    override val transfer = "Kusamutsa"
    override val newTransfer = "Kusamutsa kwatsopano"
    override val transferHistory = "Mbiri ya kusamutsa"
    override val fromSite = "Kuchokera ku malo"
    override val toSite = "Kupita ku malo"
    override val transferStatus = "Status ya kusamutsa"
    override val transferPending = "Ikudikirira"
    override val transferCompleted = "Yamalizidwa"
    override val transferCancelled = "Yaletsedwa"
    override val completeTransfer = "Malizitsani kusamutsa"
    override val cancelTransfer = "Letsani kusamutsa"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Katundu"
    override val stockMovements = "Kusuntha kwa katundu"
    override val stockIn = "Katundu wolowa"
    override val stockOut = "Katundu wotuluka"
    override val stockAdjustment = "Kukonza mndandanda"
    override val movementType = "Mtundu wa kusuntha"
    override val movementDate = "Tsiku la kusuntha"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Malipoti"
    override val salesReport = "Lipoti la kugulitsa"
    override val stockReport = "Lipoti la katundu"
    override val profitReport = "Lipoti la phindu"
    override val exportReport = "Lipoti la export"
    override val dateRange = "Nthawi ya masiku"
    override val startDate = "Tsiku loyambira"
    override val endDate = "Tsiku lomaliza"
    override val generateReport = "Pangani lipoti"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Mbiri"
    override val myProfile = "Mbiri yanga"
    override val information = "Zambiri"
    override val currentPassword = "Mawu achinsinsi apano"
    override val newPassword = "Mawu achinsinsi atsopano"
    override val confirmPassword = "Tsimikizani mawu achinsinsi"
    override val passwordsDoNotMatch = "Mawu achinsinsi sakufanana"
    override val passwordChangedSuccessfully = "Mawu achinsinsi asinthidwa bwino"
    override val userNotFound = "Wogwiritsa sanapezekedwa"
    override val incorrectPassword = "Mawu achinsinsi apano si olondola"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Yasync"
    override val pendingChanges = "{count} zosintha zikudikirira"
    override val conflictsToResolve = "{count} mkangano woti muthane nawo"
    override val online = "Pa intaneti"
    override val offline = "Opanda intaneti"
    override val realtimeConnected = "Realtime yaphatikizidwa"
    override val realtimeDisconnected = "Realtime sichinayambe"
    override val lastError = "Vuto lomaliza"
    override val offlineMode = "Njira yopanda intaneti"
    override val conflictsDetected = "Mapokoso apezeka"
    override val changesWillSyncWhenOnline = "Zosintha zidzasynkidwa mukabwerera pa intaneti"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Zosintha"
    override val language = "Chilankhulo"
    override val selectLanguage = "Sankhani chilankhulo"
    override val theme = "Theme"
    override val darkMode = "Dark mode"
    override val lightMode = "Light mode"
    override val systemDefault = "Zofuna za foni"
    override val about = "Za app"
    override val version = "Version"
    override val syncSettings = "Zosintha za sync"
    override val lastSync = "Sync yomaliza"
    override val syncNow = "Sync tsopano"
    override val syncing = "Ikusync..."
    override val syncSuccess = "Sync yatha"
    override val syncError = "Sync yalephera"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Ogwiritsa"
    override val user = "Wogwiritsa"
    override val addUser = "Onjezani wogwiritsa"
    override val editUser = "Sinthani wogwiritsa"
    override val deleteUser = "Chotsani wogwiritsa"
    override val fullName = "Dzina lonse"
    override val role = "Udindo"
    override val admin = "Woyang'anira"
    override val permissions = "Zilolezo"
    override val canView = "Atha kuwona"
    override val canCreate = "Atha kupanga"
    override val canEdit = "Atha kusintha"
    override val canDelete = "Atha kuchotsa"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Mitundu ya packaging"
    override val packagingType = "Mtundu wa packaging"
    override val addPackagingType = "Onjezani mtundu"
    override val editPackagingType = "Sinthani mtundu"
    override val level1Name = "Dzina la level 1"
    override val level2Name = "Dzina la level 2"
    override val level2Quantity = "Kuchuluka kwa level 2"
    override val conversionFactor = "Conversion factor"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} ndi chofunika"
    override val invalidEmail = "Imelo si yolondola"
    override val invalidPhone = "Nambala ya foni si yolondola"
    override val valueTooShort = "{field} iyenera kukhala osachepera {min}"
    override val valueTooLong = "{field} siyenera kupitirira {max}"
    override val valueMustBePositive = "Mtengo uyenera kukhala woposa zero"
    override val passwordTooShort = "Mawu achinsinsi ayenera kukhala osachepera 6 characters"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Osachepera zilembo 8"
    override val passwordNeedsUppercase = "Kamodzi lilembo lalikulu (A-Z)"
    override val passwordNeedsLowercase = "Kamodzi lilembo laling'ono (a-z)"
    override val passwordNeedsDigit = "Kamodzi nambala (0-9)"
    override val passwordNeedsSpecial = "Kamodzi chizindikiro chapadera (!@#\$%...)"
    override val passwordStrengthWeak = "Yofooka"
    override val passwordStrengthMedium = "Yapakati"
    override val passwordStrengthStrong = "Yamphamvu"
    override val passwordRequirements = "Zofunikira za password:"
    override val passwordStrength = "Mphamvu ya password:"
    override val passwordMustBeDifferent = "Password yatsopano iyenera kusiyana ndi yakale"
    override val usernameAlreadyExists = "Dzina logwiritsira ntchito lilipo kale"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Sitingachotse"
    override val entityInUse = "{entity} iyi ikugwiritsidwa ntchito mu zolemba {count}"
    override val deactivateInstead = "Mukufuna kuyimitsa m'malo mochotsa?"
    override val deactivate = "Yimitsani"
    override val reactivate = "Yambitsaninso"
    override val showInactive = "Onetsani zoyimitsidwa"
    override val hideInactive = "Bisani zoyimitsidwa"
    override val inactive = "Yoyimitsidwa"
    override val active = "Yogwira"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Lero"
    override val yesterday = "Dzulo"
    override val thisWeek = "Sabata ino"
    override val thisMonth = "Mwezi uno"
    override val thisYear = "Chaka chino"
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
    override val notificationSettings = "Zosintha za zidziwitso"
    override val notificationExpiryAlerts = "Zidziwitso za kutha"
    override val notificationEnableExpiry = "Yatsani zidziwitso za kutha"
    override val notificationWarningDays = "Masiku ochenjeza asanafike kutha"
    override val notificationExpiryDescription = "Landirani zidziwitso katundu akuyandikira kutha"
    override val notificationLowStockAlerts = "Zidziwitso za stock yotsika"
    override val notificationEnableLowStock = "Yatsani zidziwitso za stock yotsika"
    override val notificationLowStockDescription = "Landirani zidziwitso stock ikatsikira pansi pa minimum"
    override val notificationInvalidDays = "Lembani masiku oyenera (1-365)"
    override val settingsSaved = "Zosintha zasungidwa bwino"
    override val supabaseNotConfigured = "Supabase sinakonzedwe"
    override val notifications = "Zidziwitso"
    override val noNotifications = "Palibe zidziwitso"
    override val dismissAll = "Chotsani zonse"
    override val allNotificationsDismissed = "Zidziwitso zonse zachotsedwa"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Malo apano"
    override val operations = "Ntchito"
    override val purchaseProducts = "Gulani zinthu"
    override val sellProducts = "Gulitsani zinthu"
    override val transferProducts = "Samutsani zinthu"
    override val viewStock = "Onani katundu"
    override val inventoryStock = "Werengani katundu"
    override val administration = "Ulamuliro"
    override val management = "Kasamalidwe"
    override val siteManagement = "Kasamalidwe ka malo"
    override val manageProducts = "Samalani zinthu"
    override val manageCategories = "Samalani magulu"
    override val manageCustomers = "Samalani makasitomala"
    override val userManagement = "Kasamalidwe ka ogwiritsa"
    override val history = "Mbiri"
    override val configuration = "Zosintha"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Yatha"
    override val remainingQty = "Kuchuluka kotsala"
    override val noPurchases = "Palibe zogula"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "Palibe zogulitsa"
    override val saleDetails = "Zambiri za kugulitsa"
    override val items = "Zinthu"
    override val total = "Chiwerengero"
    override val date = "Tsiku"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Kuwerengera"
    override val noInventories = "Palibe kuwerengera"
    override val inProgress = "Ikuchitika"
    override val completed = "Yamalizidwa"
    override val pending = "Ikudikirira"
    override val newInventory = "Kuwerengera kwatsopano"
    override val start = "Yambani"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "Palibe zosamutsa"
    override val sourceSite = "Malo ochokera"
    override val destinationSite = "Malo opita"
    override val quantityToTransfer = "Kuchuluka kosamutsa"
    override val create = "Pangani"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "Palibe katundu"
    override val summary = "Chidule"
    override val outOfStock = "Yatha"
    override val stockByProduct = "Katundu pa chinthu"
    override val noMovements = "Palibe kusuntha"
    override val availableStock = "Katundu wopezeka"
    override val preview = "Onetsani kaye"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "Palibe mitundu ya packaging"
    override val addLevel2 = "Onjezani level 2"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Mbiri ya audit"
    override val noHistory = "Palibe mbiri"
    override val filterBy = "Sefani na"
    override val all = "Zonse"
    override val created = "Yapangidwa"
    override val updated = "Yasinthidwa"
    override val deleted = "Yachotsedwa"
    override val details = "Zambiri"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Zosintha za Supabase"
    override val projectUrl = "URL ya project"
    override val anonKey = "Anon key"
    override val synchronization = "Kusync"
    override val syncData = "Sync deta"
    override val currentStatus = "Status yapano"
    override val configured = "Yakonzedwa"
    override val connection = "Kulumikizana"
    override val testConnection = "Yesani kulumikizana"
    override val clearConfiguration = "Chotsani zosintha"
    override val configSaved = "Zosintha zasungidwa"
    override val syncCompleted = "Sync yatha"
    override val connectionSuccessful = "Kulumikizana kwachita"
    override val howToGetInfo = "Momwe mungapezere zambiri"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Konzani Supabase"
    override val authentication = "Kutsimikizira"
    override val enterCredentials = "Lembani zambiri zolowera"
    override val invalidPassword = "Mawu achinsinsi si olondola"
    override val accountDisabled = "Account yayimitsidwa"
    override val connectionError = "Vuto la kulumikizana"
    override val firstLoginRequiresInternet = "Kulowa koyamba kumafuna intaneti"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "Onani"
    override val select = "Sankhani"
    override val chooseProduct = "Sankhani chinthu"
    override val orSelect = "kapena sankhani"
    override val enable = "Yatsani"
    override val later = "Pambuyo"
    override val alertsDescription = "Landirani zidziwitso pazinthu zofunika kutsatira"
    override val justNow = "Tsopano"
    override val minutesAgo = "mphindi zapitazo"
    override val hoursAgo = "maola apitawo"
    override val daysAgo = "masiku apitawo"
    override val critical = "Zamkulu"
    override val urgent = "Zachangu"
    override val info = "Zambiri"
    override val low = "Zochepera"
    override val nearestExpiry = "Tsiku lotha pafupi"
    override val lots = "Zinthu zambiri"
    override val addNote = "Onjezani cholemba"
    override val saving = "Ikusunga..."
    override val continue_ = "Pitirizani"
    override val unknownSite = "Malo osadziwika"
    override val unknownProduct = "Chinthu chosadziwika"

    // EMPTY STATE MESSAGES (English fallback for Chichewa)
    override val noProductsMessage = "Onjezani chinthu choyamba kuti muyambe"
    override val noCustomersMessage = "Onjezani kasitomala woyamba kuti muyambe"
    override val noCategoriesMessage = "Onjezani gulu loyamba kuti muyambe"
    override val noSitesMessage = "Onjezani malo oyamba kuti muyambe"
    override val noPackagingTypesMessage = "Onjezani mtundu wa packaging woyamba kuti muyambe"
    override val noInventoriesMessage = "Palibe kuwerengera kumene kwachitika"
    override val noSalesMessage = "Palibe zogulitsa zimene zalembedwa"
    override val noPurchasesMessage = "Palibe zogula zimene zalembedwa"
    override val noTransfersMessage = "Palibe zosamutsa zimene zalembedwa"
    override val noStockMessage = "Palibe katundu wopezeka"
    override val noMovementsMessage = "Palibe kusuntha kwa katundu kolembedwa"
    override val noUsersMessage = "Onjezani wogwiritsa woyamba kuti muyambe"
    override val historyWillAppearHere = "Mbiri ya audit idzaoneka pano"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Onjezani malo kaye"
    override val none = "Palibe"
    override val stockAlerts = "Zidziwitso za katundu"
    override val stockAlertDescription = "Ikani malire a katundu wochepa ndi wochuluka kuti mulandire zidziwitso"
    override val transferIn = "Kusamutsa kolowa"
    override val transferOut = "Kusamutsa kotuluka"
    override val batches = "ma loti"
    override val noUsers = "Palibe ogwiritsa"
    override val adminHasAllPermissions = "Oyang'anira ali ndi zilolezo zonse"
    override val create_ = "Pangani"
    override val selectSourceSiteFirst = "Sankhani malo ochokera kaye"
    override val entries = "zolemba"
    override val optional = "osafunikira"
    override val packagingTypeName = "Dzina la mtundu wa packaging"
    override val started = "Yayamba"
    override val saleItem = "Chinthu chogulitsa"
    override val purchaseBatch = "Loti logula"
    override val stockMovement = "Kusuntha kwa katundu"
    override val supabaseStep1 = "1. Pitani ku supabase.com ndikupanga akaunti"
    override val supabaseStep2 = "2. Pangani projekiti yatsopano"
    override val supabaseStep3 = "3. Pitani ku Project Settings > API kuti mupeze URL ndi anon key"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Kusintha kumafunikira"
    override val updateAvailable = "Kusintha kulipo"
    override val appVersionIncompatible = "Mtundu wa pulogalamu yanu sugwirizana ndi database. Chonde sinthani pulogalamu kuti mupitirize."
    override val appVersion = "Mtundu wa pulogalamu"
    override val minimumRequiredVersion = "Mtundu wochepa wofunikira"
    override val databaseVersion = "Mtundu wa database"
    override val toUpdate = "Kuti musinthe"
    override val contactAdminForUpdate = "Lankhulani ndi woyang'anira wanu kuti mupeze mtundu watsopano wa pulogalamu."
    override val checkingCompatibility = "Kuwunika kugwirizana..."
    override val download = "Tsitsani"
    override val newVersionAvailable = "Mtundu watsopano wa MediStock ulipo."
    override val currentVersionLabel = "Mtundu wapano"
    override val newVersionLabel = "Mtundu watsopano"
    override val whatsNew = "Zatsopano"
    override val unableToLoadNotifications = "Sitinathe kutsitsa zidziwitso"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Zokonda za pulogalamu"
    override val currencySymbolSetting = "Chizindikiro cha ndalama"
    override val currencySymbolDescription = "Chizindikiro chogwiritsidwa ntchito kuwonetsa mitengo (monga: F, $, EUR)"
    override val settingsSavedSuccessfully = "Zokonda zasungidwa bwino"
    override val invalidCurrencySymbol = "Chonde lowetsani chizindikiro cha ndalama chokhoza"
}