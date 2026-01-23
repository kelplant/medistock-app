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
    override val addPurchase = "Aggiungi acquisto"
    override val purchaseHistory = "Storico acquisti"
    override val supplier = "Fornitore"
    override val supplierName = "Nome fornitore"
    override val batchNumber = "Numero lotto"
    override val purchaseDate = "Data acquisto"
    override val expiryDate = "Data di scadenza"
    override val quantity = "Quantità"
    override val initialQuantity = "Quantità iniziale"
    override val remainingQuantity = "Quantità rimanente"
    override val totalAmount = "Totale"
    override val purchaseRecorded = "Acquisto registrato"

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
    override val passwordTooShort = "La password deve essere di almeno 6 caratteri"

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
}
