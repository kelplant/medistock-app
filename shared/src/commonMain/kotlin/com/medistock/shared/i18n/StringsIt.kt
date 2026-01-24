package com.medistock.shared.i18n

/**
 * Italian strings implementation.
 */
object StringsIt : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Annulla"
    override val save = "Salva"
    override val delete = "Elimina"
    override val edit = "Modifica"
    override val add = "Aggiungi"
    override val search = "Cerca"
    override val loading = "Caricamento..."
    override val error = "Errore"
    override val success = "Operazione riuscita"
    override val warning = "Avviso"
    override val confirm = "Conferma"
    override val yes = "Sì"
    override val no = "No"
    override val close = "Chiudi"
    override val back = "Indietro"
    override val next = "Avanti"
    override val retry = "Riprova"
    override val noData = "Nessun dato disponibile"
    override val required = "Obbligatorio"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Accesso"
    override val username = "Nome utente"
    override val password = "Password"
    override val login = "Accedi"
    override val logout = "Esci"
    override val logoutConfirm = "Sei sicuro di voler uscire?"
    override val changePassword = "Cambia password"
    override val loginError = "Accesso non riuscito"
    override val loginErrorInvalidCredentials = "Nome utente o password non validi"
    override val welcomeBack = "Bentornato, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Home"
    override val dashboard = "Dashboard"
    override val quickActions = "Azioni rapide"
    override val recentActivity = "Attività recenti"
    override val todaySales = "Vendite di oggi"
    override val lowStock = "Scorte basse"
    override val pendingTransfers = "Trasferimenti in attesa"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Siti"
    override val site = "Sito"
    override val siteName = "Nome del sito"
    override val addSite = "Aggiungi sito"
    override val editSite = "Modifica sito"
    override val deleteSite = "Elimina sito"
    override val deleteSiteConfirm = "Sei sicuro di voler eliminare questo sito?"
    override val siteDeleted = "Sito eliminato"
    override val siteDeactivated = "Sito disattivato"
    override val noSites = "Nessun sito trovato"
    override val selectSite = "Seleziona un sito"
    override val allSites = "Tutti i siti"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Categorie"
    override val category = "Categoria"
    override val categoryName = "Nome categoria"
    override val addCategory = "Aggiungi categoria"
    override val editCategory = "Modifica categoria"
    override val deleteCategory = "Elimina categoria"
    override val deleteCategoryConfirm = "Sei sicuro di voler eliminare questa categoria?"
    override val categoryDeleted = "Categoria eliminata"
    override val categoryDeactivated = "Categoria disattivata"
    override val noCategories = "Nessuna categoria trovata"
    override val selectCategory = "Seleziona una categoria"
    override val allCategories = "Tutte le categorie"
    override val uncategorized = "Senza categoria"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Prodotti"
    override val product = "Prodotto"
    override val productName = "Nome prodotto"
    override val addProduct = "Aggiungi prodotto"
    override val editProduct = "Modifica prodotto"
    override val deleteProduct = "Elimina prodotto"
    override val deleteProductConfirm = "Sei sicuro di voler eliminare questo prodotto?"
    override val productDeleted = "Prodotto eliminato"
    override val productDeactivated = "Prodotto disattivato"
    override val noProducts = "Nessun prodotto trovato"
    override val selectProduct = "Seleziona un prodotto"
    override val unit = "Unità"
    override val unitVolume = "Volume unità"
    override val description = "Descrizione"
    override val minStock = "Scorta minima"
    override val maxStock = "Scorta massima"
    override val currentStock = "Scorta attuale"
    override val price = "Prezzo"
    override val purchasePrice = "Prezzo di acquisto"
    override val sellingPrice = "Prezzo di vendita"
    override val margin = "Margine"
    override val marginType = "Tipo di margine"
    override val marginValue = "Valore del margine"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Clienti"
    override val customer = "Cliente"
    override val customerName = "Nome cliente"
    override val addCustomer = "Aggiungi cliente"
    override val editCustomer = "Modifica cliente"
    override val deleteCustomer = "Elimina cliente"
    override val deleteCustomerConfirm = "Sei sicuro di voler eliminare questo cliente?"
    override val customerDeleted = "Cliente eliminato"
    override val customerDeactivated = "Cliente disattivato"
    override val noCustomers = "Nessun cliente trovato"
    override val selectCustomer = "Seleziona un cliente"
    override val phone = "Telefono"
    override val email = "Email"
    override val address = "Indirizzo"
    override val notes = "Note"
    override val walkInCustomer = "Cliente occasionale"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Acquisti"
    override val purchase = "Acquisto"
    override val newPurchase = "Nuovo acquisto"
    override val addPurchase = "Aggiungi acquisto"
    override val purchaseHistory = "Storico acquisti"
    override val supplier = "Fornitore"
    override val supplierName = "Nome fornitore"
    override val batchNumber = "Numero lotto"
    override val purchaseDate = "Data acquisto"
    override val expiryDate = "Data di scadenza"
    override val expiryDateOptional = "Data di scadenza (opzionale)"
    override val quantity = "Quantità"
    override val initialQuantity = "Quantità iniziale"
    override val remainingQuantity = "Quantità rimanente"
    override val totalAmount = "Totale"
    override val purchaseRecorded = "Acquisto registrato"
    override val unitPurchasePrice = "Prezzo di acquisto unitario"
    override val unitSellingPrice = "Prezzo di vendita unitario"
    override val marginCalculatedAuto = "Margine: calcolato automaticamente"
    override val sellingPriceNote = "Il prezzo di vendita viene calcolato automaticamente in base al margine del prodotto, ma può essere modificato."
    override val savePurchase = "Salva acquisto"
    override val enterSupplierName = "Inserisci il nome del fornitore"
    override val batchNumberExample = "Es: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Vendite"
    override val sale = "Vendita"
    override val newSale = "Nuova vendita"
    override val saleHistory = "Storico vendite"
    override val saleDate = "Data vendita"
    override val saleTotal = "Totale vendita"
    override val saleItems = "Articoli vendita"
    override val addItem = "Aggiungi articolo"
    override val removeItem = "Rimuovi articolo"
    override val unitPrice = "Prezzo unitario"
    override val itemTotal = "Totale articolo"
    override val subtotal = "Subtotale"
    override val discount = "Sconto"
    override val grandTotal = "Totale complessivo"
    override val completeSale = "Completa vendita"
    override val saleCompleted = "Vendita completata"
    override val noSaleItems = "Nessun articolo nella vendita"
    override val insufficientStock = "Scorte insufficienti per {product}: {available} disponibili, {requested} richiesti"
    override val remainingQuantityNeeded = "Quantità rimanente necessaria: {quantity} unità"
    override val editSale = "Modifica vendita"
    override val editPurchase = "Modifica acquisto"
    override val productsToSell = "Prodotti da vendere"
    override val addProductToSale = "+ Aggiungi prodotto"
    override val enterCustomerName = "Inserisci il nome del cliente"
    override val pricePerUnit = "Prezzo per unità"
    override val exampleQuantity = "Es: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Inventario"
    override val inventoryCount = "Conteggio inventario"
    override val startInventory = "Avvia inventario"
    override val completeInventory = "Completa inventario"
    override val inventoryInProgress = "Inventario in corso"
    override val theoreticalQuantity = "Quantità teorica"
    override val countedQuantity = "Quantità conteggiata"
    override val discrepancy = "Differenza"
    override val reason = "Motivo"
    override val inventoryCompleted = "Inventario completato"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Trasferimenti"
    override val transfer = "Trasferimento"
    override val newTransfer = "Nuovo trasferimento"
    override val transferHistory = "Storico trasferimenti"
    override val fromSite = "Da sito"
    override val toSite = "A sito"
    override val transferStatus = "Stato trasferimento"
    override val transferPending = "In attesa"
    override val transferCompleted = "Completato"
    override val transferCancelled = "Annullato"
    override val completeTransfer = "Completa trasferimento"
    override val cancelTransfer = "Annulla trasferimento"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Magazzino"
    override val stockMovements = "Movimenti magazzino"
    override val stockIn = "Entrata"
    override val stockOut = "Uscita"
    override val stockAdjustment = "Rettifica"
    override val movementType = "Tipo movimento"
    override val movementDate = "Data movimento"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Report"
    override val salesReport = "Report vendite"
    override val stockReport = "Report scorte"
    override val profitReport = "Report profitti"
    override val exportReport = "Esporta report"
    override val dateRange = "Intervallo date"
    override val startDate = "Data inizio"
    override val endDate = "Data fine"
    override val generateReport = "Genera report"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Profilo"
    override val myProfile = "Il mio profilo"
    override val information = "Informazioni"
    override val currentPassword = "Password attuale"
    override val newPassword = "Nuova password"
    override val confirmPassword = "Conferma password"
    override val passwordsDoNotMatch = "Le password non corrispondono"
    override val passwordChangedSuccessfully = "Password cambiata con successo"
    override val userNotFound = "Utente non trovato"
    override val incorrectPassword = "Password attuale errata"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Sincronizzato"
    override val pendingChanges = "{count} modifica/e in sospeso"
    override val conflictsToResolve = "{count} conflitto/i da risolvere"
    override val online = "Online"
    override val offline = "Offline"
    override val realtimeConnected = "Tempo reale connesso"
    override val realtimeDisconnected = "Tempo reale disconnesso"
    override val lastError = "Ultimo errore"
    override val offlineMode = "Modalità offline"
    override val conflictsDetected = "Conflitti rilevati"
    override val changesWillSyncWhenOnline = "Le modifiche verranno sincronizzate quando tornerai online"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Impostazioni"
    override val language = "Lingua"
    override val selectLanguage = "Seleziona lingua"
    override val theme = "Tema"
    override val darkMode = "Modalità scura"
    override val lightMode = "Modalità chiara"
    override val systemDefault = "Predefinito di sistema"
    override val about = "Informazioni"
    override val version = "Versione"
    override val syncSettings = "Impostazioni sincronizzazione"
    override val lastSync = "Ultima sincronizzazione"
    override val syncNow = "Sincronizza ora"
    override val syncing = "Sincronizzazione..."
    override val syncSuccess = "Sincronizzazione completata"
    override val syncError = "Sincronizzazione non riuscita"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Utenti"
    override val user = "Utente"
    override val addUser = "Aggiungi utente"
    override val editUser = "Modifica utente"
    override val deleteUser = "Elimina utente"
    override val fullName = "Nome completo"
    override val role = "Ruolo"
    override val admin = "Amministratore"
    override val permissions = "Permessi"
    override val canView = "Può visualizzare"
    override val canCreate = "Può creare"
    override val canEdit = "Può modificare"
    override val canDelete = "Può eliminare"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Tipi di confezione"
    override val packagingType = "Tipo di confezione"
    override val addPackagingType = "Aggiungi tipo di confezione"
    override val editPackagingType = "Modifica tipo di confezione"
    override val level1Name = "Nome livello 1"
    override val level2Name = "Nome livello 2"
    override val level2Quantity = "Quantità livello 2"
    override val conversionFactor = "Fattore di conversione"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} è obbligatorio"
    override val invalidEmail = "Indirizzo email non valido"
    override val invalidPhone = "Numero di telefono non valido"
    override val valueTooShort = "{field} deve essere di almeno {min} caratteri"
    override val valueTooLong = "{field} non deve superare {max} caratteri"
    override val valueMustBePositive = "Il valore deve essere positivo"
    override val passwordTooShort = "La password deve essere di almeno 8 caratteri"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Almeno 8 caratteri"
    override val passwordNeedsUppercase = "Almeno una lettera maiuscola (A-Z)"
    override val passwordNeedsLowercase = "Almeno una lettera minuscola (a-z)"
    override val passwordNeedsDigit = "Almeno una cifra (0-9)"
    override val passwordNeedsSpecial = "Almeno un carattere speciale (!@#\$%...)"
    override val passwordStrengthWeak = "Debole"
    override val passwordStrengthMedium = "Media"
    override val passwordStrengthStrong = "Forte"
    override val passwordRequirements = "Requisiti password:"
    override val passwordStrength = "Forza password:"
    override val passwordMustBeDifferent = "La nuova password deve essere diversa da quella attuale"
    override val usernameAlreadyExists = "Nome utente già esistente"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Impossibile eliminare"
    override val entityInUse = "Questo {entity} è usato in {count} record"
    override val deactivateInstead = "Vuoi disattivarlo invece?"
    override val deactivate = "Disattiva"
    override val reactivate = "Riattiva"
    override val showInactive = "Mostra inattivi"
    override val hideInactive = "Nascondi inattivi"
    override val inactive = "Inattivo"
    override val active = "Attivo"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Oggi"
    override val yesterday = "Ieri"
    override val thisWeek = "Questa settimana"
    override val thisMonth = "Questo mese"
    override val thisYear = "Quest'anno"
    override val dateFormat = "dd/MM/yyyy"
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
    override val notificationSettings = "Impostazioni notifiche"
    override val notificationExpiryAlerts = "Avvisi di scadenza"
    override val notificationEnableExpiry = "Attiva notifiche di scadenza"
    override val notificationWarningDays = "Giorni di preavviso prima della scadenza"
    override val notificationExpiryDescription = "Ricevi avvisi quando i prodotti stanno per scadere"
    override val notificationLowStockAlerts = "Avvisi scorte basse"
    override val notificationEnableLowStock = "Attiva notifiche scorte basse"
    override val notificationLowStockDescription = "Ricevi avvisi quando le scorte scendono sotto la soglia minima"
    override val notificationInvalidDays = "Inserisci un numero di giorni valido (1-365)"
    override val settingsSaved = "Impostazioni salvate con successo"
    override val supabaseNotConfigured = "Supabase non è configurato"
    override val notifications = "Notifiche"
    override val noNotifications = "Nessuna notifica"
    override val dismissAll = "Elimina tutto"
    override val allNotificationsDismissed = "Tutte le notifiche sono state eliminate"

    override val currentSite = "Sito attuale"
    override val operations = "Operazioni"
    override val purchaseProducts = "Acquista prodotti"
    override val sellProducts = "Vendi prodotti"
    override val transferProducts = "Trasferisci prodotti"
    override val viewStock = "Visualizza scorte"
    override val inventoryStock = "Inventario scorte"
    override val administration = "Amministrazione"
    override val management = "Gestione"
    override val siteManagement = "Gestione siti"
    override val manageProducts = "Gestisci prodotti"
    override val manageCategories = "Gestisci categorie"
    override val manageCustomers = "Gestisci clienti"
    override val userManagement = "Gestione utenti"
    override val history = "Cronologia"
    override val configuration = "Configurazione"
    override val exhausted = "Esaurito"
    override val remainingQty = "Quantità rimanente"
    override val noPurchases = "Nessun acquisto trovato"
    override val noSales = "Nessuna vendita trovata"
    override val saleDetails = "Dettagli vendita"
    override val items = "Articoli"
    override val total = "Totale"
    override val date = "Data"
    override val inventories = "Inventari"
    override val noInventories = "Nessun inventario trovato"
    override val inProgress = "In corso"
    override val completed = "Completato"
    override val pending = "In attesa"
    override val newInventory = "Nuovo inventario"
    override val start = "Inizia"
    override val noTransfers = "Nessun trasferimento trovato"
    override val sourceSite = "Sito di origine"
    override val destinationSite = "Sito di destinazione"
    override val quantityToTransfer = "Quantità da trasferire"
    override val create = "Crea"
    override val noStock = "Nessun dato di scorte"
    override val summary = "Riepilogo"
    override val outOfStock = "Esaurito"
    override val stockByProduct = "Scorte per prodotto"
    override val noMovements = "Nessun movimento trovato"
    override val availableStock = "Scorte disponibili"
    override val preview = "Anteprima"
    override val noPackagingTypes = "Nessun tipo di imballaggio trovato"
    override val addLevel2 = "Aggiungi livello 2"
    override val auditHistory = "Cronologia audit"
    override val noHistory = "Nessuna cronologia trovata"
    override val filterBy = "Filtra per"
    override val all = "Tutto"
    override val created = "Creato"
    override val updated = "Aggiornato"
    override val deleted = "Eliminato"
    override val details = "Dettagli"
    override val supabaseConfiguration = "Configurazione Supabase"
    override val projectUrl = "URL progetto"
    override val anonKey = "Chiave anonima"
    override val synchronization = "Sincronizzazione"
    override val syncData = "Sincronizza dati"
    override val currentStatus = "Stato attuale"
    override val configured = "Configurato"
    override val connection = "Connessione"
    override val testConnection = "Test connessione"
    override val clearConfiguration = "Cancella configurazione"
    override val configSaved = "Configurazione salvata"
    override val syncCompleted = "Sincronizzazione completata"
    override val connectionSuccessful = "Connessione riuscita"
    override val howToGetInfo = "Come ottenere queste informazioni:"
    override val configureSupabase = "Configura Supabase"
    override val authentication = "Autenticazione"
    override val enterCredentials = "Inserisci le tue credenziali"
    override val invalidPassword = "Password non valida"
    override val accountDisabled = "Questo account è disabilitato"
    override val connectionError = "Errore di connessione"
    override val firstLoginRequiresInternet = "Il primo accesso richiede Internet"
    override val view = "Visualizza"
    override val select = "Seleziona"
    override val chooseProduct = "Scegli un prodotto"
    override val orSelect = "Oppure seleziona"
    override val enable = "Attiva"
    override val later = "Più tardi"
    override val alertsDescription = "Le notifiche ti avvisano sui prodotti scaduti e sulle scorte basse."
    override val justNow = "Adesso"
    override val minutesAgo = "{count} min fa"
    override val hoursAgo = "{count} ore fa"
    override val daysAgo = "{count} giorno/i fa"
    override val critical = "Critico"
    override val urgent = "Urgente"
    override val info = "Info"
    override val low = "Basso"
    override val nearestExpiry = "Scadenza più vicina"
    override val lots = "lotto/i"
    override val addNote = "Aggiungi nota..."
    override val saving = "Salvataggio..."
    override val continue_ = "Continua"
    override val unknownSite = "Sito sconosciuto"
    override val unknownProduct = "Prodotto sconosciuto"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Aggiungi il tuo primo prodotto per iniziare"
    override val noCustomersMessage = "Aggiungi il tuo primo cliente per iniziare"
    override val noCategoriesMessage = "Aggiungi la tua prima categoria per iniziare"
    override val noSitesMessage = "Aggiungi il tuo primo sito per iniziare"
    override val noPackagingTypesMessage = "Aggiungi il tuo primo tipo di imballaggio per iniziare"
    override val noInventoriesMessage = "Non sono stati ancora effettuati inventari"
    override val noSalesMessage = "Non sono state ancora registrate vendite"
    override val noPurchasesMessage = "Non sono stati ancora registrati acquisti"
    override val noTransfersMessage = "Non sono stati ancora registrati trasferimenti"
    override val noStockMessage = "Nessuna giacenza disponibile"
    override val noMovementsMessage = "Nessun movimento di magazzino registrato"
    override val noUsersMessage = "Aggiungi il tuo primo utente per iniziare"
    override val historyWillAppearHere = "La cronologia di audit apparirà qui"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Aggiungi prima un sito"
    override val none = "Nessuno"
    override val stockAlerts = "Avvisi giacenza"
    override val stockAlertDescription = "Imposta livelli di giacenza minima e massima per ricevere avvisi"
    override val transferIn = "Trasferimento in entrata"
    override val transferOut = "Trasferimento in uscita"
    override val batches = "lotti"
    override val noUsers = "Nessun utente"
    override val adminHasAllPermissions = "Gli amministratori hanno tutti i permessi"
    override val create_ = "Crea"
    override val selectSourceSiteFirst = "Seleziona prima il sito di origine"
    override val entries = "voci"
    override val optional = "opzionale"
    override val packagingTypeName = "Nome tipo imballaggio"
    override val started = "Iniziato"
    override val saleItem = "Articolo vendita"
    override val purchaseBatch = "Lotto acquisto"
    override val stockMovement = "Movimento magazzino"
    override val supabaseStep1 = "1. Vai su supabase.com e crea un account"
    override val supabaseStep2 = "2. Crea un nuovo progetto"
    override val supabaseStep3 = "3. Vai su Impostazioni progetto > API per trovare URL e chiave anon"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Aggiornamento richiesto"
    override val updateAvailable = "Aggiornamento disponibile"
    override val appVersionIncompatible = "La versione dell'app non è compatibile con il database. Aggiorna l'app per continuare."
    override val appVersion = "Versione app"
    override val minimumRequiredVersion = "Versione minima richiesta"
    override val databaseVersion = "Versione database"
    override val toUpdate = "Per aggiornare"
    override val contactAdminForUpdate = "Contatta il tuo amministratore per ottenere l'ultima versione dell'app."
    override val checkingCompatibility = "Verifica compatibilità..."
    override val download = "Scarica"
    override val newVersionAvailable = "Una nuova versione di MediStock è disponibile."
    override val currentVersionLabel = "Versione attuale"
    override val newVersionLabel = "Nuova versione"
    override val whatsNew = "Novità"
    override val unableToLoadNotifications = "Impossibile caricare le notifiche"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Impostazioni app"
    override val currencySymbolSetting = "Simbolo valuta"
    override val currencySymbolDescription = "Simbolo usato per visualizzare i prezzi (es: F, $, EUR)"
    override val settingsSavedSuccessfully = "Impostazioni salvate con successo"
    override val invalidCurrencySymbol = "Inserisci un simbolo valuta valido"
}
