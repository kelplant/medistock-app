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
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.ProductWithCategory
import com.medistock.ui.adapters.ProductAdapter
import com.medistock.ui.product.ProductAddActivity
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "medistock-db").build()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val siteId = PrefsHelper.getActiveSiteId(this@MainActivity)
            db.productDao().getProductsWithCategoryForSite(siteId).collect { products ->
                adapter = ProductAdapter(products)
                recyclerView.adapter = adapter
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
            R.id.menu_add_product -> {
                startActivity(Intent(this, ProductAddActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}