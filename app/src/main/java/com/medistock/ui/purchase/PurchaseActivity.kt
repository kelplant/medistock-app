package com.medistock.ui.purchase

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.ui.LocalizedActivity
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.ProductPrice
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.model.StockMovement
import com.medistock.shared.domain.model.Supplier
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PurchaseActivity : LocalizedActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var spinnerSite: Spinner
    private lateinit var spinnerProduct: Spinner
    private lateinit var spinnerSupplier: Spinner
    private lateinit var editQuantity: EditText
    private lateinit var editPurchasePrice: EditText
    private lateinit var editSellingPrice: EditText
    private lateinit var editSupplier: EditText
    private lateinit var editBatchNumber: EditText
    private lateinit var editExpiryDate: EditText
    private lateinit var btnSave: Button
    private lateinit var textMarginInfo: TextView
    private lateinit var labelSupplierOrEnter: TextView

    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var suppliers: List<Supplier> = emptyList()
    private var packagingTypes: Map<String, PackagingType> = emptyMap()
    private var selectedProductId: String? = null
    private var selectedProduct: Product? = null
    private var selectedSiteId: String? = null
    private var selectedSupplierId: String? = null
    private var isManualSellingPrice = false // Track if user manually modified selling price

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private fun getUnit(product: Product?): String {
        if (product == null) return ""
        val packagingType = packagingTypes[product.packagingTypeId]
        // Purchases always use level 1 (base unit) since stock is tracked in base units
        return packagingType?.level1Name ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)
        selectedSiteId = PrefsHelper.getActiveSiteId(this)

        spinnerSite = findViewById(R.id.spinnerSitePurchase)
        spinnerProduct = findViewById(R.id.spinnerProductPurchase)
        spinnerSupplier = findViewById(R.id.spinnerSupplier)
        editQuantity = findViewById(R.id.editPurchaseQuantity)
        editPurchasePrice = findViewById(R.id.editPurchasePrice)
        editSellingPrice = findViewById(R.id.editSellingPrice)
        editSupplier = findViewById(R.id.editSupplierName)
        editBatchNumber = findViewById(R.id.editBatchNumber)
        editExpiryDate = findViewById(R.id.editExpiryDate)
        btnSave = findViewById(R.id.btnSavePurchase)
        textMarginInfo = findViewById(R.id.textMarginInfo)
        labelSupplierOrEnter = findViewById(R.id.labelSupplierOrEnter)

        loadSites()
        loadProducts()
        loadSuppliers()

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
                    selectedProductId = selectedProduct?.id
                    updateMarginInfo()
                    calculateSellingPrice()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerSupplier.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                // First item is "-- Select --" (no supplier selected)
                if (position > 0 && position <= suppliers.size) {
                    val supplier = suppliers[position - 1]
                    selectedSupplierId = supplier.id
                    editSupplier.setText(supplier.name)
                } else {
                    selectedSupplierId = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSupplierId = null
            }
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

    override fun applyLocalizedStrings() {
        supportActionBar?.title = strings.newPurchase

        // Labels
        findViewById<TextView>(R.id.labelRecordPurchase)?.text = strings.newPurchase
        findViewById<TextView>(R.id.labelSitePurchase)?.text = strings.site
        findViewById<TextView>(R.id.labelProductPurchase)?.text = strings.product
        findViewById<TextView>(R.id.labelQuantityPurchase)?.text = strings.quantity
        findViewById<TextView>(R.id.labelPurchasePrice)?.text = strings.unitPurchasePrice
        textMarginInfo.text = strings.marginCalculatedAuto
        findViewById<TextView>(R.id.labelSellingPrice)?.text = strings.unitSellingPrice
        findViewById<TextView>(R.id.textSellingPriceNote)?.text = strings.sellingPriceNote
        findViewById<TextView>(R.id.labelSupplier)?.text = strings.supplier
        findViewById<TextView>(R.id.labelBatchNumber)?.text = strings.batchNumber
        findViewById<TextView>(R.id.labelExpiryDate)?.text = strings.expiryDateOptional

        // Hints/Placeholders
        editSupplier.hint = strings.enterSupplierName
        editBatchNumber.hint = strings.batchNumberExample
        editExpiryDate.hint = strings.dateFormat
        labelSupplierOrEnter.text = strings.orSelect

        // Button
        btnSave.text = strings.savePurchase
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = sdk.siteRepository.getAll()
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
            products = sdk.productRepository.getAll()
            packagingTypes = sdk.packagingTypeRepository.getAll().associateBy { it.id }
            withContext(Dispatchers.Main) {
                val productNames = products.map { "${it.name} (${getUnit(it)})" }
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

    private fun loadSuppliers() {
        lifecycleScope.launch(Dispatchers.IO) {
            suppliers = sdk.supplierRepository.getActive()
            withContext(Dispatchers.Main) {
                // Add "-- Select --" as first option
                val supplierNames = listOf("-- ${strings.selectSupplier} --") + suppliers.map { it.name }
                val adapter = ArrayAdapter(
                    this@PurchaseActivity,
                    android.R.layout.simple_spinner_item,
                    supplierNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSupplier.adapter = adapter
            }
        }
    }

    private fun updateMarginInfo() {
        val product = selectedProduct ?: return
        val marginInfo = when (product.marginType) {
            "fixed" -> "${strings.margin}: +${product.marginValue ?: 0.0} (${strings.margin})"
            "percentage" -> "${strings.margin}: +${product.marginValue ?: 0.0}%"
            else -> strings.marginCalculatedAuto
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
            Toast.makeText(this, strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
            return
        }

        if (purchasePrice == null || purchasePrice <= 0) {
            Toast.makeText(this, strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
            return
        }

        if (sellingPrice == null || sellingPrice <= 0) {
            Toast.makeText(this, strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProductId == null) {
            Toast.makeText(this, strings.selectProduct, Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedSiteId.isNullOrBlank()) {
            Toast.makeText(this, strings.selectSite, Toast.LENGTH_SHORT).show()
            return
        }

        // Parse expiry date if provided
        val expiryDate: Long? = if (expiryDateStr.isNotEmpty()) {
            try {
                dateFormat.parse(expiryDateStr)?.time
            } catch (e: Exception) {
                Toast.makeText(this, "${strings.error}: ${strings.dateFormat}", Toast.LENGTH_SHORT).show()
                return
            }
        } else null

        lifecycleScope.launch(Dispatchers.IO) {
            val product = selectedProduct ?: return@launch
            val currentUser = authManager.getUsername().ifBlank { "system" }
            val now = System.currentTimeMillis()

            // Create purchase batch
            val batch = PurchaseBatch(
                id = UUID.randomUUID().toString(),
                productId = selectedProductId!!,
                siteId = selectedSiteId!!,
                batchNumber = batchNumber.ifEmpty { null }, // Optional, null if not provided
                purchaseDate = now,
                initialQuantity = quantity,
                remainingQuantity = quantity,
                purchasePrice = purchasePrice,
                supplierName = supplier,
                supplierId = selectedSupplierId,
                expiryDate = expiryDate,
                isExhausted = false,
                createdAt = now,
                updatedAt = now,
                createdBy = currentUser,
                updatedBy = currentUser
            )
            sdk.purchaseBatchRepository.insert(batch)

            // Create stock movement (entry)
            val movement = StockMovement(
                id = UUID.randomUUID().toString(),
                productId = selectedProductId!!,
                siteId = selectedSiteId!!,
                quantity = quantity,
                type = "in",
                date = now,
                purchasePriceAtMovement = purchasePrice,
                sellingPriceAtMovement = sellingPrice,
                createdAt = now,
                createdBy = currentUser
            )
            sdk.stockMovementRepository.insert(movement)

            // Update product price record
            val priceRecord = ProductPrice(
                id = UUID.randomUUID().toString(),
                productId = selectedProductId!!,
                effectiveDate = now,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                source = "purchase",
                createdAt = now,
                updatedAt = now,
                createdBy = currentUser,
                updatedBy = currentUser
            )
            sdk.productPriceRepository.insert(priceRecord)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@PurchaseActivity,
                    strings.purchaseRecorded,
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
