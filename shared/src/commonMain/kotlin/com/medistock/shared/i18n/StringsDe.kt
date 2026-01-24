package com.medistock.shared.i18n

/**
 * German strings implementation.
 */
object StringsDe : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Abbrechen"
    override val save = "Speichern"
    override val delete = "Löschen"
    override val edit = "Bearbeiten"
    override val add = "Hinzufügen"
    override val search = "Suchen"
    override val loading = "Laden..."
    override val error = "Fehler"
    override val success = "Erfolg"
    override val warning = "Warnung"
    override val confirm = "Bestätigen"
    override val yes = "Ja"
    override val no = "Nein"
    override val close = "Schließen"
    override val back = "Zurück"
    override val next = "Weiter"
    override val retry = "Erneut versuchen"
    override val noData = "Keine Daten verfügbar"
    override val required = "Erforderlich"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Anmeldung"
    override val username = "Benutzername"
    override val password = "Passwort"
    override val login = "Anmelden"
    override val logout = "Abmelden"
    override val logoutConfirm = "Sind Sie sicher, dass Sie sich abmelden möchten?"
    override val changePassword = "Passwort ändern"
    override val loginError = "Anmeldung fehlgeschlagen"
    override val loginErrorInvalidCredentials = "Ungültiger Benutzername oder Passwort"
    override val welcomeBack = "Willkommen zurück, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Startseite"
    override val dashboard = "Dashboard"
    override val quickActions = "Schnellaktionen"
    override val recentActivity = "Letzte Aktivität"
    override val todaySales = "Heutige Verkäufe"
    override val lowStock = "Niedriger Bestand"
    override val pendingTransfers = "Ausstehende Transfers"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Standorte"
    override val site = "Standort"
    override val siteName = "Standortname"
    override val addSite = "Standort hinzufügen"
    override val editSite = "Standort bearbeiten"
    override val deleteSite = "Standort löschen"
    override val deleteSiteConfirm = "Sind Sie sicher, dass Sie diesen Standort löschen möchten?"
    override val siteDeleted = "Standort gelöscht"
    override val siteDeactivated = "Standort deaktiviert"
    override val noSites = "Keine Standorte gefunden"
    override val selectSite = "Standort auswählen"
    override val allSites = "Alle Standorte"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Kategorien"
    override val category = "Kategorie"
    override val categoryName = "Kategoriename"
    override val addCategory = "Kategorie hinzufügen"
    override val editCategory = "Kategorie bearbeiten"
    override val deleteCategory = "Kategorie löschen"
    override val deleteCategoryConfirm = "Sind Sie sicher, dass Sie diese Kategorie löschen möchten?"
    override val categoryDeleted = "Kategorie gelöscht"
    override val categoryDeactivated = "Kategorie deaktiviert"
    override val noCategories = "Keine Kategorien gefunden"
    override val selectCategory = "Kategorie auswählen"
    override val allCategories = "Alle Kategorien"
    override val uncategorized = "Nicht kategorisiert"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Produkte"
    override val product = "Produkt"
    override val productName = "Produktname"
    override val addProduct = "Produkt hinzufügen"
    override val editProduct = "Produkt bearbeiten"
    override val deleteProduct = "Produkt löschen"
    override val deleteProductConfirm = "Sind Sie sicher, dass Sie dieses Produkt löschen möchten?"
    override val productDeleted = "Produkt gelöscht"
    override val productDeactivated = "Produkt deaktiviert"
    override val noProducts = "Keine Produkte gefunden"
    override val selectProduct = "Produkt auswählen"
    override val unit = "Einheit"
    override val unitVolume = "Einheitsvolumen"
    override val description = "Beschreibung"
    override val minStock = "Mindestbestand"
    override val maxStock = "Höchstbestand"
    override val currentStock = "Aktueller Bestand"
    override val price = "Preis"
    override val purchasePrice = "Einkaufspreis"
    override val sellingPrice = "Verkaufspreis"
    override val margin = "Marge"
    override val marginType = "Margentyp"
    override val marginValue = "Margenwert"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Kunden"
    override val customer = "Kunde"
    override val customerName = "Kundenname"
    override val addCustomer = "Kunde hinzufügen"
    override val editCustomer = "Kunde bearbeiten"
    override val deleteCustomer = "Kunde löschen"
    override val deleteCustomerConfirm = "Sind Sie sicher, dass Sie diesen Kunden löschen möchten?"
    override val customerDeleted = "Kunde gelöscht"
    override val customerDeactivated = "Kunde deaktiviert"
    override val noCustomers = "Keine Kunden gefunden"
    override val selectCustomer = "Kunde auswählen"
    override val phone = "Telefon"
    override val email = "E-Mail"
    override val address = "Adresse"
    override val notes = "Notizen"
    override val walkInCustomer = "Laufkunde"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Einkäufe"
    override val purchase = "Einkauf"
    override val newPurchase = "Neuer Einkauf"
    override val addPurchase = "Einkauf hinzufügen"
    override val purchaseHistory = "Einkaufshistorie"
    override val supplier = "Lieferant"
    override val supplierName = "Lieferantenname"
    override val batchNumber = "Chargennummer"
    override val purchaseDate = "Einkaufsdatum"
    override val expiryDate = "Ablaufdatum"
    override val expiryDateOptional = "Ablaufdatum (optional)"
    override val quantity = "Menge"
    override val initialQuantity = "Anfangsmenge"
    override val remainingQuantity = "Restmenge"
    override val totalAmount = "Gesamtbetrag"
    override val purchaseRecorded = "Einkauf erfasst"
    override val unitPurchasePrice = "Einkaufspreis pro Einheit"
    override val unitSellingPrice = "Verkaufspreis pro Einheit"
    override val marginCalculatedAuto = "Marge: automatisch berechnet"
    override val sellingPriceNote = "Der Verkaufspreis wird automatisch basierend auf der Produktmarge berechnet, kann aber geändert werden."
    override val savePurchase = "Einkauf speichern"
    override val enterSupplierName = "Lieferantenname eingeben"
    override val batchNumberExample = "Z.B.: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Verkäufe"
    override val sale = "Verkauf"
    override val newSale = "Neuer Verkauf"
    override val saleHistory = "Verkaufshistorie"
    override val saleDate = "Verkaufsdatum"
    override val saleTotal = "Verkaufssumme"
    override val saleItems = "Verkaufsartikel"
    override val addItem = "Artikel hinzufügen"
    override val removeItem = "Artikel entfernen"
    override val unitPrice = "Einzelpreis"
    override val itemTotal = "Artikelsumme"
    override val subtotal = "Zwischensumme"
    override val discount = "Rabatt"
    override val grandTotal = "Gesamtsumme"
    override val completeSale = "Verkauf abschließen"
    override val saleCompleted = "Verkauf abgeschlossen"
    override val noSaleItems = "Keine Artikel im Verkauf"
    override val insufficientStock = "Unzureichender Bestand für {product}: {available} verfügbar, {requested} angefordert"
    override val remainingQuantityNeeded = "Benötigte Restmenge: {quantity} Einheiten"
    override val editSale = "Verkauf bearbeiten"
    override val editPurchase = "Einkauf bearbeiten"
    override val productsToSell = "Zu verkaufende Produkte"
    override val addProductToSale = "+ Produkt hinzufügen"
    override val enterCustomerName = "Kundenname eingeben"
    override val pricePerUnit = "Preis pro Einheit"
    override val exampleQuantity = "Z.B.: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Inventar"
    override val inventoryCount = "Inventurzählung"
    override val startInventory = "Inventur starten"
    override val completeInventory = "Inventur abschließen"
    override val inventoryInProgress = "Inventur läuft"
    override val theoreticalQuantity = "Theoretische Menge"
    override val countedQuantity = "Gezählte Menge"
    override val discrepancy = "Abweichung"
    override val reason = "Grund"
    override val inventoryCompleted = "Inventur abgeschlossen"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Transfers"
    override val transfer = "Transfer"
    override val newTransfer = "Neuer Transfer"
    override val transferHistory = "Transferhistorie"
    override val fromSite = "Von Standort"
    override val toSite = "Zu Standort"
    override val transferStatus = "Transferstatus"
    override val transferPending = "Ausstehend"
    override val transferCompleted = "Abgeschlossen"
    override val transferCancelled = "Storniert"
    override val completeTransfer = "Transfer abschließen"
    override val cancelTransfer = "Transfer stornieren"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Bestand"
    override val stockMovements = "Bestandsbewegungen"
    override val stockIn = "Wareneingang"
    override val stockOut = "Warenausgang"
    override val stockAdjustment = "Bestandskorrektur"
    override val movementType = "Bewegungstyp"
    override val movementDate = "Bewegungsdatum"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Berichte"
    override val salesReport = "Verkaufsbericht"
    override val stockReport = "Bestandsbericht"
    override val profitReport = "Gewinnbericht"
    override val exportReport = "Bericht exportieren"
    override val dateRange = "Zeitraum"
    override val startDate = "Startdatum"
    override val endDate = "Enddatum"
    override val generateReport = "Bericht erstellen"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Profil"
    override val myProfile = "Mein Profil"
    override val information = "Informationen"
    override val currentPassword = "Aktuelles Passwort"
    override val newPassword = "Neues Passwort"
    override val confirmPassword = "Passwort bestätigen"
    override val passwordsDoNotMatch = "Passwörter stimmen nicht überein"
    override val passwordChangedSuccessfully = "Passwort erfolgreich geändert"
    override val userNotFound = "Benutzer nicht gefunden"
    override val incorrectPassword = "Aktuelles Passwort falsch"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Synchronisiert"
    override val pendingChanges = "{count} ausstehende Änderung(en)"
    override val conflictsToResolve = "{count} Konflikt(e) zu lösen"
    override val online = "Online"
    override val offline = "Offline"
    override val realtimeConnected = "Echtzeit verbunden"
    override val realtimeDisconnected = "Echtzeit getrennt"
    override val lastError = "Letzter Fehler"
    override val offlineMode = "Offline-Modus"
    override val conflictsDetected = "Konflikte erkannt"
    override val changesWillSyncWhenOnline = "Änderungen werden synchronisiert, wenn Sie wieder online sind"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Einstellungen"
    override val language = "Sprache"
    override val selectLanguage = "Sprache auswählen"
    override val theme = "Design"
    override val darkMode = "Dunkelmodus"
    override val lightMode = "Hellmodus"
    override val systemDefault = "Systemstandard"
    override val about = "Über"
    override val version = "Version"
    override val syncSettings = "Synchronisierungseinstellungen"
    override val lastSync = "Letzte Synchronisierung"
    override val syncNow = "Jetzt synchronisieren"
    override val syncing = "Synchronisierung..."
    override val syncSuccess = "Synchronisierung abgeschlossen"
    override val syncError = "Synchronisierung fehlgeschlagen"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Benutzer"
    override val user = "Benutzer"
    override val addUser = "Benutzer hinzufügen"
    override val editUser = "Benutzer bearbeiten"
    override val deleteUser = "Benutzer löschen"
    override val fullName = "Vollständiger Name"
    override val role = "Rolle"
    override val admin = "Administrator"
    override val permissions = "Berechtigungen"
    override val canView = "Kann anzeigen"
    override val canCreate = "Kann erstellen"
    override val canEdit = "Kann bearbeiten"
    override val canDelete = "Kann löschen"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Verpackungstypen"
    override val packagingType = "Verpackungstyp"
    override val addPackagingType = "Verpackungstyp hinzufügen"
    override val editPackagingType = "Verpackungstyp bearbeiten"
    override val level1Name = "Name Ebene 1"
    override val level2Name = "Name Ebene 2"
    override val level2Quantity = "Menge Ebene 2"
    override val conversionFactor = "Umrechnungsfaktor"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} ist erforderlich"
    override val invalidEmail = "Ungültige E-Mail-Adresse"
    override val invalidPhone = "Ungültige Telefonnummer"
    override val valueTooShort = "{field} muss mindestens {min} Zeichen haben"
    override val valueTooLong = "{field} darf nicht mehr als {max} Zeichen haben"
    override val valueMustBePositive = "Wert muss positiv sein"
    override val passwordTooShort = "Passwort muss mindestens 8 Zeichen haben"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Mindestens 8 Zeichen"
    override val passwordNeedsUppercase = "Mindestens ein Großbuchstabe (A-Z)"
    override val passwordNeedsLowercase = "Mindestens ein Kleinbuchstabe (a-z)"
    override val passwordNeedsDigit = "Mindestens eine Ziffer (0-9)"
    override val passwordNeedsSpecial = "Mindestens ein Sonderzeichen (!@#\$%...)"
    override val passwordStrengthWeak = "Schwach"
    override val passwordStrengthMedium = "Mittel"
    override val passwordStrengthStrong = "Stark"
    override val passwordRequirements = "Passwortanforderungen:"
    override val passwordStrength = "Passwortstärke:"
    override val passwordMustBeDifferent = "Das neue Passwort muss sich vom aktuellen unterscheiden"
    override val usernameAlreadyExists = "Benutzername existiert bereits"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Löschen nicht möglich"
    override val entityInUse = "Dieses {entity} wird in {count} Datensätzen verwendet"
    override val deactivateInstead = "Möchten Sie es stattdessen deaktivieren?"
    override val deactivate = "Deaktivieren"
    override val reactivate = "Reaktivieren"
    override val showInactive = "Inaktive anzeigen"
    override val hideInactive = "Inaktive ausblenden"
    override val inactive = "Inaktiv"
    override val active = "Aktiv"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Heute"
    override val yesterday = "Gestern"
    override val thisWeek = "Diese Woche"
    override val thisMonth = "Diesen Monat"
    override val thisYear = "Dieses Jahr"
    override val dateFormat = "dd.MM.yyyy"
    override val timeFormat = "HH:mm"

    // ============================================
    // NUMBERS & CURRENCY
    // ============================================
    override val currencySymbol = "€"
    override val currencyFormat = "{amount} {symbol}"
    override val decimalSeparator = ","
    override val thousandsSeparator = "."

    // ============================================
    // NOTIFICATIONS
    // ============================================
    override val notificationSettings = "Benachrichtigungseinstellungen"
    override val notificationExpiryAlerts = "Ablaufwarnungen"
    override val notificationEnableExpiry = "Ablaufbenachrichtigungen aktivieren"
    override val notificationWarningDays = "Warntage vor Ablauf"
    override val notificationExpiryDescription = "Benachrichtigungen erhalten, wenn Produkte bald ablaufen"
    override val notificationLowStockAlerts = "Niedrige Bestandswarnungen"
    override val notificationEnableLowStock = "Niedrige Bestandsbenachrichtigungen aktivieren"
    override val notificationLowStockDescription = "Benachrichtigungen erhalten, wenn der Bestand unter den Mindestschwellenwert fällt"
    override val notificationInvalidDays = "Bitte geben Sie eine gültige Anzahl von Tagen ein (1-365)"
    override val settingsSaved = "Einstellungen erfolgreich gespeichert"
    override val supabaseNotConfigured = "Supabase ist nicht konfiguriert"
    override val notifications = "Benachrichtigungen"
    override val noNotifications = "Keine Benachrichtigungen"
    override val dismissAll = "Alle verwerfen"
    override val allNotificationsDismissed = "Alle Benachrichtigungen wurden verworfen"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Aktueller Standort"
    override val operations = "Operationen"
    override val purchaseProducts = "Produkte kaufen"
    override val sellProducts = "Produkte verkaufen"
    override val transferProducts = "Produkte übertragen"
    override val viewStock = "Bestand anzeigen"
    override val inventoryStock = "Bestandsinventur"
    override val administration = "Verwaltung"
    override val management = "Management"
    override val siteManagement = "Standortverwaltung"
    override val manageProducts = "Produkte verwalten"
    override val manageCategories = "Kategorien verwalten"
    override val manageCustomers = "Kunden verwalten"
    override val userManagement = "Benutzerverwaltung"
    override val history = "Verlauf"
    override val configuration = "Konfiguration"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Erschöpft"
    override val remainingQty = "Restmenge"
    override val noPurchases = "Keine Einkäufe gefunden"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "Keine Verkäufe gefunden"
    override val saleDetails = "Verkaufsdetails"
    override val items = "Artikel"
    override val total = "Gesamt"
    override val date = "Datum"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Inventuren"
    override val noInventories = "Keine Inventuren gefunden"
    override val inProgress = "In Bearbeitung"
    override val completed = "Abgeschlossen"
    override val pending = "Ausstehend"
    override val newInventory = "Neue Inventur"
    override val start = "Starten"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "Keine Übertragungen gefunden"
    override val sourceSite = "Quellstandort"
    override val destinationSite = "Zielstandort"
    override val quantityToTransfer = "Zu übertragende Menge"
    override val create = "Erstellen"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "Keine Bestandsdaten"
    override val summary = "Zusammenfassung"
    override val outOfStock = "Nicht vorrätig"
    override val stockByProduct = "Bestand nach Produkt"
    override val noMovements = "Keine Bewegungen gefunden"
    override val availableStock = "Verfügbarer Bestand"
    override val preview = "Vorschau"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "Keine Verpackungsarten gefunden"
    override val addLevel2 = "Ebene 2 hinzufügen"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Audit-Verlauf"
    override val noHistory = "Kein Verlauf gefunden"
    override val filterBy = "Filtern nach"
    override val all = "Alle"
    override val created = "Erstellt"
    override val updated = "Aktualisiert"
    override val deleted = "Gelöscht"
    override val details = "Details"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Supabase-Konfiguration"
    override val projectUrl = "Projekt-URL"
    override val anonKey = "Anonymer Schlüssel"
    override val synchronization = "Synchronisierung"
    override val syncData = "Daten synchronisieren"
    override val currentStatus = "Aktueller Status"
    override val configured = "Konfiguriert"
    override val connection = "Verbindung"
    override val testConnection = "Verbindung testen"
    override val clearConfiguration = "Konfiguration löschen"
    override val configSaved = "Konfiguration gespeichert"
    override val syncCompleted = "Synchronisierung erfolgreich"
    override val connectionSuccessful = "Verbindung erfolgreich"
    override val howToGetInfo = "So erhalten Sie diese Informationen:"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Supabase konfigurieren"
    override val authentication = "Authentifizierung"
    override val enterCredentials = "Bitte geben Sie Ihre Anmeldedaten ein"
    override val invalidPassword = "Ungültiges Passwort"
    override val accountDisabled = "Dieses Konto ist deaktiviert"
    override val connectionError = "Verbindungsfehler"
    override val firstLoginRequiresInternet = "Die erste Anmeldung erfordert Internet"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "Ansehen"
    override val select = "Auswählen"
    override val chooseProduct = "Produkt auswählen"
    override val orSelect = "Oder auswählen"
    override val enable = "Aktivieren"
    override val later = "Später"
    override val alertsDescription = "Benachrichtigungen warnen Sie vor abgelaufenen Produkten und niedrigem Bestand."
    override val justNow = "Gerade eben"
    override val minutesAgo = "Vor {count} Min"
    override val hoursAgo = "Vor {count} Std"
    override val daysAgo = "Vor {count} Tag(en)"
    override val critical = "Kritisch"
    override val urgent = "Dringend"
    override val info = "Info"
    override val low = "Niedrig"
    override val nearestExpiry = "Nächstes Ablaufdatum"
    override val lots = "Charge(n)"
    override val addNote = "Notiz hinzufügen..."
    override val saving = "Speichern..."
    override val continue_ = "Fortfahren"
    override val unknownSite = "Unbekannter Standort"
    override val unknownProduct = "Unbekanntes Produkt"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Fügen Sie Ihr erstes Produkt hinzu, um zu beginnen"
    override val noCustomersMessage = "Fügen Sie Ihren ersten Kunden hinzu, um zu beginnen"
    override val noCategoriesMessage = "Fügen Sie Ihre erste Kategorie hinzu, um zu beginnen"
    override val noSitesMessage = "Fügen Sie Ihren ersten Standort hinzu, um zu beginnen"
    override val noPackagingTypesMessage = "Fügen Sie Ihren ersten Verpackungstyp hinzu, um zu beginnen"
    override val noInventoriesMessage = "Es wurden noch keine Inventuren durchgeführt"
    override val noSalesMessage = "Es wurden noch keine Verkäufe erfasst"
    override val noPurchasesMessage = "Es wurden noch keine Einkäufe erfasst"
    override val noTransfersMessage = "Es wurden noch keine Transfers erfasst"
    override val noStockMessage = "Kein Bestand verfügbar"
    override val noMovementsMessage = "Keine Lagerbewegungen erfasst"
    override val noUsersMessage = "Fügen Sie Ihren ersten Benutzer hinzu, um zu beginnen"
    override val historyWillAppearHere = "Der Audit-Verlauf wird hier angezeigt"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Fügen Sie zuerst einen Standort hinzu"
    override val none = "Keiner"
    override val stockAlerts = "Bestandswarnungen"
    override val stockAlertDescription = "Legen Sie Mindest- und Höchstbestände fest, um Warnungen zu erhalten"
    override val transferIn = "Transfer eingehend"
    override val transferOut = "Transfer ausgehend"
    override val batches = "Chargen"
    override val noUsers = "Keine Benutzer"
    override val adminHasAllPermissions = "Administratoren haben alle Berechtigungen"
    override val create_ = "Erstellen"
    override val selectSourceSiteFirst = "Wählen Sie zuerst den Quellstandort"
    override val entries = "Einträge"
    override val optional = "optional"
    override val packagingTypeName = "Name des Verpackungstyps"
    override val started = "Gestartet"
    override val saleItem = "Verkaufsartikel"
    override val purchaseBatch = "Einkaufscharge"
    override val stockMovement = "Lagerbewegung"
    override val supabaseStep1 = "1. Gehen Sie zu supabase.com und erstellen Sie ein Konto"
    override val supabaseStep2 = "2. Erstellen Sie ein neues Projekt"
    override val supabaseStep3 = "3. Gehen Sie zu Projekteinstellungen > API, um Ihre URL und den Anon-Schlüssel zu finden"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Update erforderlich"
    override val updateAvailable = "Update verfügbar"
    override val appVersionIncompatible = "Ihre App-Version ist nicht mit der Datenbank kompatibel. Bitte aktualisieren Sie die App, um fortzufahren."
    override val appVersion = "App-Version"
    override val minimumRequiredVersion = "Erforderliche Mindestversion"
    override val databaseVersion = "Datenbankversion"
    override val toUpdate = "Zum Aktualisieren"
    override val contactAdminForUpdate = "Kontaktieren Sie Ihren Administrator, um die neueste Version der App zu erhalten."
    override val checkingCompatibility = "Kompatibilität wird geprüft..."
    override val download = "Herunterladen"
    override val newVersionAvailable = "Eine neue Version von MediStock ist verfügbar."
    override val currentVersionLabel = "Aktuelle Version"
    override val newVersionLabel = "Neue Version"
    override val whatsNew = "Neuigkeiten"
    override val unableToLoadNotifications = "Benachrichtigungen können nicht geladen werden"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "App-Einstellungen"
    override val currencySymbolSetting = "Währungssymbol"
    override val currencySymbolDescription = "Symbol zur Anzeige von Preisen (z.B. F, $, EUR)"
    override val settingsSavedSuccessfully = "Einstellungen erfolgreich gespeichert"
    override val invalidCurrencySymbol = "Bitte geben Sie ein gültiges Währungssymbol ein"
}
