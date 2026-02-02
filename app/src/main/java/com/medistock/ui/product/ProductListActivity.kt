package com.medistock.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.ui.adapters.ProductAdapter
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductListActivity : AppCompatActivity() {
    private lateinit var adapter: ProductAdapter
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.products
        sdk = MedistockApplication.sdk

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProducts)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddProduct)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter { product ->
            val intent = Intent(this, ProductAddEditActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, ProductAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun loadProducts() {
        val siteId = PrefsHelper.getActiveSiteId(this)
        if (siteId.isNullOrBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            val products = sdk.productRepository.getWithCategoryForSite(siteId)
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
