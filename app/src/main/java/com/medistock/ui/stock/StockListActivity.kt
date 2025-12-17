package com.medistock.ui.stock

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.CurrentStock
import com.medistock.data.entities.Site
import com.medistock.ui.adapters.StockAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockListActivity : AppCompatActivity() {

    private lateinit var spinnerSites: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var db: AppDatabase
    private lateinit var summaryText: TextView

    private var sites: List<Site> = emptyList()
    private var currentStockItems: List<CurrentStock> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerSites = findViewById(R.id.spinnerSites)
        recyclerView = findViewById(R.id.recyclerViewStock)
        summaryText = findViewById(R.id.textStockSummary)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList())
        recyclerView.adapter = adapter

        db = AppDatabase.getInstance(this)

        loadSites()
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll().first()
            withContext(Dispatchers.Main) {
                // Add "All Sites" option
                val siteNames = mutableListOf("Tous les sites")
                siteNames.addAll(sites.map { it.name })

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
                        if (position == 0) {
                            // Load all sites
                            loadStockAllSites()
                        } else {
                            // Load specific site
                            val selectedSite = sites[position - 1]
                            loadStockForSite(selectedSite.id)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    private fun loadStockForSite(siteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            currentStockItems = db.stockMovementDao().getCurrentStockForSite(siteId).first()
            withContext(Dispatchers.Main) {
                adapter.updateData(currentStockItems)
                updateSummary()
            }
        }
    }

    private fun loadStockAllSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentStockItems = db.stockMovementDao().getCurrentStockAllSites().first()
            withContext(Dispatchers.Main) {
                adapter.updateData(currentStockItems)
                updateSummary()
            }
        }
    }

    private fun updateSummary() {
        val totalProducts = currentStockItems.distinctBy { it.productId }.size
        val itemsInStock = currentStockItems.count { it.quantityOnHand > 0 }
        val outOfStock = currentStockItems.count { it.quantityOnHand <= 0 }

        summaryText.text = "Produits: $totalProducts | En stock: $itemsInStock | Rupture: $outOfStock"
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