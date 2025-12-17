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
    private lateinit var editFarmerName: EditText
    private lateinit var textTotalAmount: TextView
    private lateinit var btnSaveSale: Button

    private lateinit var saleItemAdapter: SaleItemAdapter
    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var currentStock: Map<Long, Double> = emptyMap() // productId -> quantity available
    private var selectedSiteId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        selectedSiteId = PrefsHelper.getActiveSiteId(this)

        spinnerSite = findViewById(R.id.spinnerSiteSale)
        recyclerSaleItems = findViewById(R.id.recyclerSaleItems)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        editFarmerName = findViewById(R.id.editFarmerName)
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

                // Select the current active site
                val currentSiteIndex = sites.indexOfFirst { it.id == selectedSiteId }
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
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    quantity = quantity,
                    pricePerUnit = price
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
        val farmerName = editFarmerName.text.toString().trim()

        if (farmerName.isEmpty()) {
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

                // Save each sale item and create stock movements
                saleItemAdapter.items.forEach { item ->
                    // Create product sale record
                    val productSale = ProductSale(
                        productId = item.productId,
                        quantity = item.quantity,
                        priceAtSale = item.pricePerUnit,
                        farmerName = farmerName,
                        date = currentTime,
                        siteId = selectedSiteId
                    )
                    db.productSaleDao().insert(productSale)

                    // Create stock movement
                    val movement = StockMovement(
                        productId = item.productId,
                        type = "out",
                        quantity = item.quantity,
                        date = currentTime,
                        siteId = selectedSiteId,
                        purchasePriceAtMovement = 0.0, // Could be improved to track actual purchase price
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

// Extension function to get total quantity for a specific product
fun SaleItemAdapter.getTotalQuantityForProduct(productId: Long): Double {
    return this.items.filter { it.productId == productId }.sumOf { it.quantity }
}
