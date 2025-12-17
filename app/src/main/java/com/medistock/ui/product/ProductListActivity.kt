package com.medistock.ui.product

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Product
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listView = findViewById(R.id.listProducts)
        db = AppDatabase.getInstance(applicationContext)

        val siteId = PrefsHelper.getActiveSiteId(this)

        lifecycleScope.launch {
            val products = getProductsForSite(siteId)
            val items = products.map { product ->
                // Affiche le nom du produit et l’unité
                "${product.name} (${product.unit})"
            }
            withContext(Dispatchers.Main) {
                listView.adapter = ArrayAdapter(
                    this@ProductListActivity,
                    android.R.layout.simple_list_item_1,
                    items
                )
            }
        }
    }

    private suspend fun getProductsForSite(siteId: Long): List<Product> {
        return db.productDao().getProductsForSite(siteId).first()
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