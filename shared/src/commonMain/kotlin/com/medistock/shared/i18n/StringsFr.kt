package com.medistock.shared.i18n

/**
 * French strings implementation.
 */
object StringsFr : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Annuler"
    override val save = "Enregistrer"
    override val delete = "Supprimer"
    override val edit = "Modifier"
    override val add = "Ajouter"
    override val search = "Rechercher"
    override val loading = "Chargement..."
    override val error = "Erreur"
    override val success = "Succès"
    override val warning = "Attention"
    override val confirm = "Confirmer"
    override val yes = "Oui"
    override val no = "Non"
    override val close = "Fermer"
    override val back = "Retour"
    override val next = "Suivant"
    override val retry = "Réessayer"
    override val noData = "Aucune donnée disponible"
    override val required = "Obligatoire"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Connexion"
    override val username = "Nom d'utilisateur"
    override val password = "Mot de passe"
    override val login = "Se connecter"
    override val logout = "Se déconnecter"
    override val logoutConfirm = "Êtes-vous sûr de vouloir vous déconnecter ?"
    override val changePassword = "Changer le mot de passe"
    override val loginError = "Échec de la connexion"
    override val loginErrorInvalidCredentials = "Nom d'utilisateur ou mot de passe invalide"
    override val welcomeBack = "Bon retour, {name} !"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Accueil"
    override val dashboard = "Tableau de bord"
    override val quickActions = "Actions rapides"
    override val recentActivity = "Activité récente"
    override val todaySales = "Ventes du jour"
    override val lowStock = "Stock bas"
    override val pendingTransfers = "Transferts en attente"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Sites"
    override val site = "Site"
    override val siteName = "Nom du site"
    override val addSite = "Ajouter un site"
    override val editSite = "Modifier le site"
    override val deleteSite = "Supprimer le site"
    override val deleteSiteConfirm = "Êtes-vous sûr de vouloir supprimer ce site ?"
    override val siteDeleted = "Site supprimé"
    override val siteDeactivated = "Site désactivé"
    override val noSites = "Aucun site trouvé"
    override val selectSite = "Sélectionner un site"
    override val allSites = "Tous les sites"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Catégories"
    override val category = "Catégorie"
    override val categoryName = "Nom de la catégorie"
    override val addCategory = "Ajouter une catégorie"
    override val editCategory = "Modifier la catégorie"
    override val deleteCategory = "Supprimer la catégorie"
    override val deleteCategoryConfirm = "Êtes-vous sûr de vouloir supprimer cette catégorie ?"
    override val categoryDeleted = "Catégorie supprimée"
    override val categoryDeactivated = "Catégorie désactivée"
    override val noCategories = "Aucune catégorie trouvée"
    override val selectCategory = "Sélectionner une catégorie"
    override val allCategories = "Toutes les catégories"
    override val uncategorized = "Non catégorisé"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Produits"
    override val product = "Produit"
    override val productName = "Nom du produit"
    override val addProduct = "Ajouter un produit"
    override val editProduct = "Modifier le produit"
    override val deleteProduct = "Supprimer le produit"
    override val deleteProductConfirm = "Êtes-vous sûr de vouloir supprimer ce produit ?"
    override val productDeleted = "Produit supprimé"
    override val productDeactivated = "Produit désactivé"
    override val noProducts = "Aucun produit trouvé"
    override val selectProduct = "Sélectionner un produit"
    override val unit = "Unité"
    override val unitVolume = "Volume unitaire"
    override val description = "Description"
    override val minStock = "Stock minimum"
    override val maxStock = "Stock maximum"
    override val currentStock = "Stock actuel"
    override val price = "Prix"
    override val purchasePrice = "Prix d'achat"
    override val sellingPrice = "Prix de vente"
    override val margin = "Marge"
    override val marginType = "Type de marge"
    override val marginValue = "Valeur de marge"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Clients"
    override val customer = "Client"
    override val customerName = "Nom du client"
    override val addCustomer = "Ajouter un client"
    override val editCustomer = "Modifier le client"
    override val deleteCustomer = "Supprimer le client"
    override val deleteCustomerConfirm = "Êtes-vous sûr de vouloir supprimer ce client ?"
    override val customerDeleted = "Client supprimé"
    override val customerDeactivated = "Client désactivé"
    override val noCustomers = "Aucun client trouvé"
    override val selectCustomer = "Sélectionner un client"
    override val phone = "Téléphone"
    override val email = "E-mail"
    override val address = "Adresse"
    override val notes = "Notes"
    override val walkInCustomer = "Client de passage"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Achats"
    override val purchase = "Achat"
    override val newPurchase = "Nouvel achat"
    override val addPurchase = "Ajouter un achat"
    override val purchaseHistory = "Historique des achats"
    override val supplier = "Fournisseur"
    override val supplierName = "Nom du fournisseur"
    override val batchNumber = "Numéro de lot"
    override val purchaseDate = "Date d'achat"
    override val expiryDate = "Date d'expiration"
    override val expiryDateOptional = "Date d'expiration (optionnel)"
    override val quantity = "Quantité"
    override val initialQuantity = "Quantité initiale"
    override val remainingQuantity = "Quantité restante"
    override val totalAmount = "Montant total"
    override val purchaseRecorded = "Achat enregistré"
    override val unitPurchasePrice = "Prix d'achat unitaire"
    override val unitSellingPrice = "Prix de vente unitaire"
    override val marginCalculatedAuto = "Marge : calculée automatiquement"
    override val sellingPriceNote = "Le prix de vente est calculé automatiquement selon la marge du produit, mais vous pouvez le modifier."
    override val savePurchase = "Enregistrer l'achat"
    override val enterSupplierName = "Entrez le nom du fournisseur"
    override val batchNumberExample = "Ex : LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Ventes"
    override val sale = "Vente"
    override val newSale = "Nouvelle vente"
    override val saleHistory = "Historique des ventes"
    override val saleDate = "Date de vente"
    override val saleTotal = "Total de la vente"
    override val saleItems = "Articles de la vente"
    override val addItem = "Ajouter un article"
    override val removeItem = "Retirer l'article"
    override val unitPrice = "Prix unitaire"
    override val itemTotal = "Total de l'article"
    override val subtotal = "Sous-total"
    override val discount = "Remise"
    override val grandTotal = "Total général"
    override val completeSale = "Finaliser la vente"
    override val saleCompleted = "Vente finalisée"
    override val noSaleItems = "Aucun article dans la vente"
    override val insufficientStock = "Stock insuffisant pour {product} : {available} disponibles, {requested} demandés"
    override val remainingQuantityNeeded = "Quantité restante nécessaire : {quantity} unités"
    override val editSale = "Modifier la vente"
    override val editPurchase = "Modifier l'achat"
    override val productsToSell = "Produits à vendre"
    override val addProductToSale = "+ Ajouter un produit"
    override val enterCustomerName = "Entrez le nom du client"
    override val pricePerUnit = "Prix par unité"
    override val exampleQuantity = "Ex : 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Inventaire"
    override val inventoryCount = "Comptage d'inventaire"
    override val startInventory = "Démarrer l'inventaire"
    override val completeInventory = "Terminer l'inventaire"
    override val inventoryInProgress = "Inventaire en cours"
    override val theoreticalQuantity = "Quantité théorique"
    override val countedQuantity = "Quantité comptée"
    override val discrepancy = "Écart"
    override val reason = "Motif"
    override val inventoryCompleted = "Inventaire terminé"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Transferts"
    override val transfer = "Transfert"
    override val newTransfer = "Nouveau transfert"
    override val transferHistory = "Historique des transferts"
    override val fromSite = "Site d'origine"
    override val toSite = "Site de destination"
    override val transferStatus = "Statut du transfert"
    override val transferPending = "En attente"
    override val transferCompleted = "Terminé"
    override val transferCancelled = "Annulé"
    override val completeTransfer = "Terminer le transfert"
    override val cancelTransfer = "Annuler le transfert"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Stock"
    override val stockMovements = "Mouvements de stock"
    override val stockIn = "Entrée de stock"
    override val stockOut = "Sortie de stock"
    override val stockAdjustment = "Ajustement de stock"
    override val movementType = "Type de mouvement"
    override val movementDate = "Date du mouvement"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Rapports"
    override val salesReport = "Rapport des ventes"
    override val stockReport = "Rapport de stock"
    override val profitReport = "Rapport de bénéfices"
    override val exportReport = "Exporter le rapport"
    override val dateRange = "Période"
    override val startDate = "Date de début"
    override val endDate = "Date de fin"
    override val generateReport = "Générer le rapport"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Profil"
    override val myProfile = "Mon profil"
    override val information = "Informations"
    override val currentPassword = "Mot de passe actuel"
    override val newPassword = "Nouveau mot de passe"
    override val confirmPassword = "Confirmer le mot de passe"
    override val passwordsDoNotMatch = "Les mots de passe ne correspondent pas"
    override val passwordChangedSuccessfully = "Mot de passe modifié avec succès"
    override val userNotFound = "Utilisateur introuvable"
    override val incorrectPassword = "Mot de passe actuel incorrect"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Synchronisé"
    override val pendingChanges = "{count} modification(s) en attente"
    override val conflictsToResolve = "{count} conflit(s) à résoudre"
    override val online = "En ligne"
    override val offline = "Hors ligne"
    override val realtimeConnected = "Temps réel connecté"
    override val realtimeDisconnected = "Temps réel déconnecté"
    override val lastError = "Dernière erreur"
    override val offlineMode = "Mode hors ligne"
    override val conflictsDetected = "Conflits détectés"
    override val changesWillSyncWhenOnline = "Les modifications seront synchronisées lorsque vous serez de nouveau en ligne"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Paramètres"
    override val language = "Langue"
    override val selectLanguage = "Sélectionner la langue"
    override val theme = "Thème"
    override val darkMode = "Mode sombre"
    override val lightMode = "Mode clair"
    override val systemDefault = "Défaut système"
    override val about = "À propos"
    override val version = "Version"
    override val syncSettings = "Paramètres de synchronisation"
    override val lastSync = "Dernière synchronisation"
    override val syncNow = "Synchroniser maintenant"
    override val syncing = "Synchronisation..."
    override val syncSuccess = "Synchronisation terminée"
    override val syncError = "Échec de la synchronisation"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Utilisateurs"
    override val user = "Utilisateur"
    override val addUser = "Ajouter un utilisateur"
    override val editUser = "Modifier l'utilisateur"
    override val deleteUser = "Supprimer l'utilisateur"
    override val fullName = "Nom complet"
    override val role = "Rôle"
    override val admin = "Administrateur"
    override val permissions = "Permissions"
    override val canView = "Peut consulter"
    override val canCreate = "Peut créer"
    override val canEdit = "Peut modifier"
    override val canDelete = "Peut supprimer"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Types de conditionnement"
    override val packagingType = "Type de conditionnement"
    override val addPackagingType = "Ajouter un type de conditionnement"
    override val editPackagingType = "Modifier le type de conditionnement"
    override val level1Name = "Nom niveau 1"
    override val level2Name = "Nom niveau 2"
    override val level2Quantity = "Quantité niveau 2"
    override val conversionFactor = "Facteur de conversion"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} est obligatoire"
    override val invalidEmail = "Adresse e-mail invalide"
    override val invalidPhone = "Numéro de téléphone invalide"
    override val valueTooShort = "{field} doit contenir au moins {min} caractères"
    override val valueTooLong = "{field} ne doit pas dépasser {max} caractères"
    override val valueMustBePositive = "La valeur doit être positive"
    override val passwordTooShort = "Le mot de passe doit contenir au moins 8 caractères"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Au moins 8 caractères"
    override val passwordNeedsUppercase = "Au moins une lettre majuscule (A-Z)"
    override val passwordNeedsLowercase = "Au moins une lettre minuscule (a-z)"
    override val passwordNeedsDigit = "Au moins un chiffre (0-9)"
    override val passwordNeedsSpecial = "Au moins un caractère spécial (!@#\$%...)"
    override val passwordStrengthWeak = "Faible"
    override val passwordStrengthMedium = "Moyen"
    override val passwordStrengthStrong = "Fort"
    override val passwordRequirements = "Exigences du mot de passe :"
    override val passwordStrength = "Force du mot de passe :"
    override val passwordMustBeDifferent = "Le nouveau mot de passe doit être différent de l'actuel"
    override val usernameAlreadyExists = "Ce nom d'utilisateur existe déjà"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Impossible de supprimer"
    override val entityInUse = "Ce(tte) {entity} est utilisé(e) dans {count} enregistrements"
    override val deactivateInstead = "Voulez-vous le/la désactiver à la place ?"
    override val deactivate = "Désactiver"
    override val reactivate = "Réactiver"
    override val showInactive = "Afficher les inactifs"
    override val hideInactive = "Masquer les inactifs"
    override val inactive = "Inactif"
    override val active = "Actif"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Aujourd'hui"
    override val yesterday = "Hier"
    override val thisWeek = "Cette semaine"
    override val thisMonth = "Ce mois-ci"
    override val thisYear = "Cette année"
    override val dateFormat = "dd/MM/yyyy"
    override val timeFormat = "HH:mm"

    // ============================================
    // NUMBERS & CURRENCY
    // ============================================
    override val currencySymbol = "€"
    override val currencyFormat = "{amount} {symbol}"
    override val decimalSeparator = ","
    override val thousandsSeparator = " "

    // ============================================
    // NOTIFICATIONS
    // ============================================
    override val notificationSettings = "Paramètres des notifications"
    override val notificationExpiryAlerts = "Alertes d'expiration"
    override val notificationEnableExpiry = "Activer les alertes d'expiration"
    override val notificationWarningDays = "Jours d'avertissement avant expiration"
    override val notificationExpiryDescription = "Recevoir des alertes lorsque des produits sont sur le point d'expirer"
    override val notificationLowStockAlerts = "Alertes de stock bas"
    override val notificationEnableLowStock = "Activer les alertes de stock bas"
    override val notificationLowStockDescription = "Recevoir des alertes lorsque le stock descend sous le seuil minimum"
    override val notificationInvalidDays = "Veuillez entrer un nombre de jours valide (1-365)"
    override val settingsSaved = "Paramètres enregistrés avec succès"
    override val supabaseNotConfigured = "Supabase n'est pas configuré"
    override val notifications = "Notifications"
    override val noNotifications = "Aucune notification"
    override val dismissAll = "Tout acquitter"
    override val allNotificationsDismissed = "Toutes les notifications ont été acquittées"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Site actuel"
    override val operations = "Opérations"
    override val purchaseProducts = "Acheter des produits"
    override val sellProducts = "Vendre des produits"
    override val transferProducts = "Transférer des produits"
    override val viewStock = "Voir le stock"
    override val inventoryStock = "Inventaire du stock"
    override val administration = "Administration"
    override val management = "Gestion"
    override val siteManagement = "Gestion des sites"
    override val manageProducts = "Gérer les produits"
    override val manageCategories = "Gérer les catégories"
    override val manageCustomers = "Gérer les clients"
    override val userManagement = "Gestion des utilisateurs"
    override val history = "Historique"
    override val configuration = "Configuration"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Épuisé"
    override val remainingQty = "Qté restante"
    override val noPurchases = "Aucun achat trouvé"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "Aucune vente trouvée"
    override val saleDetails = "Détails de la vente"
    override val items = "Articles"
    override val total = "Total"
    override val date = "Date"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Inventaires"
    override val noInventories = "Aucun inventaire trouvé"
    override val inProgress = "En cours"
    override val completed = "Terminé"
    override val pending = "En attente"
    override val newInventory = "Nouvel inventaire"
    override val start = "Démarrer"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "Aucun transfert trouvé"
    override val sourceSite = "Site source"
    override val destinationSite = "Site destination"
    override val quantityToTransfer = "Quantité à transférer"
    override val create = "Créer"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "Aucune donnée de stock"
    override val summary = "Résumé"
    override val outOfStock = "Rupture de stock"
    override val stockByProduct = "Stock par produit"
    override val noMovements = "Aucun mouvement trouvé"
    override val availableStock = "Stock disponible"
    override val preview = "Aperçu"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "Aucun type d'emballage trouvé"
    override val addLevel2 = "Ajouter niveau 2"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Historique d'audit"
    override val noHistory = "Aucun historique trouvé"
    override val filterBy = "Filtrer par"
    override val all = "Tout"
    override val created = "Créé"
    override val updated = "Mis à jour"
    override val deleted = "Supprimé"
    override val details = "Détails"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Configuration Supabase"
    override val projectUrl = "URL du projet"
    override val anonKey = "Clé anonyme"
    override val synchronization = "Synchronisation"
    override val syncData = "Synchroniser les données"
    override val currentStatus = "État actuel"
    override val configured = "Configuré"
    override val connection = "Connexion"
    override val testConnection = "Tester la connexion"
    override val clearConfiguration = "Effacer la configuration"
    override val configSaved = "Configuration enregistrée"
    override val syncCompleted = "Synchronisation réussie"
    override val connectionSuccessful = "Connexion réussie"
    override val howToGetInfo = "Comment obtenir ces informations :"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Configurer Supabase"
    override val authentication = "Authentification"
    override val enterCredentials = "Veuillez entrer vos identifiants"
    override val invalidPassword = "Mot de passe invalide"
    override val accountDisabled = "Ce compte est désactivé"
    override val connectionError = "Erreur de connexion"
    override val firstLoginRequiresInternet = "La première connexion nécessite Internet"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "Voir"
    override val select = "Sélectionner"
    override val chooseProduct = "Choisir un produit"
    override val orSelect = "Ou sélectionner"
    override val enable = "Activer"
    override val later = "Plus tard"
    override val alertsDescription = "Les notifications vous alertent des produits expirés et du stock bas."
    override val justNow = "À l'instant"
    override val minutesAgo = "Il y a {count} min"
    override val hoursAgo = "Il y a {count} h"
    override val daysAgo = "Il y a {count} jour(s)"
    override val critical = "Critique"
    override val urgent = "Urgent"
    override val info = "Info"
    override val low = "Faible"
    override val nearestExpiry = "Expiration proche"
    override val lots = "lot(s)"
    override val addNote = "Ajouter une note..."
    override val saving = "Enregistrement..."
    override val continue_ = "Continuer"
    override val unknownSite = "Site inconnu"
    override val unknownProduct = "Produit inconnu"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Ajoutez votre premier produit pour commencer"
    override val noCustomersMessage = "Ajoutez votre premier client pour commencer"
    override val noCategoriesMessage = "Ajoutez votre première catégorie pour commencer"
    override val noSitesMessage = "Ajoutez votre premier site pour commencer"
    override val noPackagingTypesMessage = "Ajoutez votre premier type de conditionnement pour commencer"
    override val noInventoriesMessage = "Aucun inventaire n'a encore été effectué"
    override val noSalesMessage = "Aucune vente n'a encore été enregistrée"
    override val noPurchasesMessage = "Aucun achat n'a encore été enregistré"
    override val noTransfersMessage = "Aucun transfert n'a encore été enregistré"
    override val noStockMessage = "Aucun stock disponible"
    override val noMovementsMessage = "Aucun mouvement de stock enregistré"
    override val noUsersMessage = "Ajoutez votre premier utilisateur pour commencer"
    override val historyWillAppearHere = "L'historique d'audit apparaîtra ici"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Ajoutez d'abord un site"
    override val none = "Aucun"
    override val stockAlerts = "Alertes de stock"
    override val stockAlertDescription = "Définissez les niveaux de stock minimum et maximum pour recevoir des alertes"
    override val transferIn = "Transfert entrant"
    override val transferOut = "Transfert sortant"
    override val batches = "lots"
    override val noUsers = "Aucun utilisateur"
    override val adminHasAllPermissions = "Les administrateurs ont toutes les permissions"
    override val create_ = "Créer"
    override val selectSourceSiteFirst = "Sélectionnez d'abord le site source"
    override val entries = "entrées"
    override val optional = "optionnel"
    override val packagingTypeName = "Nom du type de conditionnement"
    override val started = "Démarré"
    override val saleItem = "Article de vente"
    override val purchaseBatch = "Lot d'achat"
    override val stockMovement = "Mouvement de stock"
    override val supabaseStep1 = "1. Allez sur supabase.com et créez un compte"
    override val supabaseStep2 = "2. Créez un nouveau projet"
    override val supabaseStep3 = "3. Allez dans Paramètres du projet > API pour trouver votre URL et clé anon"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Mise à jour requise"
    override val updateAvailable = "Mise à jour disponible"
    override val appVersionIncompatible = "La version de l'application n'est pas compatible avec la base de données. Veuillez mettre à jour l'application pour continuer."
    override val appVersion = "Version de l'application"
    override val minimumRequiredVersion = "Version minimale requise"
    override val databaseVersion = "Version de la base de données"
    override val toUpdate = "Pour mettre à jour"
    override val contactAdminForUpdate = "Contactez votre administrateur pour obtenir la dernière version de l'application."
    override val checkingCompatibility = "Vérification de la compatibilité..."
    override val download = "Télécharger"
    override val newVersionAvailable = "Une nouvelle version de MediStock est disponible."
    override val currentVersionLabel = "Version actuelle"
    override val newVersionLabel = "Nouvelle version"
    override val whatsNew = "Nouveautés"
    override val unableToLoadNotifications = "Impossible de charger les notifications"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Paramètres de l'application"
    override val currencySymbolSetting = "Symbole monétaire"
    override val currencySymbolDescription = "Symbole utilisé pour afficher les prix (ex: F, $, EUR)"
    override val settingsSavedSuccessfully = "Paramètres enregistrés avec succès"
    override val invalidCurrencySymbol = "Veuillez entrer un symbole monétaire valide"
}
