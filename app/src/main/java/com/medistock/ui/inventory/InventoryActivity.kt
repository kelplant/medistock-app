package com.medistock.ui.inventory

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

class InventoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerProduct: Spinner
    private lateinit var textTheoreticalStock: TextView
    private lateinit var editCountedQuantity: EditText
    private lateinit var textDiscrepancy: TextView
    private lateinit var editCountedBy: EditText
    private lateinit var editReason: EditText
    private lateinit var editNotes: EditText
    private lateinit var btnSaveInventory: Button

    private var products: List<Product> = emptyList()
    private var currentStockItems: List<CurrentStock> = emptyList()
    private var selectedProductId: Long = 0L
    private var theoreticalQuantity: Double = 0.0
    private var currentSiteId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        currentSiteId = PrefsHelper.getActiveSiteId(this)

        spinnerProduct = findViewById(R.id.spinnerProductInventory)
        textTheoreticalStock = findViewById(R.id.textTheoreticalStock)
        editCountedQuantity = findViewById(R.id.editCountedQuantity)
        textDiscrepancy = findViewById(R.id.textDiscrepancy)
        editCountedBy = findViewById(R.id.editCountedBy)
        editReason = findViewById(R.id.editReason)
        editNotes = findViewById(R.id.editNotes)
        btnSaveInventory = findViewById(R.id.btnSaveInventory)

        loadProducts()

        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < products.size) {
                    selectedProductId = products[position].id
                    updateTheoreticalStock()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editCountedQuantity.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = calculateDiscrepancy()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        btnSaveInventory.setOnClickListener { saveInventory() }
    }

    private fun loadProducts() {
        lifecycleScope.launch(Dispatchers.IO) {
            products = db.productDao().getAll().first()
            currentStockItems = db.stockMovementDao().getCurrentStockForSite(currentSiteId).first()

            withContext(Dispatchers.Main) {
                val productNames = products.map { "${it.name} (${it.unit ?: "Units"})" }
                val adapter = ArrayAdapter(
                    this@InventoryActivity,
                    android.R.layout.simple_spinner_item,
                    productNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerProduct.adapter = adapter
            }
        }
    }

    private fun updateTheoreticalStock() {
        val stockItem = currentStockItems.find { it.productId == selectedProductId }
        theoreticalQuantity = stockItem?.quantityOnHand ?: 0.0

        val product = products.find { it.id == selectedProductId }
        textTheoreticalStock.text = "Theoretical Stock: $theoreticalQuantity ${product?.unit ?: ""}"
        calculateDiscrepancy()
    }

    private fun calculateDiscrepancy() {
        val countedQty = editCountedQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val discrepancy = countedQty - theoreticalQuantity

        val color = when {
            discrepancy == 0.0 -> android.graphics.Color.GREEN
            discrepancy > 0 -> android.graphics.Color.rgb(0, 128, 0) // Surplus
            else -> android.graphics.Color.RED // Shortage
        }

        val sign = if (discrepancy > 0) "+" else ""
        textDiscrepancy.text = "Discrepancy: $sign$discrepancy"
        textDiscrepancy.setTextColor(color)
    }

    private fun saveInventory() {
        val countedQty = editCountedQuantity.text.toString().toDoubleOrNull()
        val countedBy = editCountedBy.text.toString().trim()
        val reason = editReason.text.toString().trim()
        val notes = editNotes.text.toString().trim()

        if (countedQty == null) {
            Toast.makeText(this, "Enter the counted quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProductId == 0L) {
            Toast.makeText(this, "Select a product", Toast.LENGTH_SHORT).show()
            return
        }

        val discrepancy = countedQty - theoreticalQuantity

        lifecycleScope.launch(Dispatchers.IO) {
            val inventory = Inventory(
                productId = selectedProductId,
                siteId = currentSiteId,
                countDate = System.currentTimeMillis(),
                countedQuantity = countedQty,
                theoreticalQuantity = theoreticalQuantity,
                discrepancy = discrepancy,
                reason = reason,
                countedBy = countedBy,
                notes = notes
            )
            db.inventoryDao().insert(inventory)

            // If there's a discrepancy, create a stock adjustment movement
            if (discrepancy != 0.0) {
                val product = products.find { it.id == selectedProductId }
                val latestPrice = db.productPriceDao().getLatestPrice(selectedProductId).first()

                val movement = StockMovement(
                    productId = selectedProductId,
                    type = if (discrepancy > 0) "in" else "out",
                    quantity = Math.abs(discrepancy),
                    date = System.currentTimeMillis(),
                    purchasePriceAtMovement = latestPrice?.purchasePrice ?: 0.0,
                    sellingPriceAtMovement = latestPrice?.sellingPrice ?: 0.0,
                    siteId = currentSiteId
                )
                db.stockMovementDao().insert(movement)
            }

            withContext(Dispatchers.Main) {
                val message = if (discrepancy == 0.0) {
                    "Inventory saved - No discrepancy"
                } else {
                    "Inventory saved - Discrepancy: $discrepancy adjusted"
                }
                Toast.makeText(this@InventoryActivity, message, Toast.LENGTH_SHORT).show()
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
