package com.medistock.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.ui.adapters.ProductWithCategoryAdapter
import com.medistock.ui.product.ProductAddActivity
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductWithCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val siteId = PrefsHelper.getActiveSiteId(this@MainActivity)
            if (!siteId.isNullOrBlank()) {
                sdk.productRepository.observeWithCategoryForSite(siteId).collect { products ->
                    adapter = ProductWithCategoryAdapter(products)
                    recyclerView.adapter = adapter
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_add_product -> {
                startActivity(Intent(this, ProductAddActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
