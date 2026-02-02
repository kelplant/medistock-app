package com.medistock.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var spinnerFromSite: Spinner
    private lateinit var spinnerToSite: Spinner
    private lateinit var recyclerTransferItems: RecyclerView
    private lateinit var btnAddProduct: Button
    private lateinit var btnSaveTransfer: Button

    private lateinit var transferItemAdapter: TransferItemAdapter
    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var packagingTypes: Map<String, PackagingType> = emptyMap()
    private var currentStock: Map<String, Double> = emptyMap() // productId -> quantity available
    private var selectedFromSiteId: String? = null
    private var selectedToSiteId: String? = null
    private var transferId: String? = null // null for new transfer, value for editing

    private fun getUnit(product: Product?): String {
        if (product == null) return ""
        val packagingType = packagingTypes[product.packagingTypeId]
        return packagingType?.getLevelName(product.selectedLevel) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)
        selectedFromSiteId = PrefsHelper.getActiveSiteId(this)

        // Check if editing existing transfer
        transferId = intent.getStringExtra("TRANSFER_ID")?.takeIf { it.isNotBlank() }
        supportActionBar?.title = if (transferId != null) "${L.strings.edit} ${L.strings.transfer}" else L.strings.newTransfer

        spinnerFromSite = findViewById(R.id.spinnerFromSite)
        spinnerToSite = findViewById(R.id.spinnerToSite)
        recyclerTransferItems = findViewById(R.id.recyclerTransferItems)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        btnSaveTransfer = findViewById(R.id.btnSaveTransfer)

        // Setup RecyclerView
        transferItemAdapter = TransferItemAdapter(mutableListOf()) { item ->
            removeItem(item)
        }
        recyclerTransferItems.layoutManager = LinearLayoutManager(this)
        recyclerTransferItems.adapter = transferItemAdapter

        loadSites()
        loadProducts()

        spinnerFromSite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < sites.size) {
                    val newSiteId = sites[position].id
                    // Clear cart when source site changes (batch selections become stale)
                    if (newSiteId != selectedFromSiteId && transferItemAdapter.itemCount > 0) {
                        transferItemAdapter.clear()
                        updateSaveButtonState()
                    }
                    selectedFromSiteId = newSiteId
                    loadStockForSite()
                    updateToSiteSpinner()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerToSite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val filteredSites = sites.filter { it.id != selectedFromSiteId }
                if (position >= 0 && position < filteredSites.size) {
                    selectedToSiteId = filteredSites[position].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnAddProduct.setOnClickListener {
            if (selectedFromSiteId == selectedToSiteId) {
                Toast.makeText(this, "${L.strings.fromSite} ${L.strings.toSite}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddProductDialog()
        }

        btnSaveTransfer.setOnClickListener {
            saveTransfer()
        }

        if (transferId != null) {
            loadTransferForEdit()
        }
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = sdk.siteRepository.getAll()
            withContext(Dispatchers.Main) {
                val siteNames = sites.map { it.name }
                val adapter = ArrayAdapter(
                    this@TransferActivity,
                    android.R.layout.simple_spinner_item,
                    siteNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFromSite.adapter = adapter

                // Select the current active site
                val currentSiteIndex = sites.indexOfFirst { it.id == selectedFromSiteId }
                if (currentSiteIndex >= 0) {
                    spinnerFromSite.setSelection(currentSiteIndex)
                }

                updateToSiteSpinner()
            }
        }
    }

    private fun updateToSiteSpinner() {
        val filteredSites = sites.filter { it.id != selectedFromSiteId }
        val siteNames = filteredSites.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            siteNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerToSite.adapter = adapter

        if (filteredSites.isNotEmpty()) {
            selectedToSiteId = filteredSites[0].id
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
            val siteId = selectedFromSiteId ?: return@launch
            val stockItems = sdk.stockRepository.getCurrentStockForSite(siteId)
            currentStock = stockItems.associate { it.productId to it.quantityOnHand }
        }
    }

    private fun loadTransferForEdit() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transferIdValue = transferId ?: return@launch
            val transfer = sdk.productTransferRepository.getById(transferIdValue)
            if (transfer != null) {
                withContext(Dispatchers.Main) {
                    // Set sites
                    selectedFromSiteId = transfer.fromSiteId
                    selectedToSiteId = transfer.toSiteId

                    val fromSiteIndex = sites.indexOfFirst { it.id == selectedFromSiteId }
                    if (fromSiteIndex >= 0) {
                        spinnerFromSite.setSelection(fromSiteIndex)
                    }

                    // Load product info
                    val product = sdk.productRepository.getById(transfer.productId)
                    if (product != null) {
                        val item = TransferItem(
                            productId = product.id,
                            productName = product.name,
                            unit = getUnit(product),
                            quantity = transfer.quantity
                        )
                        transferItemAdapter.addItem(item)
                        updateSaveButtonState()
                    }
                }
            }
        }
    }

    private fun showAddProductDialog() {
        if (products.isEmpty()) {
            Toast.makeText(this, L.strings.noProducts, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transfer_item, null)
        val spinnerProduct = dialogView.findViewById<Spinner>(R.id.spinnerProductDialog)
        val textAvailableStock = dialogView.findViewById<TextView>(R.id.textAvailableStock)
        val labelBatchSelection = dialogView.findViewById<TextView>(R.id.labelBatchSelection)
        val spinnerBatchDialog = dialogView.findViewById<Spinner>(R.id.spinnerBatchDialog)
        val textBatchExpiryWarning = dialogView.findViewById<TextView>(R.id.textBatchExpiryWarning)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editQuantityDialog)

        val productNames = products.map { "${it.name} (${getUnit(it)})" }
        val productAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = productAdapter

        var selectedProduct: Product? = null
        var sortedBatches: List<PurchaseBatch> = emptyList()
        var selectedBatchId: String? = null

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

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

        fun loadBatchesForProduct(product: Product) {
            lifecycleScope.launch(Dispatchers.IO) {
                val batches = sdk.purchaseBatchRepository.getByProductAndSite(
                    product.id, selectedFromSiteId ?: ""
                ).filter { !it.isExhausted && it.remainingQuantity > 0 }

                // Smart batch suggestion: expiring soon first (by expiryDate ASC), then FIFO (purchaseDate ASC)
                val threshold = System.currentTimeMillis() + thirtyDaysMs
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
                            this@TransferActivity,
                            android.R.layout.simple_spinner_item,
                            batchLabels
                        )
                        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerBatchDialog.adapter = batchAdapter

                        // Pre-select first batch (the suggested one)
                        spinnerBatchDialog.setSelection(0)
                        val firstBatch = sorted.first()
                        selectedBatchId = firstBatch.id
                        updateBatchExpiryWarning(firstBatch)
                    } else {
                        labelBatchSelection.visibility = View.GONE
                        spinnerBatchDialog.visibility = View.GONE
                        textBatchExpiryWarning.visibility = View.GONE
                        selectedBatchId = null
                    }
                }
            }
        }

        spinnerBatchDialog.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < sortedBatches.size) {
                    val batch = sortedBatches[position]
                    selectedBatchId = batch.id
                    updateBatchExpiryWarning(batch)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    selectedProduct = products[position]
                    val availableQty = currentStock[selectedProduct!!.id] ?: 0.0
                    textAvailableStock.text = "Available stock at source: $availableQty ${getUnit(selectedProduct)}"
                    loadBatchesForProduct(selectedProduct!!)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        AlertDialog.Builder(this)
            .setTitle(L.strings.addProduct)
            .setView(dialogView)
            .setPositiveButton(L.strings.add) { _, _ ->
                val product = selectedProduct
                val quantity = editQuantity.text.toString().toDoubleOrNull()

                if (product == null) {
                    Toast.makeText(this, L.strings.selectProduct, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, L.strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check stock availability (non-blocking: warning only)
                val availableQty = currentStock[product.id] ?: 0.0
                val alreadyInCart = transferItemAdapter.getTotalQuantityForProduct(product.id)
                val totalNeeded = alreadyInCart + quantity

                if (totalNeeded > availableQty) {
                    Toast.makeText(
                        this,
                        L.strings.insufficientStock,
                        Toast.LENGTH_LONG
                    ).show()
                    // Non-blocking: continue adding the item (warning only)
                }

                // Add item to the list with batchId
                val transferItem = TransferItem(
                    productId = product.id,
                    productName = product.name,
                    unit = getUnit(product),
                    quantity = quantity,
                    batchId = selectedBatchId
                )
                transferItemAdapter.addItem(transferItem)
                updateSaveButtonState()
            }
            .setNegativeButton(L.strings.cancel, null)
            .show()
    }

    private fun removeItem(item: TransferItem) {
        transferItemAdapter.removeItem(item)
        updateSaveButtonState()
    }

    private fun updateSaveButtonState() {
        btnSaveTransfer.isEnabled = transferItemAdapter.itemCount > 0
    }

    private fun saveTransfer() {
        if (selectedFromSiteId == null || selectedToSiteId == null || selectedFromSiteId == selectedToSiteId) {
            Toast.makeText(this, L.strings.selectSite, Toast.LENGTH_SHORT).show()
            return
        }

        if (transferItemAdapter.itemCount == 0) {
            Toast.makeText(this, L.strings.noProducts, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = authManager.getUsername()

                // Process each transfer item via shared TransferUseCase
                // (handles FIFO batch transfer, stock movements, updateStockDelta, audit)
                for (item in transferItemAdapter.getItems()) {
                    val input = TransferInput(
                        productId = item.productId,
                        fromSiteId = selectedFromSiteId!!,
                        toSiteId = selectedToSiteId!!,
                        quantity = item.quantity,
                        notes = null,
                        userId = currentUser,
                        preferredBatchId = item.batchId
                    )

                    when (val result = sdk.transferUseCase.execute(input)) {
                        is UseCaseResult.Success -> {
                            for (warning in result.warnings) {
                                if (warning is BusinessWarning.InsufficientStock) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@TransferActivity,
                                            "${L.strings.insufficientStock}: ${item.productName}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                        is UseCaseResult.Error -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@TransferActivity,
                                    "${L.strings.error}: ${result.error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@launch
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferActivity,
                        L.strings.transferCompleted,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferActivity,
                        "${L.strings.error}: ${e.message}",
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
