package com.medistock.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.ProductTransfer
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.model.StockMovement
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.util.BatchTransferHelper
import com.medistock.shared.i18n.L
import com.medistock.util.InsufficientStockException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    private var currentStock: Map<String, Double> = emptyMap() // productId -> quantity available
    private var selectedFromSiteId: String? = null
    private var selectedToSiteId: String? = null
    private var transferId: String? = null // null for new transfer, value for editing

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
                    selectedFromSiteId = sites[position].id
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
                            unit = product.unit,
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
        val editQuantity = dialogView.findViewById<EditText>(R.id.editQuantityDialog)

        val productNames = products.map { "${it.name} (${it.unit})" }
        val productAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = productAdapter

        var selectedProduct: Product? = null

        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    selectedProduct = products[position]
                    val availableQty = currentStock[selectedProduct!!.id] ?: 0.0
                    textAvailableStock.text = "Available stock at source: $availableQty ${selectedProduct!!.unit}"
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

                // Check stock availability
                val availableQty = currentStock[product.id] ?: 0.0
                val alreadyInCart = transferItemAdapter.getTotalQuantityForProduct(product.id)
                val totalNeeded = alreadyInCart + quantity

                if (totalNeeded > availableQty) {
                    Toast.makeText(
                        this,
                        L.strings.insufficientStock,
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                // Add item to the list
                val transferItem = TransferItem(
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    quantity = quantity
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
                val currentTime = System.currentTimeMillis()
                val currentUser = authManager.getUsername()
                val batchHelper = BatchTransferHelper(sdk.purchaseBatchRepository)

                // Process each transfer item with FIFO batch management
                transferItemAdapter.getItems().forEach { item ->
                    // Transfer batches using FIFO
                    val batchTransfers = try {
                        batchHelper.transferBatchesFIFO(
                            productId = item.productId,
                            fromSiteId = selectedFromSiteId!!,
                            toSiteId = selectedToSiteId!!,
                            totalQuantity = item.quantity,
                            currentUser = currentUser
                        )
                    } catch (e: InsufficientStockException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@TransferActivity,
                                "${L.strings.insufficientStock}: ${item.productName}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // Calculate weighted average purchase price
                    val avgPurchasePrice = batchHelper.calculateAveragePurchasePrice(batchTransfers)

                    // Get current selling price
                    val latestPrice = sdk.productPriceRepository.getLatestPrice(item.productId)
                    val sellingPrice = latestPrice?.sellingPrice ?: 0.0

                    // Create product transfer record
                    val productTransfer = ProductTransfer(
                        id = transferId ?: UUID.randomUUID().toString(),
                        productId = item.productId,
                        quantity = item.quantity,
                        fromSiteId = selectedFromSiteId!!,
                        toSiteId = selectedToSiteId!!,
                        status = "completed",
                        notes = "Transferred ${batchTransfers.size} batch(es)",
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        createdBy = currentUser,
                        updatedBy = currentUser
                    )

                    sdk.productTransferRepository.insert(productTransfer)

                    // Create stock movement for source site (transfer out)
                    val movementOut = StockMovement(
                        id = UUID.randomUUID().toString(),
                        productId = item.productId,
                        siteId = selectedFromSiteId!!,
                        quantity = item.quantity,
                        type = "out",
                        date = currentTime,
                        purchasePriceAtMovement = avgPurchasePrice,
                        sellingPriceAtMovement = sellingPrice,
                        createdAt = currentTime,
                        createdBy = currentUser
                    )
                    sdk.stockMovementRepository.insert(movementOut)

                    // Create stock movement for destination site (transfer in)
                    val movementIn = StockMovement(
                        id = UUID.randomUUID().toString(),
                        productId = item.productId,
                        siteId = selectedToSiteId!!,
                        quantity = item.quantity,
                        type = "in",
                        date = currentTime,
                        purchasePriceAtMovement = avgPurchasePrice,
                        sellingPriceAtMovement = sellingPrice,
                        createdAt = currentTime,
                        createdBy = currentUser
                    )
                    sdk.stockMovementRepository.insert(movementIn)
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
