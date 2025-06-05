package com.medistock.ui.sales

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import kotlinx.coroutines.launch

class SaleActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)

        db = AppDatabase.getInstance(this)

        val productSpinner = findViewById<Spinner>(R.id.spinnerProduct)
        val quantityInput = findViewById<EditText>(R.id.editSaleQuantity)
        val farmerInput = findViewById<EditText>(R.id.editFarmerName)
        val priceInput = findViewById<EditText>(R.id.editSalePrice)
        val saveButton = findViewById<Button>(R.id.btnSaveSale)

        lifecycleScope.launch {
            products = db.productDao().getAll()
            productSpinner.adapter = ArrayAdapter(
                this@SaleActivity,
                android.R.layout.simple_spinner_item,
                products.map { it.name }
            )
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
}