package com.medistock.shared.i18n

/**
 * Russian strings implementation.
 */
object StringsRu : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "ОК"
    override val cancel = "Отмена"
    override val save = "Сохранить"
    override val delete = "Удалить"
    override val edit = "Редактировать"
    override val add = "Добавить"
    override val search = "Поиск"
    override val loading = "Загрузка..."
    override val error = "Ошибка"
    override val success = "Успешно"
    override val warning = "Предупреждение"
    override val confirm = "Подтвердить"
    override val yes = "Да"
    override val no = "Нет"
    override val close = "Закрыть"
    override val back = "Назад"
    override val next = "Далее"
    override val retry = "Повторить"
    override val noData = "Данные отсутствуют"
    override val required = "Обязательно"

    // ============================================
    // AUTH
    // ============================================
    override val loginTitle = "Вход"
    override val username = "Имя пользователя"
    override val password = "Пароль"
    override val login = "Войти"
    override val logout = "Выйти"
    override val logoutConfirm = "Вы уверены, что хотите выйти?"
    override val changePassword = "Изменить пароль"
    override val loginError = "Не удалось войти"
    override val loginErrorInvalidCredentials = "Неверное имя пользователя или пароль"
    override val welcomeBack = "С возвращением, {name}!"

    // ============================================
    // HOME / DASHBOARD
    // ============================================
    override val home = "Главная"
    override val dashboard = "Панель"
    override val quickActions = "Быстрые действия"
    override val recentActivity = "Последние действия"
    override val todaySales = "Продажи за сегодня"
    override val lowStock = "Низкий остаток"
    override val pendingTransfers = "Ожидающие перемещения"

    // ============================================
    // SITES
    // ============================================
    override val sites = "Сайты"
    override val site = "Сайт"
    override val siteName = "Название сайта"
    override val addSite = "Добавить сайт"
    override val editSite = "Редактировать сайт"
    override val deleteSite = "Удалить сайт"
    override val deleteSiteConfirm = "Вы уверены, что хотите удалить этот сайт?"
    override val siteDeleted = "Сайт удалён"
    override val siteDeactivated = "Сайт деактивирован"
    override val noSites = "Сайты не найдены"
    override val selectSite = "Выберите сайт"
    override val allSites = "Все сайты"

    // ============================================
    // CATEGORIES
    // ============================================
    override val categories = "Категории"
    override val category = "Категория"
    override val categoryName = "Название категории"
    override val addCategory = "Добавить категорию"
    override val editCategory = "Редактировать категорию"
    override val deleteCategory = "Удалить категорию"
    override val deleteCategoryConfirm = "Вы уверены, что хотите удалить эту категорию?"
    override val categoryDeleted = "Категория удалена"
    override val categoryDeactivated = "Категория деактивирована"
    override val noCategories = "Категории не найдены"
    override val selectCategory = "Выберите категорию"
    override val allCategories = "Все категории"
    override val uncategorized = "Без категории"

    // ============================================
    // PRODUCTS
    // ============================================
    override val products = "Товары"
    override val product = "Товар"
    override val productName = "Название товара"
    override val addProduct = "Добавить товар"
    override val editProduct = "Редактировать товар"
    override val deleteProduct = "Удалить товар"
    override val deleteProductConfirm = "Вы уверены, что хотите удалить этот товар?"
    override val productDeleted = "Товар удалён"
    override val productDeactivated = "Товар деактивирован"
    override val noProducts = "Товары не найдены"
    override val selectProduct = "Выберите товар"
    override val unit = "Единица"
    override val unitVolume = "Объём единицы"
    override val description = "Описание"
    override val minStock = "Минимальный остаток"
    override val maxStock = "Максимальный остаток"
    override val currentStock = "Текущий остаток"
    override val price = "Цена"
    override val purchasePrice = "Закупочная цена"
    override val sellingPrice = "Цена продажи"
    override val margin = "Маржа"
    override val marginType = "Тип маржи"
    override val marginValue = "Значение маржи"

    // ============================================
    // CUSTOMERS
    // ============================================
    override val customers = "Клиенты"
    override val customer = "Клиент"
    override val customerName = "Имя клиента"
    override val addCustomer = "Добавить клиента"
    override val editCustomer = "Редактировать клиента"
    override val deleteCustomer = "Удалить клиента"
    override val deleteCustomerConfirm = "Вы уверены, что хотите удалить этого клиента?"
    override val customerDeleted = "Клиент удалён"
    override val customerDeactivated = "Клиент деактивирован"
    override val noCustomers = "Клиенты не найдены"
    override val selectCustomer = "Выберите клиента"
    override val phone = "Телефон"
    override val email = "Эл. почта"
    override val address = "Адрес"
    override val notes = "Заметки"
    override val walkInCustomer = "Случайный клиент"

    // ============================================
    // PURCHASES
    // ============================================
    override val purchases = "Закупки"
    override val purchase = "Закупка"
    override val newPurchase = "Новая закупка"
    override val addPurchase = "Добавить закупку"
    override val purchaseHistory = "История закупок"
    override val supplier = "Поставщик"
    override val supplierName = "Название поставщика"
    override val batchNumber = "Номер партии"
    override val purchaseDate = "Дата закупки"
    override val expiryDate = "Срок годности"
    override val expiryDateOptional = "Срок годности (необязательно)"
    override val quantity = "Количество"
    override val initialQuantity = "Начальное количество"
    override val remainingQuantity = "Остаток"
    override val totalAmount = "Итого"
    override val purchaseRecorded = "Закупка сохранена"
    override val unitPurchasePrice = "Закупочная цена за единицу"
    override val unitSellingPrice = "Цена продажи за единицу"
    override val marginCalculatedAuto = "Маржа: рассчитывается автоматически"
    override val sellingPriceNote = "Цена продажи рассчитывается автоматически на основе маржи товара, но её можно изменить."
    override val savePurchase = "Сохранить закупку"
    override val enterSupplierName = "Введите название поставщика"
    override val batchNumberExample = "Напр.: LOT2024001"

    // ============================================
    // SALES
    // ============================================
    override val sales = "Продажи"
    override val sale = "Продажа"
    override val newSale = "Новая продажа"
    override val saleHistory = "История продаж"
    override val saleDate = "Дата продажи"
    override val saleTotal = "Сумма продажи"
    override val saleItems = "Позиции продажи"
    override val addItem = "Добавить позицию"
    override val removeItem = "Удалить позицию"
    override val unitPrice = "Цена за единицу"
    override val itemTotal = "Сумма позиции"
    override val subtotal = "Промежуточный итог"
    override val discount = "Скидка"
    override val grandTotal = "Итого к оплате"
    override val completeSale = "Завершить продажу"
    override val saleCompleted = "Продажа завершена"
    override val noSaleItems = "Нет позиций в продаже"
    override val insufficientStock = "Недостаточно остатка для {product}: доступно {available}, запрошено {requested}"
    override val remainingQuantityNeeded = "Необходимое остаточное количество: {quantity} единиц"
    override val editSale = "Редактировать продажу"
    override val editPurchase = "Редактировать закупку"
    override val productsToSell = "Товары к продаже"
    override val addProductToSale = "+ Добавить товар"
    override val enterCustomerName = "Введите имя клиента"
    override val pricePerUnit = "Цена за единицу"
    override val exampleQuantity = "Напр.: 10"

    // ============================================
    // INVENTORY
    // ============================================
    override val inventory = "Инвентаризация"
    override val inventoryCount = "Подсчёт инвентаря"
    override val startInventory = "Начать инвентаризацию"
    override val completeInventory = "Завершить инвентаризацию"
    override val inventoryInProgress = "Инвентаризация идёт"
    override val theoreticalQuantity = "Теоретическое количество"
    override val countedQuantity = "Фактическое количество"
    override val discrepancy = "Расхождение"
    override val reason = "Причина"
    override val inventoryCompleted = "Инвентаризация завершена"

    // ============================================
    // TRANSFERS
    // ============================================
    override val transfers = "Перемещения"
    override val transfer = "Перемещение"
    override val newTransfer = "Новое перемещение"
    override val transferHistory = "История перемещений"
    override val fromSite = "Откуда"
    override val toSite = "Куда"
    override val transferStatus = "Статус"
    override val transferPending = "Ожидает"
    override val transferCompleted = "Завершено"
    override val transferCancelled = "Отменено"
    override val completeTransfer = "Завершить перемещение"
    override val cancelTransfer = "Отменить перемещение"

    // ============================================
    // STOCK
    // ============================================
    override val stock = "Склад"
    override val stockMovements = "Движения склада"
    override val stockIn = "Приход"
    override val stockOut = "Расход"
    override val stockAdjustment = "Корректировка"
    override val movementType = "Тип движения"
    override val movementDate = "Дата движения"

    // ============================================
    // REPORTS
    // ============================================
    override val reports = "Отчёты"
    override val salesReport = "Отчёт по продажам"
    override val stockReport = "Отчёт по складу"
    override val profitReport = "Отчёт по прибыли"
    override val exportReport = "Экспорт отчёта"
    override val dateRange = "Диапазон дат"
    override val startDate = "Дата начала"
    override val endDate = "Дата окончания"
    override val generateReport = "Сформировать отчёт"

    // ============================================
    // PROFILE
    // ============================================
    override val profile = "Профиль"
    override val myProfile = "Мой профиль"
    override val information = "Информация"
    override val currentPassword = "Текущий пароль"
    override val newPassword = "Новый пароль"
    override val confirmPassword = "Подтвердите пароль"
    override val passwordsDoNotMatch = "Пароли не совпадают"
    override val passwordChangedSuccessfully = "Пароль успешно изменён"
    override val userNotFound = "Пользователь не найден"
    override val incorrectPassword = "Неверный текущий пароль"

    // ============================================
    // SYNC STATUS
    // ============================================
    override val synced = "Синхронизировано"
    override val pendingChanges = "{count} ожидающих изменений"
    override val conflictsToResolve = "{count} конфликтов для решения"
    override val online = "Онлайн"
    override val offline = "Офлайн"
    override val realtimeConnected = "Режим реального времени подключён"
    override val realtimeDisconnected = "Режим реального времени отключён"
    override val lastError = "Последняя ошибка"
    override val offlineMode = "Офлайн-режим"
    override val conflictsDetected = "Обнаружены конфликты"
    override val changesWillSyncWhenOnline = "Изменения будут синхронизированы, когда вы снова подключитесь к сети"

    // ============================================
    // SETTINGS
    // ============================================
    override val settings = "Настройки"
    override val language = "Язык"
    override val selectLanguage = "Выберите язык"
    override val theme = "Тема"
    override val darkMode = "Тёмная тема"
    override val lightMode = "Светлая тема"
    override val systemDefault = "Как в системе"
    override val about = "О приложении"
    override val version = "Версия"
    override val syncSettings = "Настройки синхронизации"
    override val lastSync = "Последняя синхронизация"
    override val syncNow = "Синхронизировать"
    override val syncing = "Синхронизация..."
    override val syncSuccess = "Синхронизация завершена"
    override val syncError = "Ошибка синхронизации"

    // ============================================
    // USERS & PERMISSIONS
    // ============================================
    override val users = "Пользователи"
    override val user = "Пользователь"
    override val addUser = "Добавить пользователя"
    override val editUser = "Редактировать пользователя"
    override val deleteUser = "Удалить пользователя"
    override val fullName = "Полное имя"
    override val role = "Роль"
    override val admin = "Администратор"
    override val permissions = "Права"
    override val canView = "Просмотр"
    override val canCreate = "Создание"
    override val canEdit = "Редактирование"
    override val canDelete = "Удаление"

    // ============================================
    // PACKAGING TYPES
    // ============================================
    override val packagingTypes = "Типы упаковки"
    override val packagingType = "Тип упаковки"
    override val addPackagingType = "Добавить тип упаковки"
    override val editPackagingType = "Редактировать тип упаковки"
    override val level1Name = "Название уровня 1"
    override val level2Name = "Название уровня 2"
    override val level2Quantity = "Количество уровня 2"
    override val conversionFactor = "Коэффициент пересчёта"

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    override val fieldRequired = "Поле {field} обязательно"
    override val invalidEmail = "Некорректный адрес эл. почты"
    override val invalidPhone = "Некорректный номер телефона"
    override val valueTooShort = "Поле {field} должно быть минимум {min} символов"
    override val valueTooLong = "Поле {field} не должно превышать {max} символов"
    override val valueMustBePositive = "Значение должно быть положительным"
    override val passwordTooShort = "Пароль должен быть минимум 8 символов"

    // PASSWORD COMPLEXITY
    override val passwordMinLength = "Минимум 8 символов"
    override val passwordNeedsUppercase = "Минимум одна заглавная буква (A-Z)"
    override val passwordNeedsLowercase = "Минимум одна строчная буква (a-z)"
    override val passwordNeedsDigit = "Минимум одна цифра (0-9)"
    override val passwordNeedsSpecial = "Минимум один специальный символ (!@#\$%...)"
    override val passwordStrengthWeak = "Слабый"
    override val passwordStrengthMedium = "Средний"
    override val passwordStrengthStrong = "Сильный"
    override val passwordRequirements = "Требования к паролю:"
    override val passwordStrength = "Надёжность пароля:"
    override val passwordMustBeDifferent = "Новый пароль должен отличаться от текущего"
    override val usernameAlreadyExists = "Имя пользователя уже существует"

    // ============================================
    // REFERENTIAL INTEGRITY
    // ============================================
    override val cannotDelete = "Невозможно удалить"
    override val entityInUse = "{entity} используется в {count} записях"
    override val deactivateInstead = "Деактивировать вместо удаления?"
    override val deactivate = "Деактивировать"
    override val reactivate = "Активировать"
    override val showInactive = "Показать неактивные"
    override val hideInactive = "Скрыть неактивные"
    override val inactive = "Неактивно"
    override val active = "Активно"

    // ============================================
    // DATE & TIME
    // ============================================
    override val today = "Сегодня"
    override val yesterday = "Вчера"
    override val thisWeek = "На этой неделе"
    override val thisMonth = "В этом месяце"
    override val thisYear = "В этом году"
    override val dateFormat = "dd.MM.yyyy"
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
    override val notificationSettings = "Настройки уведомлений"
    override val notificationExpiryAlerts = "Уведомления об истечении срока"
    override val notificationEnableExpiry = "Включить уведомления об истечении срока"
    override val notificationWarningDays = "Дней предупреждения до истечения"
    override val notificationExpiryDescription = "Получать уведомления о скором истечении срока товаров"
    override val notificationLowStockAlerts = "Уведомления о низком запасе"
    override val notificationEnableLowStock = "Включить уведомления о низком запасе"
    override val notificationLowStockDescription = "Получать уведомления когда запас опускается ниже минимального"
    override val notificationInvalidDays = "Пожалуйста, введите корректное количество дней (1-365)"
    override val settingsSaved = "Настройки успешно сохранены"
    override val supabaseNotConfigured = "Supabase не настроен"
    override val notifications = "Уведомления"
    override val noNotifications = "Нет уведомлений"
    override val dismissAll = "Отклонить все"
    override val allNotificationsDismissed = "Все уведомления отклонены"

    override val currentSite = "Текущий сайт"
    override val operations = "Операции"
    override val purchaseProducts = "Закупка товаров"
    override val sellProducts = "Продажа товаров"
    override val transferProducts = "Перемещение товаров"
    override val viewStock = "Просмотр запасов"
    override val inventoryStock = "Инвентаризация"
    override val administration = "Администрирование"
    override val management = "Управление"
    override val siteManagement = "Управление сайтами"
    override val manageProducts = "Управление товарами"
    override val manageCategories = "Управление категориями"
    override val manageCustomers = "Управление клиентами"
    override val userManagement = "Управление пользователями"
    override val history = "История"
    override val configuration = "Конфигурация"
    override val exhausted = "Исчерпано"
    override val remainingQty = "Остаток"
    override val noPurchases = "Закупки не найдены"
    override val noSales = "Продажи не найдены"
    override val saleDetails = "Детали продажи"
    override val items = "Товары"
    override val total = "Итого"
    override val date = "Дата"
    override val inventories = "Инвентаризации"
    override val noInventories = "Инвентаризации не найдены"
    override val inProgress = "В процессе"
    override val completed = "Завершено"
    override val pending = "Ожидание"
    override val newInventory = "Новая инвентаризация"
    override val start = "Начать"
    override val noTransfers = "Перемещения не найдены"
    override val sourceSite = "Исходный сайт"
    override val destinationSite = "Сайт назначения"
    override val quantityToTransfer = "Количество для перемещения"
    override val create = "Создать"
    override val noStock = "Нет данных о запасах"
    override val summary = "Сводка"
    override val outOfStock = "Нет в наличии"
    override val stockByProduct = "Запасы по товарам"
    override val noMovements = "Движения не найдены"
    override val availableStock = "Доступный запас"
    override val preview = "Предпросмотр"
    override val noPackagingTypes = "Типы упаковки не найдены"
    override val addLevel2 = "Добавить уровень 2"
    override val auditHistory = "История аудита"
    override val noHistory = "История не найдена"
    override val filterBy = "Фильтровать по"
    override val all = "Все"
    override val created = "Создано"
    override val updated = "Обновлено"
    override val deleted = "Удалено"
    override val details = "Подробности"
    override val supabaseConfiguration = "Конфигурация Supabase"
    override val projectUrl = "URL проекта"
    override val anonKey = "Анонимный ключ"
    override val synchronization = "Синхронизация"
    override val syncData = "Синхронизировать данные"
    override val currentStatus = "Текущий статус"
    override val configured = "Настроено"
    override val connection = "Соединение"
    override val testConnection = "Проверить соединение"
    override val clearConfiguration = "Очистить конфигурацию"
    override val configSaved = "Конфигурация сохранена"
    override val syncCompleted = "Синхронизация завершена"
    override val connectionSuccessful = "Соединение успешно"
    override val howToGetInfo = "Как получить эту информацию:"
    override val configureSupabase = "Настроить Supabase"
    override val authentication = "Аутентификация"
    override val enterCredentials = "Введите учетные данные"
    override val invalidPassword = "Неверный пароль"
    override val accountDisabled = "Этот аккаунт отключен"
    override val connectionError = "Ошибка соединения"
    override val firstLoginRequiresInternet = "Первый вход требует Интернет"
    override val view = "Просмотр"
    override val select = "Выбрать"
    override val chooseProduct = "Выберите товар"
    override val orSelect = "Или выберите"
    override val enable = "Включить"
    override val later = "Позже"
    override val alertsDescription = "Уведомления предупреждают о просроченных товарах и низком запасе."
    override val justNow = "Только что"
    override val minutesAgo = "{count} мин назад"
    override val hoursAgo = "{count} ч назад"
    override val daysAgo = "{count} дн назад"
    override val critical = "Критично"
    override val urgent = "Срочно"
    override val info = "Инфо"
    override val low = "Низкий"
    override val nearestExpiry = "Ближайшее истечение"
    override val lots = "партия(и)"
    override val addNote = "Добавить заметку..."
    override val saving = "Сохранение..."
    override val continue_ = "Продолжить"
    override val unknownSite = "Неизвестный сайт"
    override val unknownProduct = "Неизвестный товар"

    // EMPTY STATE MESSAGES
    override val noProductsMessage = "Добавьте первый товар, чтобы начать"
    override val noCustomersMessage = "Добавьте первого клиента, чтобы начать"
    override val noCategoriesMessage = "Добавьте первую категорию, чтобы начать"
    override val noSitesMessage = "Добавьте первый сайт, чтобы начать"
    override val noPackagingTypesMessage = "Добавьте первый тип упаковки, чтобы начать"
    override val noInventoriesMessage = "Инвентаризации ещё не проводились"
    override val noSalesMessage = "Продажи ещё не записаны"
    override val noPurchasesMessage = "Покупки ещё не записаны"
    override val noTransfersMessage = "Перемещения ещё не записаны"
    override val noStockMessage = "Нет доступного запаса"
    override val noMovementsMessage = "Движения запаса не записаны"
    override val noUsersMessage = "Добавьте первого пользователя, чтобы начать"
    override val historyWillAppearHere = "История аудита появится здесь"

    // ADDITIONAL UI STRINGS
    override val addSiteFirst = "Сначала добавьте сайт"
    override val none = "Нет"
    override val stockAlerts = "Уведомления о запасах"
    override val stockAlertDescription = "Установите минимальный и максимальный уровни запаса для получения уведомлений"
    override val transferIn = "Входящее перемещение"
    override val transferOut = "Исходящее перемещение"
    override val batches = "партий"
    override val noUsers = "Нет пользователей"
    override val adminHasAllPermissions = "Администраторы имеют все разрешения"
    override val create_ = "Создать"
    override val selectSourceSiteFirst = "Сначала выберите исходный сайт"
    override val entries = "записей"
    override val optional = "необязательно"
    override val packagingTypeName = "Название типа упаковки"
    override val started = "Начато"
    override val saleItem = "Позиция продажи"
    override val purchaseBatch = "Партия покупки"
    override val stockMovement = "Движение запаса"
    override val supabaseStep1 = "1. Перейдите на supabase.com и создайте аккаунт"
    override val supabaseStep2 = "2. Создайте новый проект"
    override val supabaseStep3 = "3. Перейдите в Настройки проекта > API, чтобы найти URL и anon-ключ"

    // ============================================
    // APP UPDATE
    // ============================================
    override val updateRequired = "Требуется обновление"
    override val updateAvailable = "Доступно обновление"
    override val appVersionIncompatible = "Версия вашего приложения несовместима с базой данных. Пожалуйста, обновите приложение, чтобы продолжить."
    override val appVersion = "Версия приложения"
    override val minimumRequiredVersion = "Минимальная требуемая версия"
    override val databaseVersion = "Версия базы данных"
    override val toUpdate = "Для обновления"
    override val contactAdminForUpdate = "Обратитесь к вашему администратору, чтобы получить последнюю версию приложения."
    override val checkingCompatibility = "Проверка совместимости..."
    override val download = "Скачать"
    override val newVersionAvailable = "Доступна новая версия MediStock."
    override val currentVersionLabel = "Текущая версия"
    override val newVersionLabel = "Новая версия"
    override val whatsNew = "Что нового"
    override val unableToLoadNotifications = "Не удалось загрузить уведомления"

    // ============================================
    // APP SETTINGS
    // ============================================
    override val appSettings = "Настройки приложения"
    override val currencySymbolSetting = "Символ валюты"
    override val currencySymbolDescription = "Символ для отображения цен (напр: F, $, EUR)"
    override val settingsSavedSuccessfully = "Настройки успешно сохранены"
    override val invalidCurrencySymbol = "Пожалуйста, введите корректный символ валюты"
}
