package com.medistock.ui.sales

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.launch

class SaleListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_list)

        val listView = findViewById<ListView>(R.id.listSales)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "medistock-db").build()
        val siteId = PrefsHelper.getActiveSiteId(this)

        lifecycleScope.launch {
            val products = db.productDao().getProductsForSite(siteId).associateBy { it.id }
            val sales = db.productSaleDao().getAllForSite(siteId)
            val items = sales.map {
                val productName = products[it.productId]?.name ?: "Unknown"
                "Product: $productName, Qty: ${it.quantity}, Farmer: ${it.farmerName}, Date: ${java.util.Date(it.date)}"
            }
            listView.adapter = ArrayAdapter(this@SaleListActivity, android.R.layout.simple_list_item_1, items)
        }
    }
}