package com.medistock.ui.movement

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockMovementListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_movement_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.stockMovements

        val listView = findViewById<ListView>(R.id.listStockMovements)
        val sdk = MedistockApplication.sdk
        val siteId = PrefsHelper.getActiveSiteId(this)

        lifecycleScope.launch {
            if (siteId.isNullOrBlank()) return@launch
            val (products, movements) = withContext(Dispatchers.IO) {
                val prods = sdk.productRepository.getBySite(siteId).associateBy { it.id }
                val movs = sdk.stockMovementRepository.getBySite(siteId)
                Pair(prods, movs)
            }
            val items = movements.map {
                val productName = products[it.productId]?.name ?: L.strings.noData
                "${L.strings.product}: $productName, ${L.strings.movementType}: ${it.type}, ${L.strings.quantity}: ${it.quantity}, ${L.strings.movementDate}: ${java.util.Date(it.date)}"
            }
            listView.adapter = ArrayAdapter(this@StockMovementListActivity, android.R.layout.simple_list_item_1, items)
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
