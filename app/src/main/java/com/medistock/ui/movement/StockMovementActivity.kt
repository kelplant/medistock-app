package com.medistock.ui.movement

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.StockMovement
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class StockMovementActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var products: List<Product> = emptyList()
    private var packagingTypes: Map<String, PackagingType> = emptyMap()

    private fun getUnit(product: Product?): String {
        if (product == null) return ""
        val packagingType = packagingTypes[product.packagingTypeId]
        return packagingType?.getLevelName(product.selectedLevel) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_movement)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.stockMovements

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        val productSpinner = findViewById<Spinner>(R.id.spinnerProduct)
        val typeSpinner = findViewById<Spinner>(R.id.spinnerType)
        val quantityInput = findViewById<EditText>(R.id.editQuantity)
        val saveButton = findViewById<Button>(R.id.btnSaveMovement)

        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf(L.strings.stockIn, L.strings.stockOut))

        lifecycleScope.launch {
            products = withContext(Dispatchers.IO) {
                sdk.productRepository.getAll()
            }
            packagingTypes = withContext(Dispatchers.IO) {
                sdk.packagingTypeRepository.getAll().associateBy { it.id }
            }
            productSpinner.adapter = ArrayAdapter(
                this@StockMovementActivity,
                android.R.layout.simple_spinner_item,
                products.map { it.name + " (" + getUnit(it) + ")" }
            )
        }

        saveButton.setOnClickListener {
            val selectedProductIndex = productSpinner.selectedItemPosition
            val type = typeSpinner.selectedItem.toString()
            val quantity = quantityInput.text.toString().toDoubleOrNull()

            if (selectedProductIndex >= 0 && quantity != null) {
                val product = products[selectedProductIndex]
                val quantityInBaseUnit = quantity * (product.unitVolume ?: 1.0)
                lifecycleScope.launch {
                    val siteId = PrefsHelper.getActiveSiteId(this@StockMovementActivity)
                    if (!siteId.isNullOrBlank()) {
                        val currentUser = authManager.getUsername().ifBlank { "system" }
                        val now = System.currentTimeMillis()
                        val movement = StockMovement(
                            id = UUID.randomUUID().toString(),
                            productId = product.id,
                            siteId = siteId,
                            quantity = quantityInBaseUnit,
                            type = type,
                            date = now,
                            purchasePriceAtMovement = 0.0,
                            sellingPriceAtMovement = 0.0,
                            movementType = type,
                            referenceId = null,
                            notes = "Manual stock movement",
                            createdAt = now,
                            createdBy = currentUser
                        )
                        withContext(Dispatchers.IO) {
                            sdk.stockMovementRepository.insert(movement)
                        }
                        finish()
                    } else {
                        Toast.makeText(
                            this@StockMovementActivity,
                            L.strings.selectSite,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
