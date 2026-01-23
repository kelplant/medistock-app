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
    override val addPurchase = "Einkauf hinzufügen"
    override val purchaseHistory = "Einkaufshistorie"
    override val supplier = "Lieferant"
    override val supplierName = "Lieferantenname"
    override val batchNumber = "Chargennummer"
    override val purchaseDate = "Einkaufsdatum"
    override val expiryDate = "Ablaufdatum"
    override val quantity = "Menge"
    override val initialQuantity = "Anfangsmenge"
    override val remainingQuantity = "Restmenge"
    override val totalAmount = "Gesamtbetrag"
    override val purchaseRecorded = "Einkauf erfasst"

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
    override val passwordTooShort = "Passwort muss mindestens 6 Zeichen haben"

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
}
