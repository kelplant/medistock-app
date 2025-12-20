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
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.util.BatchTransferHelper
import com.medistock.util.InsufficientStockException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransferActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private lateinit var spinnerFromSite: Spinner
    private lateinit var spinnerToSite: Spinner
    private lateinit var recyclerTransferItems: RecyclerView
    private lateinit var btnAddProduct: Button
    private lateinit var btnSaveTransfer: Button

    private lateinit var transferItemAdapter: TransferItemAdapter
    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var currentStock: Map<Long, Double> = emptyMap() // productId -> quantity available
    private var selectedFromSiteId: Long = 0L
    private var selectedToSiteId: Long = 0L
    private var transferId: Long = 0L // 0 for new transfer, > 0 for editing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        authManager = AuthManager.getInstance(this)
        selectedFromSiteId = PrefsHelper.getActiveSiteId(this)

        // Check if editing existing transfer
        transferId = intent.getLongExtra("TRANSFER_ID", 0L)
        supportActionBar?.title = if (transferId > 0) "Edit Transfer" else "New Transfer"

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
                Toast.makeText(this, "Source and destination sites must be different", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddProductDialog()
        }

        btnSaveTransfer.setOnClickListener {
            saveTransfer()
        }

        if (transferId > 0) {
            loadTransferForEdit()
        }
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll().first()
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
            products = db.productDao().getAll().first()
        }
    }

    private fun loadStockForSite() {
        lifecycleScope.launch(Dispatchers.IO) {
            val stockItems = db.stockMovementDao().getCurrentStockForSite(selectedFromSiteId).first()
            currentStock = stockItems.associate { it.productId to it.quantityOnHand }
        }
    }

    private fun loadTransferForEdit() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transfer = db.productTransferDao().getById(transferId).first()
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
                    val product = db.productDao().getById(transfer.productId).first()
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
            Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show()
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
            .setTitle("Add Product to Transfer")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val product = selectedProduct
                val quantity = editQuantity.text.toString().toDoubleOrNull()

                if (product == null) {
                    Toast.makeText(this, "Select a product", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check stock availability
                val availableQty = currentStock[product.id] ?: 0.0
                val alreadyInCart = transferItemAdapter.getTotalQuantityForProduct(product.id)
                val totalNeeded = alreadyInCart + quantity

                if (totalNeeded > availableQty) {
                    Toast.makeText(
                        this,
                        "Insufficient stock. Available: $availableQty, Already added: $alreadyInCart",
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
            .setNegativeButton("Cancel", null)
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
        if (selectedFromSiteId == selectedToSiteId) {
            Toast.makeText(this, "Source and destination sites must be different", Toast.LENGTH_SHORT).show()
            return
        }

        if (transferItemAdapter.itemCount == 0) {
            Toast.makeText(this, "Add at least one product", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val currentUser = authManager.getUsername()
                val batchHelper = BatchTransferHelper(db.purchaseBatchDao())

                // Process each transfer item with FIFO batch management
                transferItemAdapter.getItems().forEach { item ->
                    // Transfer batches using FIFO
                    val batchTransfers = try {
                        batchHelper.transferBatchesFIFO(
                            productId = item.productId,
                            fromSiteId = selectedFromSiteId,
                            toSiteId = selectedToSiteId,
                            totalQuantity = item.quantity,
                            currentUser = currentUser
                        )
                    } catch (e: InsufficientStockException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@TransferActivity,
                                "Insufficient stock for ${item.productName}: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // Calculate weighted average purchase price
                    val avgPurchasePrice = batchHelper.calculateAveragePurchasePrice(batchTransfers)

                    // Get current selling price
                    val latestPrice = db.productPriceDao().getLatestPrice(item.productId).first()
                    val sellingPrice = latestPrice?.sellingPrice ?: 0.0

                    // Create product transfer record
                    val productTransfer = ProductTransfer(
                        id = if (transferId > 0) transferId else 0,
                        productId = item.productId,
                        quantity = item.quantity,
                        fromSiteId = selectedFromSiteId,
                        toSiteId = selectedToSiteId,
                        date = currentTime,
                        notes = "Transferred ${batchTransfers.size} batch(es)",
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        createdBy = currentUser,
                        updatedBy = currentUser
                    )

                    db.productTransferDao().insert(productTransfer)

                    // Create stock movement for source site (transfer out)
                    val movementOut = StockMovement(
                        productId = item.productId,
                        type = "out",
                        quantity = item.quantity,
                        date = currentTime,
                        siteId = selectedFromSiteId,
                        purchasePriceAtMovement = avgPurchasePrice,
                        sellingPriceAtMovement = sellingPrice,
                        createdAt = currentTime,
                        createdBy = currentUser
                    )
                    db.stockMovementDao().insert(movementOut)

                    // Create stock movement for destination site (transfer in)
                    val movementIn = StockMovement(
                        productId = item.productId,
                        type = "in",
                        quantity = item.quantity,
                        date = currentTime,
                        siteId = selectedToSiteId,
                        purchasePriceAtMovement = avgPurchasePrice,
                        sellingPriceAtMovement = sellingPrice,
                        createdAt = currentTime,
                        createdBy = currentUser
                    )
                    db.stockMovementDao().insert(movementIn)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferActivity,
                        "Transfer completed successfully with FIFO batch management",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferActivity,
                        "Error saving transfer: ${e.message}",
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
