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
    override val addPurchase = "Ajouter un achat"
    override val purchaseHistory = "Historique des achats"
    override val supplier = "Fournisseur"
    override val supplierName = "Nom du fournisseur"
    override val batchNumber = "Numéro de lot"
    override val purchaseDate = "Date d'achat"
    override val expiryDate = "Date d'expiration"
    override val quantity = "Quantité"
    override val initialQuantity = "Quantité initiale"
    override val remainingQuantity = "Quantité restante"
    override val totalAmount = "Montant total"
    override val purchaseRecorded = "Achat enregistré"

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
    override val passwordTooShort = "Le mot de passe doit contenir au moins 6 caractères"

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
}
