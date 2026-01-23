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
    override val walkInCustomer = "Kasitomala wolowa mwachibadwa"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Zogula"
    override val purchase = "Kugula"
    override val addPurchase = "Onjezani kugula"
    override val purchaseHistory = "Mbiri ya kugula"
    override val supplier = "Wopereka katundu"
    override val supplierName = "Dzina la wopereka katundu"
    override val batchNumber = "Nambala ya batch"
    override val purchaseDate = "Tsiku logulira"
    override val expiryDate = "Tsiku lotha ntchito"
    override val quantity = "Kuchuluka"
    override val initialQuantity = "Kuchuluka koyambirira"
    override val remainingQuantity = "Kuchuluka kotsala"
    override val totalAmount = "Chiwerengero chonse"
    override val purchaseRecorded = "Kugula kwalembedwa"

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
    override val insufficientStock = "Katundu sayokwanira pa {product}: {available} ilipo, {requested} yafunsidwa"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Mndandanda wa katundu"
    override val inventoryCount = "Kuwerengera katundu"
    override val startInventory = "Yambani kuwerengera"
    override val completeInventory = "Malizitsani kuwerengera"
    override val inventoryInProgress = "Kuwerengera kukuchitika"
    override val theoreticalQuantity = "Kuchuluka mwa theory"
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
    override val dateRange = "Nthawi (Date range)"
    override val startDate = "Tsiku loyambira"
    override val endDate = "Tsiku lomaliza"
    override val generateReport = "Pangani lipoti"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Zosintha"
    override val language = "Chilankhulo"
    override val selectLanguage = "Sankhani chilankhulo"
    override val theme = "Theme"
    override val darkMode = "Dark mode"
    override val lightMode = "Light mode"
    override val systemDefault = "System default"
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
    override val admin = "Administrator"
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
    override val addPackagingType = "Onjezani mtundu wa packaging"
    override val editPackagingType = "Sinthani mtundu wa packaging"
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
    override val valueTooShort = "{field} iyenera kukhala osachepera {min} characters"
    override val valueTooLong = "{field} siyenera kupitirira {max} characters"
    override val valueMustBePositive = "Mtengo uyenera kukhala positive"
    override val passwordTooShort = "Mawu achinsinsi ayenera kukhala osachepera 6 characters"

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
}