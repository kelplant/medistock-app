package com.medistock.ui.sales

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
import com.medistock.ui.adapters.SaleItemAdapter
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaleActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerSite: Spinner
    private lateinit var recyclerSaleItems: RecyclerView
    private lateinit var btnAddProduct: Button
    private lateinit var editCustomerName: EditText
    private lateinit var textTotalAmount: TextView
    private lateinit var btnSaveSale: Button

    private lateinit var saleItemAdapter: SaleItemAdapter
    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var currentStock: Map<Long, Double> = emptyMap()
    private var selectedSiteId: Long = 0L
    private var editingSaleId: Long? = null
    private var existingSale: Sale? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        selectedSiteId = PrefsHelper.getActiveSiteId(this)
        editingSaleId = intent.getLongExtra("SALE_ID", -1L).takeIf { it != -1L }

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

        btnAddProduct.setOnClickListener {
            showAddProductDialog()
        }

        btnSaveSale.setOnClickListener {
            saveSale()
        }

        // If editing, load the existing sale
        editingSaleId?.let { saleId ->
            supportActionBar?.title = "Edit Sale"
            loadExistingSale(saleId)
        } ?: run {
            supportActionBar?.title = "New Sale"
        }
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll().first()
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
            products = db.productDao().getAll().first()
        }
    }

    private fun loadStockForSite() {
        lifecycleScope.launch(Dispatchers.IO) {
            val stockItems = db.stockMovementDao().getCurrentStockForSite(selectedSiteId).first()
            currentStock = stockItems.associate { it.productId to it.quantityOnHand }
        }
    }

    private fun loadExistingSale(saleId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val saleWithItems = db.saleDao().getSaleWithItems(saleId).first()
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
            Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sale_item, null)
        val spinnerProduct = dialogView.findViewById<Spinner>(R.id.spinnerProductDialog)
        val textAvailableStock = dialogView.findViewById<TextView>(R.id.textAvailableStock)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editQuantityDialog)
        val editPrice = dialogView.findViewById<EditText>(R.id.editPriceDialog)

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
                    textAvailableStock.text = "Available stock: $availableQty ${selectedProduct!!.unit}"

                    // Load suggested price
                    lifecycleScope.launch(Dispatchers.IO) {
                        val latestPrice = db.productPriceDao().getLatestPrice(selectedProduct!!.id).first()
                        withContext(Dispatchers.Main) {
                            if (latestPrice != null) {
                                editPrice.setText(String.format("%.2f", latestPrice.sellingPrice))
                            }
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Add Product")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val product = selectedProduct
                val quantity = editQuantity.text.toString().toDoubleOrNull()
                val price = editPrice.text.toString().toDoubleOrNull()

                if (product == null) {
                    Toast.makeText(this, "Select a product", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (price == null || price <= 0) {
                    Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check stock availability
                val availableQty = currentStock[product.id] ?: 0.0
                val alreadyInCart = saleItemAdapter.getTotalQuantityForProduct(product.id)
                val totalNeeded = alreadyInCart + quantity

                if (totalNeeded > availableQty) {
                    Toast.makeText(
                        this,
                        "Insufficient stock. Available: $availableQty, Already in cart: $alreadyInCart",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                // Add item to the list
                val saleItem = SaleItem(
                    saleId = editingSaleId ?: 0L,
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    quantity = quantity,
                    pricePerUnit = price,
                    subtotal = quantity * price
                )
                saleItemAdapter.addItem(saleItem)
                updateTotal()
                updateSaveButtonState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeItem(item: SaleItem) {
        saleItemAdapter.removeItem(item)
        updateTotal()
        updateSaveButtonState()
    }

    private fun updateTotal() {
        val total = saleItemAdapter.getTotalAmount()
        textTotalAmount.text = "Total: ${String.format("%.2f", total)}"
    }

    private fun updateSaveButtonState() {
        btnSaveSale.isEnabled = saleItemAdapter.itemCount > 0
    }

    private fun saveSale() {
        val customerName = editCustomerName.text.toString().trim()

        if (customerName.isEmpty()) {
            Toast.makeText(this, "Enter customer name", Toast.LENGTH_SHORT).show()
            return
        }

        if (saleItemAdapter.itemCount == 0) {
            Toast.makeText(this, "Add at least one product", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val totalAmount = saleItemAdapter.getTotalAmount()

                if (editingSaleId != null && existingSale != null) {
                    // Update existing sale
                    val updatedSale = existingSale!!.copy(
                        customerName = customerName,
                        totalAmount = totalAmount,
                        siteId = selectedSiteId
                    )
                    db.saleDao().update(updatedSale)

                    // Delete old sale items
                    db.saleItemDao().deleteAllForSale(editingSaleId!!)

                    // Reverse old stock movements by adding back
                    val oldItems = db.saleItemDao().getItemsForSale(editingSaleId!!).first()
                    oldItems.forEach { oldItem ->
                        val movement = StockMovement(
                            productId = oldItem.productId,
                            type = "in",
                            quantity = oldItem.quantity,
                            date = currentTime,
                            siteId = selectedSiteId,
                            purchasePriceAtMovement = 0.0,
                            sellingPriceAtMovement = oldItem.pricePerUnit
                        )
                        db.stockMovementDao().insert(movement)
                    }

                    // Insert new sale items and stock movements
                    saleItemAdapter.getItems().forEach { item ->
                        val newItem = item.copy(saleId = editingSaleId!!)
                        db.saleItemDao().insert(newItem)

                        val movement = StockMovement(
                            productId = item.productId,
                            type = "out",
                            quantity = item.quantity,
                            date = currentTime,
                            siteId = selectedSiteId,
                            purchasePriceAtMovement = 0.0,
                            sellingPriceAtMovement = item.pricePerUnit
                        )
                        db.stockMovementDao().insert(movement)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SaleActivity,
                            "Sale updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    // Create new sale
                    val sale = Sale(
                        customerName = customerName,
                        date = currentTime,
                        totalAmount = totalAmount,
                        siteId = selectedSiteId,
                        createdBy = "" // Can be filled with current user if needed
                    )
                    val saleId = db.saleDao().insert(sale)

                    // Insert sale items and create stock movements
                    saleItemAdapter.getItems().forEach { item ->
                        val newItem = item.copy(saleId = saleId)
                        db.saleItemDao().insert(newItem)

                        val movement = StockMovement(
                            productId = item.productId,
                            type = "out",
                            quantity = item.quantity,
                            date = currentTime,
                            siteId = selectedSiteId,
                            purchasePriceAtMovement = 0.0,
                            sellingPriceAtMovement = item.pricePerUnit
                        )
                        db.stockMovementDao().insert(movement)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SaleActivity,
                            "Sale completed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SaleActivity,
                        "Error saving sale: ${e.message}",
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
