package com.medistock.ui.movement

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Product
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StockMovementListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_movement_list)

        val listView = findViewById<ListView>(R.id.listStockMovements)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "medistock-db").build()
        val siteId = PrefsHelper.getActiveSiteId(this)

        lifecycleScope.launch {
            val products = db.productDao().getProductsForSite(siteId).first().associateBy { it.id }
            val movements = db.stockMovementDao().getAllForSite(siteId).first()
            val items = movements.map {
                val productName = products[it.productId]?.name ?: "Unknown"
                "Product: $productName, Type: ${it.type}, Qty: ${it.quantity}, Date: ${java.util.Date(it.date)}"
            }
            listView.adapter = ArrayAdapter(this@StockMovementListActivity, android.R.layout.simple_list_item_1, items)
        }
    }
}