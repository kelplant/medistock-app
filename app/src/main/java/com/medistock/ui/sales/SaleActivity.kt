package com.medistock.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Customer
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.Sale
import com.medistock.shared.domain.model.SaleItem
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.shared.domain.model.SaleBatchAllocation
import com.medistock.shared.domain.model.StockMovement
import com.medistock.ui.adapters.SaleItemAdapter
import com.medistock.ui.LocalizedActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaleActivity : LocalizedActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var spinnerSite: Spinner
    private lateinit var recyclerSaleItems: RecyclerView
    private lateinit var btnAddProduct: Button
    private lateinit var editCustomerName: EditText
    private lateinit var textTotalAmount: TextView
    private lateinit var btnSaveSale: Button

    private lateinit var saleItemAdapter: SaleItemAdapter
    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var packagingTypes: Map<String, PackagingType> = emptyMap()
    private var currentStock: Map<String, Double> = emptyMap()
    private var selectedSiteId: String? = null
    private var editingSaleId: String? = null
    private var existingSale: Sale? = null
    private var selectedCustomer: Customer? = null

    private fun getUnit(product: Product?): String {
        if (product == null) return ""
        val packagingType = packagingTypes[product.packagingTypeId]
        return packagingType?.getLevelName(product.selectedLevel) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)
        selectedSiteId = PrefsHelper.getActiveSiteId(this)
        editingSaleId = intent.getStringExtra("SALE_ID")?.takeIf { it.isNotBlank() }

        spinnerSite = findViewById(R.id.spinnerSiteSale)
        recyclerSaleItems = findViewById(R.id.recyclerSaleItems)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        editCustomerName = findViewById(R.id.editCustomerName)
        textTotalAmount = findViewById(R.id.textTotalAmount)
        btnSaveSale = findViewById(R.id.btnSaveSale)

        // Setup RecyclerView
        saleItemAdapter = SaleItemAdapter(mutableListOf()) { item ->
            removeItem(item)
        }
        recyclerSaleItems.layoutManager = LinearLayoutManager(this)
        recyclerSaleItems.adapter = saleItemAdapter

        loadSites()
        loadProducts()

        spinnerSite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < sites.size) {
                    selectedSiteId = sites[position].id
                    loadStockForSite()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Make customer name field clickable to open customer selection
        editCustomerName.isFocusable = false
        editCustomerName.isClickable = true
        editCustomerName.setOnClickListener {
            showCustomerSelectionDialog()
        }

        btnAddProduct.setOnClickListener {
            showAddProductDialog()
        }

        btnSaveSale.setOnClickListener {
            saveSale()
        }

        // If editing, load the existing sale
        editingSaleId?.let { saleId ->
            supportActionBar?.title = strings.editSale
            loadExistingSale(saleId)
        } ?: run {
            supportActionBar?.title = strings.newSale
        }
    }

    override fun applyLocalizedStrings() {
        // Labels
        findViewById<TextView>(R.id.labelRecordSale)?.text = strings.newSale
        findViewById<TextView>(R.id.labelSiteSale)?.text = strings.site
        findViewById<TextView>(R.id.labelProductsToSell)?.text = strings.productsToSell
        findViewById<TextView>(R.id.labelCustomerName)?.text = strings.customerName

        // Hints/Placeholders
        editCustomerName.hint = strings.enterCustomerName

        // Buttons
        btnAddProduct.text = strings.addProductToSale
        btnSaveSale.text = strings.completeSale
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = sdk.siteRepository.getAll()
            withContext(Dispatchers.Main) {
                val siteNames = sites.map { it.name }
                val adapter = ArrayAdapter(
                    this@SaleActivity,
                    android.R.layout.simple_spinner_item,
                    siteNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSite.adapter = adapter

                // Select the current active site or editing sale's site
                val targetSiteId = existingSale?.siteId ?: selectedSiteId
                val currentSiteIndex = sites.indexOfFirst { it.id == targetSiteId }
                if (currentSiteIndex >= 0) {
                    spinnerSite.setSelection(currentSiteIndex)
                }
            }
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch(Dispatchers.IO) {
            products = sdk.productRepository.getAll()
            packagingTypes = sdk.packagingTypeRepository.getAll().associateBy { it.id }
        }
    }

    private fun loadStockForSite() {
        lifecycleScope.launch(Dispatchers.IO) {
            val siteId = selectedSiteId ?: return@launch
            val stockItems = sdk.stockRepository.getCurrentStockForSite(siteId)
            currentStock = stockItems.associate { it.productId to it.quantityOnHand }
        }
    }

    private fun loadExistingSale(saleId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val saleWithItems = sdk.saleRepository.getSaleWithItems(saleId)
            saleWithItems?.let { swi ->
                existingSale = swi.sale
                withContext(Dispatchers.Main) {
                    editCustomerName.setText(swi.sale.customerName)
                    selectedSiteId = swi.sale.siteId

                    // Load items into adapter
                    saleItemAdapter.clear()
                    swi.items.forEach { item ->
                        saleItemAdapter.addItem(item)
                    }
                    updateTotal()
                    updateSaveButtonState()

                    // Reload sites to set the correct one
                    loadSites()
                }
            }
        }
    }

    private fun showAddProductDialog() {
        if (products.isEmpty()) {
            Toast.makeText(this, strings.noProducts, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sale_item, null)
        val spinnerProduct = dialogView.findViewById<Spinner>(R.id.spinnerProductDialog)
        val textAvailableStock = dialogView.findViewById<TextView>(R.id.textAvailableStock)
        val radioGroupLevel = dialogView.findViewById<RadioGroup>(R.id.radioGroupLevel)
        val radioLevel1 = dialogView.findViewById<RadioButton>(R.id.radioLevel1)
        val radioLevel2 = dialogView.findViewById<RadioButton>(R.id.radioLevel2)
        val labelBatchSelection = dialogView.findViewById<TextView>(R.id.labelBatchSelection)
        val spinnerBatchDialog = dialogView.findViewById<Spinner>(R.id.spinnerBatchDialog)
        val textBatchExpiryWarning = dialogView.findViewById<TextView>(R.id.textBatchExpiryWarning)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editQuantityDialog)
        val textPurchasePrice = dialogView.findViewById<TextView>(R.id.textPurchasePrice)
        val editPrice = dialogView.findViewById<EditText>(R.id.editPriceDialog)
        val textMarginInfo = dialogView.findViewById<TextView>(R.id.textMarginInfo)

        val productNames = products.map { "${it.name} (${getUnit(it)})" }
        val productAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = productAdapter

        var selectedProduct: Product? = null
        var selectedLevel: Int = 1
        var latestPurchasePrice: Double? = null
        var sortedBatches: List<PurchaseBatch> = emptyList()
        var selectedBatchId: String? = null

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        fun updateLevelUI(product: Product) {
            val packagingType = packagingTypes[product.packagingTypeId]
            if (packagingType != null && packagingType.hasTwoLevels()) {
                radioGroupLevel.visibility = View.VISIBLE
                radioLevel1.text = packagingType.level1Name
                radioLevel2.text = packagingType.level2Name
            } else {
                radioGroupLevel.visibility = View.GONE
                selectedLevel = 1
            }
        }

        fun getConversionFactor(product: Product): Double? {
            return product.conversionFactor
        }

        fun updateStockDisplay(product: Product) {
            val availableQty = currentStock[product.id] ?: 0.0
            val packagingType = packagingTypes[product.packagingTypeId]
            val levelName = if (selectedLevel == 2 && packagingType?.hasTwoLevels() == true) {
                val cf = getConversionFactor(product)
                if (cf != null && cf > 0) {
                    val level2Qty = availableQty / cf
                    textAvailableStock.text = "${strings.availableStock}: ${String.format("%.1f", level2Qty)} ${packagingType.level2Name} (${"%.0f".format(availableQty)} ${packagingType.level1Name})"
                    return
                }
                packagingType.getLevelName(selectedLevel) ?: ""
            } else {
                packagingType?.getLevelName(selectedLevel) ?: ""
            }
            textAvailableStock.text = "${strings.availableStock}: $availableQty $levelName"
        }

        fun isBatchExpiringSoon(batch: PurchaseBatch): Boolean {
            val expiryDate = batch.expiryDate ?: return false
            return expiryDate < System.currentTimeMillis() + thirtyDaysMs
        }

        fun updateBatchExpiryWarning(batch: PurchaseBatch?) {
            if (batch != null && isBatchExpiringSoon(batch)) {
                val expiryStr = batch.expiryDate?.let { dateFormat.format(Date(it)) } ?: ""
                textBatchExpiryWarning.text = "Lot proche de l'expiration (expire le $expiryStr)"
                textBatchExpiryWarning.visibility = View.VISIBLE
            } else {
                textBatchExpiryWarning.visibility = View.GONE
            }
        }

        fun updatePurchasePriceDisplay(product: Product) {
            val pp = latestPurchasePrice
            if (pp != null && pp > 0) {
                val packagingType = packagingTypes[product.packagingTypeId]
                val displayPrice = if (selectedLevel == 2 && packagingType?.hasTwoLevels() == true) {
                    val cf = getConversionFactor(product) ?: 1.0
                    pp * cf
                } else {
                    pp
                }
                textPurchasePrice.text = "${strings.purchasePrice}: ${String.format("%.2f", displayPrice)}"
                textPurchasePrice.visibility = View.VISIBLE
            } else {
                textPurchasePrice.visibility = View.GONE
            }
        }

        fun updateMarginDisplay(product: Product) {
            val pp = latestPurchasePrice
            val sellingPrice = editPrice.text.toString().toDoubleOrNull()
            if (pp != null && pp > 0 && sellingPrice != null && sellingPrice > 0) {
                val effectivePurchasePrice = if (selectedLevel == 2) {
                    val cf = getConversionFactor(product) ?: 1.0
                    pp * cf
                } else {
                    pp
                }
                val marginAmount = sellingPrice - effectivePurchasePrice
                val marginPercent = if (effectivePurchasePrice > 0) (marginAmount / effectivePurchasePrice) * 100 else 0.0
                textMarginInfo.text = "${strings.margin}: ${String.format("%.2f", marginAmount)} (${String.format("%.1f", marginPercent)}%)"
                textMarginInfo.visibility = View.VISIBLE
            } else {
                textMarginInfo.visibility = View.GONE
            }
        }

        fun calculateSuggestedPrice(product: Product): Double? {
            val pp = latestPurchasePrice ?: return null
            if (pp <= 0) return null
            val effectivePurchasePrice = if (selectedLevel == 2) {
                val cf = getConversionFactor(product) ?: 1.0
                pp * cf
            } else {
                pp
            }
            val marginValue = product.marginValue ?: 0.0
            return when (product.marginType) {
                "fixed" -> {
                    if (selectedLevel == 2) {
                        val cf = getConversionFactor(product) ?: 1.0
                        effectivePurchasePrice + marginValue * cf
                    } else {
                        effectivePurchasePrice + marginValue
                    }
                }
                "percentage" -> effectivePurchasePrice * (1 + marginValue / 100)
                else -> null
            }
        }

        fun updatePriceFromBatch(product: Product, batch: PurchaseBatch?) {
            if (batch != null) {
                latestPurchasePrice = batch.purchasePrice
            }
            val suggestedPrice = calculateSuggestedPrice(product)
            if (suggestedPrice != null) {
                editPrice.setText(String.format("%.2f", suggestedPrice))
            }
            updatePurchasePriceDisplay(product)
            updateMarginDisplay(product)
        }

        fun loadBatchesAndPrice(product: Product) {
            lifecycleScope.launch(Dispatchers.IO) {
                val latestPrice = sdk.productPriceRepository.getLatestPrice(product.id)
                val batches = sdk.purchaseBatchRepository.getByProductAndSite(
                    product.id, selectedSiteId ?: ""
                ).filter { !it.isExhausted && it.remainingQuantity > 0 }

                // Smart batch suggestion: expiring soon first (by expiryDate ASC), then FIFO (purchaseDate ASC)
                val now = System.currentTimeMillis()
                val threshold = now + thirtyDaysMs
                val expiringSoon = batches
                    .filter { batch -> val exp = batch.expiryDate; exp != null && exp < threshold }
                    .sortedBy { it.expiryDate ?: Long.MAX_VALUE }
                val remaining = batches
                    .filter { batch -> val exp = batch.expiryDate; exp == null || exp >= threshold }
                    .sortedBy { it.purchaseDate }
                val sorted = expiringSoon + remaining

                withContext(Dispatchers.Main) {
                    sortedBatches = sorted

                    if (sorted.isNotEmpty()) {
                        labelBatchSelection.visibility = View.VISIBLE
                        spinnerBatchDialog.visibility = View.VISIBLE

                        val batchLabels = sorted.map { batch ->
                            val batchNum = batch.batchNumber ?: "N/A"
                            val remaining = String.format("%.0f", batch.remainingQuantity)
                            "Lot: $batchNum - ${String.format("%.0f", batch.purchasePrice)} FCFA (reste: $remaining)"
                        }
                        val batchAdapter = ArrayAdapter(
                            this@SaleActivity,
                            android.R.layout.simple_spinner_item,
                            batchLabels
                        )
                        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerBatchDialog.adapter = batchAdapter

                        // Pre-select first batch (the suggested one)
                        spinnerBatchDialog.setSelection(0)
                        val firstBatch = sorted.first()
                        selectedBatchId = firstBatch.id
                        latestPurchasePrice = firstBatch.purchasePrice
                        updateBatchExpiryWarning(firstBatch)
                    } else {
                        labelBatchSelection.visibility = View.GONE
                        spinnerBatchDialog.visibility = View.GONE
                        textBatchExpiryWarning.visibility = View.GONE
                        selectedBatchId = null
                        // Fallback to product price if no batches
                        latestPurchasePrice = latestPrice?.purchasePrice
                    }

                    val suggestedPrice = calculateSuggestedPrice(product)
                    if (suggestedPrice != null) {
                        editPrice.setText(String.format("%.2f", suggestedPrice))
                    } else if (latestPrice != null) {
                        editPrice.setText(String.format("%.2f", latestPrice.sellingPrice))
                    }

                    updatePurchasePriceDisplay(product)
                    updateMarginDisplay(product)
                }
            }
        }

        spinnerBatchDialog.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < sortedBatches.size) {
                    val batch = sortedBatches[position]
                    selectedBatchId = batch.id
                    updateBatchExpiryWarning(batch)
                    selectedProduct?.let { product ->
                        updatePriceFromBatch(product, batch)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    selectedProduct = products[position]
                    selectedLevel = 1
                    radioLevel1.isChecked = true
                    updateLevelUI(selectedProduct!!)
                    updateStockDisplay(selectedProduct!!)
                    loadBatchesAndPrice(selectedProduct!!)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        radioGroupLevel.setOnCheckedChangeListener { _, checkedId ->
            selectedLevel = if (checkedId == R.id.radioLevel2) 2 else 1
            selectedProduct?.let { product ->
                updateStockDisplay(product)
                updatePurchasePriceDisplay(product)
                val suggestedPrice = calculateSuggestedPrice(product)
                if (suggestedPrice != null) {
                    editPrice.setText(String.format("%.2f", suggestedPrice))
                }
                updateMarginDisplay(product)
            }
        }

        // Update margin display when price changes
        editPrice.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                selectedProduct?.let { updateMarginDisplay(it) }
            }
        })

        AlertDialog.Builder(this)
            .setTitle(strings.addProduct)
            .setView(dialogView)
            .setPositiveButton(strings.add) { _, _ ->
                val product = selectedProduct
                val quantity = editQuantity.text.toString().toDoubleOrNull()
                val price = editPrice.text.toString().toDoubleOrNull()

                if (product == null) {
                    Toast.makeText(this, strings.selectProduct, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (price == null || price <= 0) {
                    Toast.makeText(this, strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Level 2 validation: quantity must not exceed conversionFactor
                val conversionFactor = getConversionFactor(product)
                if (selectedLevel == 2 && conversionFactor != null && quantity > conversionFactor) {
                    val packagingType = packagingTypes[product.packagingTypeId]
                    Toast.makeText(
                        this,
                        "Quantite max: ${conversionFactor.toInt()} ${packagingType?.level2Name ?: ""}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                // Compute baseQuantity (level 1 equivalent)
                val baseQuantity: Double? = if (selectedLevel == 2 && conversionFactor != null) {
                    quantity * conversionFactor
                } else {
                    null
                }

                // Check stock availability (non-blocking: warning only)
                val availableQty = currentStock[product.id] ?: 0.0
                val alreadyInCart = saleItemAdapter.getItems()
                    .filter { it.productId == product.id }
                    .sumOf { it.baseQuantity ?: it.quantity }
                val quantityInBaseUnits = baseQuantity ?: quantity
                val totalNeeded = alreadyInCart + quantityInBaseUnits

                if (totalNeeded > availableQty) {
                    Toast.makeText(
                        this,
                        strings.insufficientStock,
                        Toast.LENGTH_LONG
                    ).show()
                    // Non-blocking: continue adding the item (warning only)
                }

                // Derive unit name from the selected level
                val packagingType = packagingTypes[product.packagingTypeId]
                val unit = packagingType?.getLevelName(selectedLevel) ?: getUnit(product)

                // Add item to the list with batchId
                val saleItem = SaleItem(
                    id = UUID.randomUUID().toString(),
                    saleId = editingSaleId ?: "",
                    productId = product.id,
                    productName = product.name,
                    unit = unit,
                    quantity = quantity,
                    baseQuantity = baseQuantity,
                    unitPrice = price,
                    totalPrice = quantity * price,
                    batchId = selectedBatchId
                )
                saleItemAdapter.addItem(saleItem)
                updateTotal()
                updateSaveButtonState()
            }
            .setNegativeButton(strings.cancel, null)
            .show()
    }

    private fun removeItem(item: SaleItem) {
        saleItemAdapter.removeItem(item)
        updateTotal()
        updateSaveButtonState()
    }

    private fun showCustomerSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_customer, null)
        val editSearch = dialogView.findViewById<EditText>(R.id.editSearchCustomer)
        val recyclerCustomers = dialogView.findViewById<RecyclerView>(R.id.recyclerCustomers)
        val btnAddNew = dialogView.findViewById<Button>(R.id.btnAddNewCustomer)

        recyclerCustomers.layoutManager = LinearLayoutManager(this)
        val customerAdapter = com.medistock.ui.adapters.CustomerAdapter { customer ->
            selectedCustomer = customer
            editCustomerName.setText(customer.name)
            // Dismiss the dialog
        }
        recyclerCustomers.adapter = customerAdapter

        val dialog = AlertDialog.Builder(this)
            .setTitle(strings.selectCustomer)
            .setView(dialogView)
            .setNegativeButton(strings.cancel, null)
            .create()

        // Load customers
        lifecycleScope.launch(Dispatchers.IO) {
            val siteId = selectedSiteId ?: return@launch
            val customers = sdk.customerRepository.getBySite(siteId)
            withContext(Dispatchers.Main) {
                customerAdapter.submitList(customers)
            }
        }

        // Search functionality
        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim()
                lifecycleScope.launch(Dispatchers.IO) {
                    val siteId = selectedSiteId ?: return@launch
                    val customers = if (query.isNotEmpty()) {
                        sdk.customerRepository.search(query).filter { it.siteId == siteId }
                    } else {
                        sdk.customerRepository.getBySite(siteId)
                    }
                    withContext(Dispatchers.Main) {
                        customerAdapter.submitList(customers)
                    }
                }
            }
        })

        // Update adapter click to dismiss dialog
        val finalAdapter = com.medistock.ui.adapters.CustomerAdapter { customer ->
            selectedCustomer = customer
            editCustomerName.setText(customer.name)
            dialog.dismiss()
        }
        recyclerCustomers.adapter = finalAdapter

        // Reload customers with new adapter
        lifecycleScope.launch(Dispatchers.IO) {
            val siteId = selectedSiteId ?: return@launch
            val customers = sdk.customerRepository.getBySite(siteId)
            withContext(Dispatchers.Main) {
                finalAdapter.submitList(customers)
            }
        }

        btnAddNew.setOnClickListener {
            dialog.dismiss()
            showAddCustomerDialog()
        }

        dialog.show()
    }

    private fun showAddCustomerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null)
        val editName = dialogView.findViewById<EditText>(R.id.editCustomerName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editCustomerPhone)
        val editAddress = dialogView.findViewById<EditText>(R.id.editCustomerAddress)
        val editNotes = dialogView.findViewById<EditText>(R.id.editCustomerNotes)

        AlertDialog.Builder(this)
            .setTitle(strings.addCustomer)
            .setView(dialogView)
            .setPositiveButton(strings.save) { _, _ ->
                val name = editName.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val address = editAddress.text.toString().trim()
                val notes = editNotes.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, strings.required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val currentUser = authManager.getUsername().ifBlank { "system" }
                    val customer = sdk.createCustomer(
                        name = name,
                        phone = phone.ifEmpty { null },
                        address = address.ifEmpty { null },
                        notes = notes.ifEmpty { null },
                        siteId = selectedSiteId,
                        userId = currentUser
                    )
                    sdk.customerRepository.insert(customer)

                    withContext(Dispatchers.Main) {
                        selectedCustomer = customer
                        editCustomerName.setText(name)
                        Toast.makeText(
                            this@SaleActivity,
                            strings.success,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(strings.cancel, null)
            .show()
    }

    private fun updateTotal() {
        val total = saleItemAdapter.getTotalAmount()
        textTotalAmount.text = "${strings.saleTotal}: ${String.format("%.2f", total)}"
    }

    private fun updateSaveButtonState() {
        btnSaveSale.isEnabled = saleItemAdapter.itemCount > 0
    }

    /**
     * Allocates sale quantity to purchase batches using FIFO (First In, First Out) based on purchase date.
     * Returns list of batch allocations and the weighted average purchase price.
     */
    private suspend fun allocateBatchesFIFO(
        productId: String,
        siteId: String,
        quantityNeeded: Double,
        currentUser: String,
        preferredBatchId: String? = null
    ): Pair<List<SaleBatchAllocation>, Double> {
        // Get available batches ordered by purchase date (FIFO)
        val allBatches = sdk.purchaseBatchRepository.getByProductAndSite(productId, siteId)
            .sortedBy { it.purchaseDate }

        // If a preferred batch is specified, put it first, then FIFO for the rest
        val batches = if (preferredBatchId != null) {
            val preferred = allBatches.filter { it.id == preferredBatchId }
            val rest = allBatches.filter { it.id != preferredBatchId }
            preferred + rest
        } else {
            allBatches
        }

        val allocations = mutableListOf<SaleBatchAllocation>()
        var remainingQty = quantityNeeded
        var totalCost = 0.0
        val now = System.currentTimeMillis()

        for (batch in batches) {
            if (remainingQty <= 0) break

            val qtyToTake = minOf(remainingQty, batch.remainingQuantity)

            // Create allocation (saleItemId will be set later)
            allocations.add(
                SaleBatchAllocation(
                    id = UUID.randomUUID().toString(),
                    saleItemId = "", // Will be updated after sale item is created
                    batchId = batch.id,
                    quantityAllocated = qtyToTake,
                    purchasePriceAtAllocation = batch.purchasePrice,
                    createdAt = now,
                    createdBy = currentUser
                )
            )

            // Track cost for weighted average
            totalCost += qtyToTake * batch.purchasePrice

            // Update batch remaining quantity
            val newRemainingQty = batch.remainingQuantity - qtyToTake
            val isExhausted = newRemainingQty <= 0
            sdk.purchaseBatchRepository.updateQuantity(batch.id, newRemainingQty, isExhausted, now, currentUser)

            remainingQty -= qtyToTake
        }

        // Insufficient stock is a warning, not a blocking error (allow negative stock)
        if (remainingQty > 0) {
            android.util.Log.w("SaleActivity", "Insufficient stock: ${remainingQty} remaining needed for product $productId")
        }

        val avgPurchasePrice = if (quantityNeeded > 0) totalCost / quantityNeeded else 0.0
        return Pair(allocations, avgPurchasePrice)
    }

    /**
     * Reverses batch allocations when editing/deleting a sale.
     */
    private suspend fun reverseBatchAllocations(saleItemId: String, currentUser: String) {
        val allocations = sdk.saleBatchAllocationRepository.getBySaleItem(saleItemId)
        val now = System.currentTimeMillis()

        for (allocation in allocations) {
            // Get current batch
            val batch = sdk.purchaseBatchRepository.getById(allocation.batchId)
            batch?.let {
                // Add back the quantity
                val newRemainingQty = it.remainingQuantity + allocation.quantityAllocated
                sdk.purchaseBatchRepository.updateQuantity(it.id, newRemainingQty, false, now, currentUser)
            }
        }

        // Delete allocations
        sdk.saleBatchAllocationRepository.deleteForSaleItem(saleItemId)
    }

    private fun saveSale() {
        val customerName = editCustomerName.text.toString().trim()

        if (customerName.isEmpty()) {
            Toast.makeText(this, strings.required, Toast.LENGTH_SHORT).show()
            return
        }

        if (saleItemAdapter.itemCount == 0) {
            Toast.makeText(this, strings.noSaleItems, Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedSiteId.isNullOrBlank()) {
            Toast.makeText(this, strings.selectSite, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val totalAmount = saleItemAdapter.getTotalAmount()
                val currentUser = authManager.getUsername().ifBlank { "system" }

                if (editingSaleId != null && existingSale != null) {
                    // Update existing sale
                    val updatedSale = existingSale!!.copy(
                        customerName = customerName,
                        customerId = selectedCustomer?.id,
                        totalAmount = totalAmount
                    )
                    sdk.saleRepository.update(updatedSale)

                    // Get old items BEFORE deleting them
                    val oldItems = sdk.saleRepository.getItemsForSale(editingSaleId!!)

                    // Reverse old batch allocations and stock movements
                    oldItems.forEach { oldItem ->
                        // Reverse batch allocations
                        reverseBatchAllocations(oldItem.id, currentUser)

                        // Reverse stock movement using base quantity (level 1 units)
                        val reverseQuantity = oldItem.baseQuantity ?: oldItem.quantity
                        val movement = StockMovement(
                            id = UUID.randomUUID().toString(),
                            productId = oldItem.productId,
                            siteId = selectedSiteId ?: "",
                            quantity = reverseQuantity,
                            type = "in",
                            date = currentTime,
                            purchasePriceAtMovement = 0.0,
                            sellingPriceAtMovement = oldItem.unitPrice,
                            createdAt = currentTime,
                            createdBy = currentUser
                        )
                        sdk.stockMovementRepository.insert(movement)

                        // Update current_stock (add back the reversed quantity)
                        sdk.stockRepository.updateStockDelta(
                            productId = oldItem.productId,
                            siteId = selectedSiteId ?: "",
                            delta = reverseQuantity,
                            lastMovementId = movement.id
                        )
                    }

                    // Delete old sale items AFTER reversing
                    sdk.saleRepository.deleteItemsForSale(editingSaleId!!)

                    // Insert new sale items with FIFO batch allocation
                    saleItemAdapter.getItems().forEach { item ->
                        // Use baseQuantity (level 1 units) for stock operations
                        val stockQuantity = item.baseQuantity ?: item.quantity

                        // Allocate batches using FIFO with base quantity
                        val (allocations, avgPurchasePrice) = allocateBatchesFIFO(
                            item.productId,
                            selectedSiteId ?: "",
                            stockQuantity,
                            currentUser,
                            preferredBatchId = item.batchId
                        )

                        // Insert sale item (preserves display quantity, baseQuantity, and batchId)
                        val newItem = SaleItem(
                            id = item.id,
                            saleId = editingSaleId!!,
                            productId = item.productId,
                            productName = item.productName,
                            unit = item.unit,
                            quantity = item.quantity,
                            baseQuantity = item.baseQuantity,
                            unitPrice = item.unitPrice,
                            totalPrice = item.totalPrice,
                            batchId = item.batchId,
                            createdAt = currentTime,
                            createdBy = currentUser
                        )
                        sdk.saleRepository.insertSaleItem(newItem)

                        // Insert batch allocations with correct saleItemId
                        allocations.forEach { allocation ->
                            sdk.saleBatchAllocationRepository.insert(
                                allocation.copy(saleItemId = newItem.id)
                            )
                        }

                        // Create stock movement with base quantity (level 1 units)
                        val movement = StockMovement(
                            id = UUID.randomUUID().toString(),
                            productId = item.productId,
                            siteId = selectedSiteId ?: "",
                            quantity = stockQuantity,
                            type = "out",
                            date = currentTime,
                            purchasePriceAtMovement = avgPurchasePrice,
                            sellingPriceAtMovement = item.unitPrice,
                            createdAt = currentTime,
                            createdBy = currentUser
                        )
                        sdk.stockMovementRepository.insert(movement)

                        // Update current_stock (deduct base quantity)
                        sdk.stockRepository.updateStockDelta(
                            productId = item.productId,
                            siteId = selectedSiteId ?: "",
                            delta = -stockQuantity,
                            lastMovementId = movement.id
                        )
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SaleActivity,
                            strings.success,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    // Create new sale
                    val sale = sdk.createSale(
                        customerName = customerName,
                        siteId = selectedSiteId ?: "",
                        totalAmount = totalAmount,
                        customerId = selectedCustomer?.id,
                        userId = currentUser
                    )
                    sdk.saleRepository.insert(sale)
                    val saleId = sale.id

                    // Insert sale items with FIFO batch allocation
                    saleItemAdapter.getItems().forEach { item ->
                        // Use baseQuantity (level 1 units) for stock operations
                        val stockQuantity = item.baseQuantity ?: item.quantity

                        // Allocate batches using FIFO with base quantity
                        val (allocations, avgPurchasePrice) = allocateBatchesFIFO(
                            item.productId,
                            selectedSiteId ?: "",
                            stockQuantity,
                            currentUser,
                            preferredBatchId = item.batchId
                        )

                        // Get the product to derive the unit
                        val product = products.find { it.id == item.productId }

                        // Insert sale item (preserves display quantity and baseQuantity)
                        val newItem = sdk.createSaleItem(
                            saleId = saleId,
                            productId = item.productId,
                            productName = item.productName,
                            unit = item.unit,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            userId = currentUser
                        )
                        // Copy baseQuantity and batchId to the created item
                        val newItemWithBase = newItem.copy(
                            baseQuantity = item.baseQuantity,
                            batchId = item.batchId
                        )
                        sdk.saleRepository.insertSaleItem(newItemWithBase)

                        // Insert batch allocations with correct saleItemId
                        allocations.forEach { allocation ->
                            sdk.saleBatchAllocationRepository.insert(
                                allocation.copy(saleItemId = newItemWithBase.id)
                            )
                        }

                        // Create stock movement with base quantity (level 1 units)
                        val movement = StockMovement(
                            id = UUID.randomUUID().toString(),
                            productId = item.productId,
                            siteId = selectedSiteId ?: "",
                            quantity = stockQuantity,
                            type = "out",
                            date = currentTime,
                            purchasePriceAtMovement = avgPurchasePrice,
                            sellingPriceAtMovement = item.unitPrice,
                            createdAt = currentTime,
                            createdBy = currentUser
                        )
                        sdk.stockMovementRepository.insert(movement)

                        // Update current_stock (deduct base quantity)
                        sdk.stockRepository.updateStockDelta(
                            productId = item.productId,
                            siteId = selectedSiteId ?: "",
                            delta = -stockQuantity,
                            lastMovementId = movement.id
                        )
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SaleActivity,
                            strings.saleCompleted,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SaleActivity,
                        "${strings.error}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
