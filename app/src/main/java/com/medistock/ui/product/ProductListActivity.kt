package com.medistock.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.ui.adapters.ProductAdapter
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProductListActivity : AppCompatActivity() {
    private lateinit var adapter: ProductAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Product Management"
        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProducts)
        val btnAdd = findViewById<Button>(R.id.btnAddProduct)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter { product ->
            val intent = Intent(this, ProductAddEditActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, ProductAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun loadProducts() {
        val siteId = PrefsHelper.getActiveSiteId(this)
        CoroutineScope(Dispatchers.IO).launch {
            val products = db.productDao().getProductsForSite(siteId).first()
            runOnUiThread { adapter.submitList(products) }
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