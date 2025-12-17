package com.medistock.ui.sales

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

class SaleActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var products: List<Product> = emptyList()
    private lateinit var productSpinner: Spinner
    private lateinit var quantityInput: EditText
    private lateinit var farmerInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var saveButton: Button
    private lateinit var textPriceInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        productSpinner = findViewById(R.id.spinnerProduct)
        quantityInput = findViewById(R.id.editSaleQuantity)
        farmerInput = findViewById(R.id.editFarmerName)
        priceInput = findViewById(R.id.editSalePrice)
        saveButton = findViewById(R.id.btnSaveSale)
        textPriceInfo = findViewById(R.id.textSalePriceInfo)

        lifecycleScope.launch {
            products = db.productDao().getAll().first()
            productSpinner.adapter = ArrayAdapter(
                this@SaleActivity,
                android.R.layout.simple_spinner_item,
                products.map { it.name }
            )
        }

        // Auto-fill selling price when product is selected
        productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    loadSuggestedPrice(products[position].id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        saveButton.setOnClickListener {
            val index = productSpinner.selectedItemPosition
            val quantity = quantityInput.text.toString().toDoubleOrNull()
            val price = priceInput.text.toString().toDoubleOrNull()
            val farmer = farmerInput.text.toString().trim()

            if (index >= 0 && quantity != null && price != null && farmer.isNotEmpty()) {
                val productId = products[index].id
                lifecycleScope.launch {
                    val siteId = com.medistock.util.PrefsHelper.getActiveSiteId(this@SaleActivity)
                    db.productSaleDao().insert(
                        ProductSale(
                            productId = productId,
                            quantity = quantity,
                            priceAtSale = price,
                            farmerName = farmer,
                            date = System.currentTimeMillis(),
                            siteId = siteId
                        )
                    )
                    db.stockMovementDao().insert(
                        StockMovement(
                            productId = productId,
                            type = "out",
                            quantity = quantity,
                            date = System.currentTimeMillis(),
                            siteId = siteId,
                            purchasePriceAtMovement = price, // could be improved
                            sellingPriceAtMovement = price
                        )
                    )
                    finish()
                }
            }
        }
    }

    private fun loadSuggestedPrice(productId: Long) {
        lifecycleScope.launch {
            val latestPrice = db.productPriceDao().getLatestPrice(productId).first()
            if (latestPrice != null) {
                priceInput.setText(String.format("%.2f", latestPrice.sellingPrice))
                textPriceInfo.text = "Prix suggéré (dernier prix de vente enregistré)"
            } else {
                textPriceInfo.text = "Aucun prix de référence - Entrez le prix de vente"
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