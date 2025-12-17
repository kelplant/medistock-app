package com.medistock.ui.purchase

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PurchaseActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerSite: Spinner
    private lateinit var spinnerProduct: Spinner
    private lateinit var editQuantity: EditText
    private lateinit var editPurchasePrice: EditText
    private lateinit var editSellingPrice: EditText
    private lateinit var editSupplier: EditText
    private lateinit var editBatchNumber: EditText
    private lateinit var editExpiryDate: EditText
    private lateinit var btnSave: Button
    private lateinit var textMarginInfo: TextView

    private var sites: List<com.medistock.data.entities.Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var selectedProductId: Long = 0L
    private var selectedProduct: Product? = null
    private var selectedSiteId: Long = 0L
    private var isManualSellingPrice = false // Track if user manually modified selling price

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        selectedSiteId = PrefsHelper.getActiveSiteId(this)

        spinnerSite = findViewById(R.id.spinnerSitePurchase)
        spinnerProduct = findViewById(R.id.spinnerProductPurchase)
        editQuantity = findViewById(R.id.editPurchaseQuantity)
        editPurchasePrice = findViewById(R.id.editPurchasePrice)
        editSellingPrice = findViewById(R.id.editSellingPrice)
        editSupplier = findViewById(R.id.editSupplierName)
        editBatchNumber = findViewById(R.id.editBatchNumber)
        editExpiryDate = findViewById(R.id.editExpiryDate)
        btnSave = findViewById(R.id.btnSavePurchase)
        textMarginInfo = findViewById(R.id.textMarginInfo)

        loadSites()
        loadProducts()

        spinnerSite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < sites.size) {
                    selectedSiteId = sites[position].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    selectedProduct = products[position]
                    selectedProductId = selectedProduct?.id ?: 0L
                    updateMarginInfo()
                    calculateSellingPrice()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Auto-calculate selling price when purchase price changes (if not manually modified)
        editPurchasePrice.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isManualSellingPrice) {
                    calculateSellingPrice()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        // Track manual modifications to selling price
        editSellingPrice.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                // Only set flag if user actually typed (not programmatic change)
                if (editSellingPrice.hasFocus()) {
                    isManualSellingPrice = true
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        btnSave.setOnClickListener { savePurchase() }
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll().first()
            withContext(Dispatchers.Main) {
                val siteNames = sites.map { it.name }
                val adapter = ArrayAdapter(
                    this@PurchaseActivity,
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
            withContext(Dispatchers.Main) {
                val productNames = products.map { "${it.name} (${it.unit})" }
                val adapter = ArrayAdapter(
                    this@PurchaseActivity,
                    android.R.layout.simple_spinner_item,
                    productNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerProduct.adapter = adapter
            }
        }
    }

    private fun updateMarginInfo() {
        val product = selectedProduct ?: return
        val marginInfo = when (product.marginType) {
            "fixed" -> "Margin: +${product.marginValue ?: 0.0} (fixed)"
            "percentage" -> "Margin: +${product.marginValue ?: 0.0}%"
            else -> "No margin configured"
        }
        textMarginInfo.text = marginInfo
    }

    private fun calculateSellingPrice() {
        val product = selectedProduct ?: return
        val purchasePrice = editPurchasePrice.text.toString().toDoubleOrNull() ?: 0.0

        val calculatedSellingPrice = when (product.marginType) {
            "fixed" -> purchasePrice + (product.marginValue ?: 0.0)
            "percentage" -> purchasePrice * (1 + (product.marginValue ?: 0.0) / 100)
            else -> purchasePrice
        }

        // Only update if not manually modified
        if (!isManualSellingPrice) {
            editSellingPrice.setText(String.format("%.2f", calculatedSellingPrice))
        }
    }

    private fun savePurchase() {
        val quantity = editQuantity.text.toString().toDoubleOrNull()
        val purchasePrice = editPurchasePrice.text.toString().toDoubleOrNull()
        val sellingPrice = editSellingPrice.text.toString().toDoubleOrNull()
        val supplier = editSupplier.text.toString().trim()
        val batchNumber = editBatchNumber.text.toString().trim()
        val expiryDateStr = editExpiryDate.text.toString().trim()

        if (quantity == null || quantity <= 0) {
            Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (purchasePrice == null || purchasePrice <= 0) {
            Toast.makeText(this, "Enter a valid purchase price", Toast.LENGTH_SHORT).show()
            return
        }

        if (sellingPrice == null || sellingPrice <= 0) {
            Toast.makeText(this, "Enter a valid selling price", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProductId == 0L) {
            Toast.makeText(this, "Select a product", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse expiry date if provided
        val expiryDate: Long? = if (expiryDateStr.isNotEmpty()) {
            try {
                dateFormat.parse(expiryDateStr)?.time
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid date format (dd/MM/yyyy)", Toast.LENGTH_SHORT).show()
                return
            }
        } else null

        lifecycleScope.launch(Dispatchers.IO) {
            val product = selectedProduct ?: return@launch

            // Create purchase batch
            val batch = PurchaseBatch(
                productId = selectedProductId,
                siteId = selectedSiteId,
                batchNumber = batchNumber.ifEmpty { "BATCH-${System.currentTimeMillis()}" },
                purchaseDate = System.currentTimeMillis(),
                initialQuantity = quantity,
                remainingQuantity = quantity,
                purchasePrice = purchasePrice,
                supplierName = supplier,
                expiryDate = expiryDate,
                isExhausted = false
            )
            db.purchaseBatchDao().insert(batch)

            // Create stock movement (entry)
            val movement = StockMovement(
                productId = selectedProductId,
                type = "in",
                quantity = quantity,
                date = System.currentTimeMillis(),
                purchasePriceAtMovement = purchasePrice,
                sellingPriceAtMovement = sellingPrice,
                siteId = selectedSiteId
            )
            db.stockMovementDao().insert(movement)

            // Update product price record
            val priceRecord = ProductPrice(
                productId = selectedProductId,
                effectiveDate = System.currentTimeMillis(),
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                source = "purchase"
            )
            db.productPriceDao().insert(priceRecord)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@PurchaseActivity,
                    "Purchase recorded: $quantity ${product.unit}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
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
