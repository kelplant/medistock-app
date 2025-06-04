package com.medistock.ui.stock

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Product
import com.medistock.data.entities.Site
import com.medistock.ui.adapters.StockAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockListActivity : AppCompatActivity() {

    private lateinit var spinnerSites: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var db: AppDatabase

    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_list)

        spinnerSites = findViewById(R.id.spinnerSites)
        recyclerView = findViewById(R.id.recyclerViewStock)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList())
        recyclerView.adapter = adapter

        db = AppDatabase.getInstance(this)

        loadSites()
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll()
            withContext(Dispatchers.Main) {
                val siteNames = sites.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    this@StockListActivity,
                    android.R.layout.simple_spinner_item,
                    siteNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSites.adapter = spinnerAdapter

                spinnerSites.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, view: View?, position: Int, id: Long
                    ) {
                        val selectedSite = sites[position]
                        loadProductsForSite(selectedSite.id)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    private fun loadProductsForSite(siteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            products = db.productDao().getProductsForSite(siteId)
            withContext(Dispatchers.Main) {
                adapter.updateData(products)
            }
        }
    }
}