package com.medistock.ui.movement

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StockMovementActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_movement)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        val productSpinner = findViewById<Spinner>(R.id.spinnerProduct)
        val typeSpinner = findViewById<Spinner>(R.id.spinnerType)
        val quantityInput = findViewById<EditText>(R.id.editQuantity)
        val saveButton = findViewById<Button>(R.id.btnSaveMovement)

        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("in", "out"))

        lifecycleScope.launch {
            products = db.productDao().getAll().first()
            productSpinner.adapter = ArrayAdapter(
                this@StockMovementActivity,
                android.R.layout.simple_spinner_item,
                products.map { it.name + " (" + (it.unit ?: "Units") + ")" }
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
                    val latestPrice = db.productPriceDao().getLatestPrice(product.id).first()
                    if (latestPrice != null) {
                        val siteId = com.medistock.util.PrefsHelper.getActiveSiteId(this@StockMovementActivity)
                    db.stockMovementDao().insert(
                            StockMovement(
                                productId = product.id,
                                type = type,
                                quantity = quantityInBaseUnit,
                                date = System.currentTimeMillis(),
                                purchasePriceAtMovement = latestPrice.purchasePrice,
                                sellingPriceAtMovement = latestPrice.sellingPrice,
                            siteId = siteId
                            )
                        )
                        finish()
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