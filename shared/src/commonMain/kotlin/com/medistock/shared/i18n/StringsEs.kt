package com.medistock.shared.i18n

/**
 * Spanish strings implementation.
 */
object StringsEs : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "Aceptar"
    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val delete = "Eliminar"
    override val edit = "Editar"
    override val add = "Añadir"
    override val search = "Buscar"
    override val loading = "Cargando..."
    override val error = "Error"
    override val success = "Éxito"
    override val warning = "Advertencia"
    override val confirm = "Confirmar"
    override val yes = "Sí"
    override val no = "No"
    override val close = "Cerrar"
    override val back = "Atrás"
    override val next = "Siguiente"
    override val retry = "Reintentar"
    override val noData = "No hay datos disponibles"
    override val required = "Obligatorio"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Iniciar sesión"
    override val username = "Nombre de usuario"
    override val password = "Contraseña"
    override val login = "Iniciar sesión"
    override val logout = "Cerrar sesión"
    override val logoutConfirm = "¿Está seguro de que desea cerrar sesión?"
    override val changePassword = "Cambiar contraseña"
    override val loginError = "Error de inicio de sesión"
    override val loginErrorInvalidCredentials = "Nombre de usuario o contraseña inválidos"
    override val welcomeBack = "¡Bienvenido de nuevo, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Inicio"
    override val dashboard = "Panel"
    override val quickActions = "Acciones rápidas"
    override val recentActivity = "Actividad reciente"
    override val todaySales = "Ventas de hoy"
    override val lowStock = "Stock bajo"
    override val pendingTransfers = "Transferencias pendientes"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Sitios"
    override val site = "Sitio"
    override val siteName = "Nombre del sitio"
    override val addSite = "Añadir sitio"
    override val editSite = "Editar sitio"
    override val deleteSite = "Eliminar sitio"
    override val deleteSiteConfirm = "¿Está seguro de que desea eliminar este sitio?"
    override val siteDeleted = "Sitio eliminado"
    override val siteDeactivated = "Sitio desactivado"
    override val noSites = "No se encontraron sitios"
    override val selectSite = "Seleccionar sitio"
    override val allSites = "Todos los sitios"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Categorías"
    override val category = "Categoría"
    override val categoryName = "Nombre de la categoría"
    override val addCategory = "Añadir categoría"
    override val editCategory = "Editar categoría"
    override val deleteCategory = "Eliminar categoría"
    override val deleteCategoryConfirm = "¿Está seguro de que desea eliminar esta categoría?"
    override val categoryDeleted = "Categoría eliminada"
    override val categoryDeactivated = "Categoría desactivada"
    override val noCategories = "No se encontraron categorías"
    override val selectCategory = "Seleccionar categoría"
    override val allCategories = "Todas las categorías"
    override val uncategorized = "Sin categoría"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Productos"
    override val product = "Producto"
    override val productName = "Nombre del producto"
    override val addProduct = "Añadir producto"
    override val editProduct = "Editar producto"
    override val deleteProduct = "Eliminar producto"
    override val deleteProductConfirm = "¿Está seguro de que desea eliminar este producto?"
    override val productDeleted = "Producto eliminado"
    override val productDeactivated = "Producto desactivado"
    override val noProducts = "No se encontraron productos"
    override val selectProduct = "Seleccionar producto"
    override val unit = "Unidad"
    override val unitVolume = "Volumen unitario"
    override val description = "Descripción"
    override val minStock = "Stock mínimo"
    override val maxStock = "Stock máximo"
    override val currentStock = "Stock actual"
    override val price = "Precio"
    override val purchasePrice = "Precio de compra"
    override val sellingPrice = "Precio de venta"
    override val margin = "Margen"
    override val marginType = "Tipo de margen"
    override val marginValue = "Valor del margen"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Clientes"
    override val customer = "Cliente"
    override val customerName = "Nombre del cliente"
    override val addCustomer = "Añadir cliente"
    override val editCustomer = "Editar cliente"
    override val deleteCustomer = "Eliminar cliente"
    override val deleteCustomerConfirm = "¿Está seguro de que desea eliminar este cliente?"
    override val customerDeleted = "Cliente eliminado"
    override val customerDeactivated = "Cliente desactivado"
    override val noCustomers = "No se encontraron clientes"
    override val selectCustomer = "Seleccionar cliente"
    override val phone = "Teléfono"
    override val email = "Correo electrónico"
    override val address = "Dirección"
    override val notes = "Notas"
    override val walkInCustomer = "Cliente ocasional"

    // ============================================
    // SUPPLIERS
    // ============================================
    override val suppliers = "Proveedores"
    override val addSupplier = "Añadir proveedor"
    override val editSupplier = "Editar proveedor"
    override val deleteSupplier = "Eliminar proveedor"
    override val deleteSupplierConfirm = "¿Está seguro de que desea eliminar este proveedor?"
    override val supplierDeleted = "Proveedor eliminado"
    override val supplierDeactivated = "Proveedor desactivado"
    override val noSuppliers = "No se encontraron proveedores"
    override val selectSupplier = "Seleccionar proveedor"
    override val manageSuppliers = "Gestionar proveedores"
    override val noSuppliersMessage = "Agregue su primer proveedor para comenzar"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Compras"
    override val purchase = "Compra"
    override val newPurchase = "Nueva compra"
    override val addPurchase = "Añadir compra"
    override val purchaseHistory = "Historial de compras"
    override val supplier = "Proveedor"
    override val supplierName = "Nombre del proveedor"
    override val batchNumber = "Número de lote"
    override val purchaseDate = "Fecha de compra"
    override val expiryDate = "Fecha de caducidad"
    override val expiryDateOptional = "Fecha de caducidad (opcional)"
    override val quantity = "Cantidad"
    override val initialQuantity = "Cantidad inicial"
    override val remainingQuantity = "Cantidad restante"
    override val totalAmount = "Importe total"
    override val purchaseRecorded = "Compra registrada"
    override val unitPurchasePrice = "Precio de compra unitario"
    override val unitSellingPrice = "Precio de venta unitario"
    override val marginCalculatedAuto = "Margen: calculado automáticamente"
    override val sellingPriceNote = "El precio de venta se calcula automáticamente según el margen del producto, pero puede modificarlo."
    override val savePurchase = "Guardar compra"
    override val enterSupplierName = "Ingrese el nombre del proveedor"
    override val batchNumberExample = "Ej: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Ventas"
    override val sale = "Venta"
    override val newSale = "Nueva venta"
    override val saleHistory = "Historial de ventas"
    override val saleDate = "Fecha de venta"
    override val saleTotal = "Total de la venta"
    override val saleItems = "Artículos de la venta"
    override val addItem = "Añadir artículo"
    override val removeItem = "Eliminar artículo"
    override val unitPrice = "Precio unitario"
    override val itemTotal = "Total del artículo"
    override val subtotal = "Subtotal"
    override val discount = "Descuento"
    override val grandTotal = "Total general"
    override val completeSale = "Completar venta"
    override val saleCompleted = "Venta completada"
    override val noSaleItems = "No hay artículos en la venta"
    override val insufficientStock = "Stock insuficiente para {product}: {available} disponibles, {requested} solicitados"
    override val remainingQuantityNeeded = "Cantidad restante necesaria: {quantity} unidades"
    override val editSale = "Editar venta"
    override val editPurchase = "Editar compra"
    override val productsToSell = "Productos a vender"
    override val addProductToSale = "+ Añadir producto"
    override val enterCustomerName = "Ingrese el nombre del cliente"
    override val pricePerUnit = "Precio por unidad"
    override val exampleQuantity = "Ej: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Inventario"
    override val inventoryCount = "Conteo de inventario"
    override val startInventory = "Iniciar inventario"
    override val completeInventory = "Completar inventario"
    override val inventoryInProgress = "Inventario en progreso"
    override val theoreticalQuantity = "Cantidad teórica"
    override val countedQuantity = "Cantidad contada"
    override val discrepancy = "Discrepancia"
    override val reason = "Motivo"
    override val inventoryCompleted = "Inventario completado"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Transferencias"
    override val transfer = "Transferencia"
    override val newTransfer = "Nueva transferencia"
    override val transferHistory = "Historial de transferencias"
    override val fromSite = "Sitio de origen"
    override val toSite = "Sitio de destino"
    override val transferStatus = "Estado de la transferencia"
    override val transferPending = "Pendiente"
    override val transferCompleted = "Completada"
    override val transferCancelled = "Cancelada"
    override val completeTransfer = "Completar transferencia"
    override val cancelTransfer = "Cancelar transferencia"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Stock"
    override val stockMovements = "Movimientos de stock"
    override val stockIn = "Entrada de stock"
    override val stockOut = "Salida de stock"
    override val stockAdjustment = "Ajuste de stock"
    override val movementType = "Tipo de movimiento"
    override val movementDate = "Fecha del movimiento"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Informes"
    override val salesReport = "Informe de ventas"
    override val stockReport = "Informe de stock"
    override val profitReport = "Informe de beneficios"
    override val exportReport = "Exportar informe"
    override val dateRange = "Período"
    override val startDate = "Fecha de inicio"
    override val endDate = "Fecha de fin"
    override val generateReport = "Generar informe"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Perfil"
    override val myProfile = "Mi perfil"
    override val information = "Información"
    override val currentPassword = "Contraseña actual"
    override val newPassword = "Nueva contraseña"
    override val confirmPassword = "Confirmar contraseña"
    override val passwordsDoNotMatch = "Las contraseñas no coinciden"
    override val passwordChangedSuccessfully = "Contraseña cambiada exitosamente"
    override val userNotFound = "Usuario no encontrado"
    override val incorrectPassword = "Contraseña actual incorrecta"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Sincronizado"
    override val pendingChanges = "{count} cambio(s) pendiente(s)"
    override val conflictsToResolve = "{count} conflicto(s) por resolver"
    override val online = "En línea"
    override val offline = "Sin conexión"
    override val realtimeConnected = "Tiempo real conectado"
    override val realtimeDisconnected = "Tiempo real desconectado"
    override val lastError = "Último error"
    override val offlineMode = "Modo sin conexión"
    override val conflictsDetected = "Conflictos detectados"
    override val changesWillSyncWhenOnline = "Los cambios se sincronizarán cuando vuelva a estar en línea"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Configuración"
    override val language = "Idioma"
    override val selectLanguage = "Seleccionar idioma"
    override val theme = "Tema"
    override val darkMode = "Modo oscuro"
    override val lightMode = "Modo claro"
    override val systemDefault = "Predeterminado del sistema"
    override val about = "Acerca de"
    override val version = "Versión"
    override val syncSettings = "Configuración de sincronización"
    override val lastSync = "Última sincronización"
    override val syncNow = "Sincronizar ahora"
    override val syncing = "Sincronizando..."
    override val syncSuccess = "Sincronización completada"
    override val syncError = "Error de sincronización"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Usuarios"
    override val user = "Usuario"
    override val addUser = "Añadir usuario"
    override val editUser = "Editar usuario"
    override val deleteUser = "Eliminar usuario"
    override val fullName = "Nombre completo"
    override val role = "Rol"
    override val admin = "Administrador"
    override val permissions = "Permisos"
    override val canView = "Puede ver"
    override val canCreate = "Puede crear"
    override val canEdit = "Puede editar"
    override val canDelete = "Puede eliminar"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Tipos de empaque"
    override val packagingType = "Tipo de empaque"
    override val addPackagingType = "Añadir tipo de empaque"
    override val editPackagingType = "Editar tipo de empaque"
    override val level1Name = "Nombre nivel 1"
    override val level2Name = "Nombre nivel 2"
    override val level2Quantity = "Cantidad nivel 2"
    override val conversionFactor = "Factor de conversión"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "{field} es obligatorio"
    override val invalidEmail = "Dirección de correo electrónico inválida"
    override val invalidPhone = "Número de teléfono inválido"
    override val valueTooShort = "{field} debe tener al menos {min} caracteres"
    override val valueTooLong = "{field} no debe exceder {max} caracteres"
    override val valueMustBePositive = "El valor debe ser positivo"
    override val passwordTooShort = "La contraseña debe tener al menos 8 caracteres"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Al menos 8 caracteres"
    override val passwordNeedsUppercase = "Al menos una letra mayúscula (A-Z)"
    override val passwordNeedsLowercase = "Al menos una letra minúscula (a-z)"
    override val passwordNeedsDigit = "Al menos un dígito (0-9)"
    override val passwordNeedsSpecial = "Al menos un carácter especial (!@#\$%...)"
    override val passwordStrengthWeak = "Débil"
    override val passwordStrengthMedium = "Media"
    override val passwordStrengthStrong = "Fuerte"
    override val passwordRequirements = "Requisitos de contraseña:"
    override val passwordStrength = "Fortaleza de la contraseña:"
    override val passwordMustBeDifferent = "La nueva contraseña debe ser diferente de la actual"
    override val usernameAlreadyExists = "El nombre de usuario ya existe"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "No se puede eliminar"
    override val entityInUse = "Este/a {entity} se usa en {count} registros"
    override val deactivateInstead = "¿Desea desactivarlo/a en su lugar?"
    override val deactivate = "Desactivar"
    override val reactivate = "Reactivar"
    override val showInactive = "Mostrar inactivos"
    override val hideInactive = "Ocultar inactivos"
    override val inactive = "Inactivo"
    override val active = "Activo"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Hoy"
    override val yesterday = "Ayer"
    override val thisWeek = "Esta semana"
    override val thisMonth = "Este mes"
    override val thisYear = "Este año"
    override val dateFormat = "dd/MM/yyyy"
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
    override val notificationSettings = "Configuración de notificaciones"
    override val notificationExpiryAlerts = "Alertas de caducidad"
    override val notificationEnableExpiry = "Activar notificaciones de caducidad"
    override val notificationWarningDays = "Días de aviso antes de caducidad"
    override val notificationExpiryDescription = "Recibir alertas cuando los productos estén por caducar"
    override val notificationLowStockAlerts = "Alertas de stock bajo"
    override val notificationEnableLowStock = "Activar notificaciones de stock bajo"
    override val notificationLowStockDescription = "Recibir alertas cuando el stock caiga por debajo del umbral mínimo"
    override val notificationInvalidDays = "Por favor, introduzca un número de días válido (1-365)"
    override val settingsSaved = "Configuración guardada correctamente"
    override val supabaseNotConfigured = "Supabase no está configurado"
    override val notifications = "Notificaciones"
    override val noNotifications = "Sin notificaciones"
    override val dismissAll = "Descartar todo"
    override val allNotificationsDismissed = "Todas las notificaciones han sido descartadas"

    // ============================================
    // HOME / OPERATIONS
    // ============================================
    override val currentSite = "Sitio actual"
    override val operations = "Operaciones"
    override val purchaseProducts = "Comprar productos"
    override val sellProducts = "Vender productos"
    override val transferProducts = "Transferir productos"
    override val viewStock = "Ver stock"
    override val inventoryStock = "Inventario de stock"
    override val administration = "Administración"
    override val management = "Gestión"
    override val siteManagement = "Gestión de sitios"
    override val manageProducts = "Gestionar productos"
    override val manageCategories = "Gestionar categorías"
    override val manageCustomers = "Gestionar clientes"
    override val userManagement = "Gestión de usuarios"
    override val history = "Historial"
    override val configuration = "Configuración"

    // ============================================
    // PURCHASES EXTENDED
    // ============================================
    override val exhausted = "Agotado"
    override val remainingQty = "Cantidad restante"
    override val noPurchases = "No se encontraron compras"

    // ============================================
    // SALES EXTENDED
    // ============================================
    override val noSales = "No se encontraron ventas"
    override val saleDetails = "Detalles de venta"
    override val items = "Artículos"
    override val total = "Total"
    override val date = "Fecha"

    // ============================================
    // INVENTORY EXTENDED
    // ============================================
    override val inventories = "Inventarios"
    override val noInventories = "No se encontraron inventarios"
    override val inProgress = "En progreso"
    override val completed = "Completado"
    override val pending = "Pendiente"
    override val newInventory = "Nuevo inventario"
    override val start = "Iniciar"

    // ============================================
    // TRANSFERS EXTENDED
    // ============================================
    override val noTransfers = "No se encontraron transferencias"
    override val sourceSite = "Sitio de origen"
    override val destinationSite = "Sitio de destino"
    override val quantityToTransfer = "Cantidad a transferir"
    override val create = "Crear"

    // ============================================
    // STOCK EXTENDED
    // ============================================
    override val noStock = "Sin datos de stock"
    override val summary = "Resumen"
    override val outOfStock = "Sin stock"
    override val stockByProduct = "Stock por producto"
    override val noMovements = "No se encontraron movimientos"
    override val availableStock = "Stock disponible"
    override val preview = "Vista previa"

    // ============================================
    // PACKAGING EXTENDED
    // ============================================
    override val noPackagingTypes = "No se encontraron tipos de embalaje"
    override val addLevel2 = "Agregar nivel 2"

    // ============================================
    // AUDIT
    // ============================================
    override val auditHistory = "Historial de auditoría"
    override val noHistory = "No se encontró historial"
    override val filterBy = "Filtrar por"
    override val all = "Todo"
    override val created = "Creado"
    override val updated = "Actualizado"
    override val deleted = "Eliminado"
    override val details = "Detalles"

    // ============================================
    // SUPABASE
    // ============================================
    override val supabaseConfiguration = "Configuración de Supabase"
    override val projectUrl = "URL del proyecto"
    override val anonKey = "Clave anónima"
    override val synchronization = "Sincronización"
    override val syncData = "Sincronizar datos"
    override val currentStatus = "Estado actual"
    override val configured = "Configurado"
    override val connection = "Conexión"
    override val testConnection = "Probar conexión"
    override val clearConfiguration = "Borrar configuración"
    override val configSaved = "Configuración guardada"
    override val syncCompleted = "Sincronización completada"
    override val connectionSuccessful = "Conexión exitosa"
    override val howToGetInfo = "Cómo obtener esta información:"

    // ============================================
    // AUTH EXTENDED
    // ============================================
    override val configureSupabase = "Configurar Supabase"
    override val authentication = "Autenticación"
    override val enterCredentials = "Por favor ingrese sus credenciales"
    override val invalidPassword = "Contraseña inválida"
    override val accountDisabled = "Esta cuenta está desactivada"
    override val connectionError = "Error de conexión"
    override val firstLoginRequiresInternet = "El primer inicio de sesión requiere Internet"

    // ============================================
    // UI LABELS
    // ============================================
    override val view = "Ver"
    override val select = "Seleccionar"
    override val chooseProduct = "Elegir un producto"
    override val orSelect = "O seleccionar"
    override val enable = "Activar"
    override val later = "Más tarde"
    override val alertsDescription = "Las notificaciones le alertan sobre productos caducados y stock bajo."
    override val justNow = "Ahora mismo"
    override val minutesAgo = "Hace {count} min"
    override val hoursAgo = "Hace {count} h"
    override val daysAgo = "Hace {count} día(s)"
    override val critical = "Crítico"
    override val urgent = "Urgente"
    override val info = "Info"
    override val low = "Bajo"
    override val nearestExpiry = "Próxima caducidad"
    override val lots = "lote(s)"
    override val addNote = "Agregar nota..."
    override val saving = "Guardando..."
    override val continue_ = "Continuar"
    override val unknownSite = "Sitio desconocido"
    override val unknownProduct = "Producto desconocido"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Agregue su primer producto para comenzar"
    override val noCustomersMessage = "Agregue su primer cliente para comenzar"
    override val noCategoriesMessage = "Agregue su primera categoría para comenzar"
    override val noSitesMessage = "Agregue su primer sitio para comenzar"
    override val noPackagingTypesMessage = "Agregue su primer tipo de empaque para comenzar"
    override val noInventoriesMessage = "Aún no se han realizado inventarios"
    override val noSalesMessage = "Aún no se han registrado ventas"
    override val noPurchasesMessage = "Aún no se han registrado compras"
    override val noTransfersMessage = "Aún no se han registrado transferencias"
    override val noStockMessage = "Sin stock disponible"
    override val noMovementsMessage = "Sin movimientos de stock registrados"
    override val noUsersMessage = "Agregue su primer usuario para comenzar"
    override val historyWillAppearHere = "El historial de auditoría aparecerá aquí"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Primero agregue un sitio"
    override val none = "Ninguno"
    override val stockAlerts = "Alertas de stock"
    override val stockAlertDescription = "Configure niveles mínimos y máximos de stock para recibir alertas"
    override val transferIn = "Transferencia entrante"
    override val transferOut = "Transferencia saliente"
    override val batches = "lotes"
    override val noUsers = "Sin usuarios"
    override val adminHasAllPermissions = "Los administradores tienen todos los permisos"
    override val create_ = "Crear"
    override val selectSourceSiteFirst = "Primero seleccione el sitio de origen"
    override val entries = "entradas"
    override val optional = "opcional"
    override val packagingTypeName = "Nombre del tipo de empaque"
    override val started = "Iniciado"
    override val saleItem = "Artículo de venta"
    override val purchaseBatch = "Lote de compra"
    override val stockMovement = "Movimiento de stock"
    override val supabaseStep1 = "1. Vaya a supabase.com y cree una cuenta"
    override val supabaseStep2 = "2. Cree un nuevo proyecto"
    override val supabaseStep3 = "3. Vaya a Configuración del proyecto > API para encontrar su URL y clave anon"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Actualización requerida"
    override val updateAvailable = "Actualización disponible"
    override val appVersionIncompatible = "La versión de su aplicación no es compatible con la base de datos. Por favor, actualice la aplicación para continuar."
    override val appVersion = "Versión de la aplicación"
    override val minimumRequiredVersion = "Versión mínima requerida"
    override val databaseVersion = "Versión de la base de datos"
    override val toUpdate = "Para actualizar"
    override val contactAdminForUpdate = "Contacte a su administrador para obtener la última versión de la aplicación."
    override val checkingCompatibility = "Verificando compatibilidad..."
    override val download = "Descargar"
    override val newVersionAvailable = "Una nueva versión de MediStock está disponible."
    override val currentVersionLabel = "Versión actual"
    override val newVersionLabel = "Nueva versión"
    override val whatsNew = "Novedades"
    override val unableToLoadNotifications = "No se pueden cargar las notificaciones"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Configuración de la aplicación"
    override val currencySymbolSetting = "Símbolo de moneda"
    override val currencySymbolDescription = "Símbolo usado para mostrar precios (ej: F, $, EUR)"
    override val settingsSavedSuccessfully = "Configuración guardada correctamente"
    override val invalidCurrencySymbol = "Por favor, ingrese un símbolo de moneda válido"
}
