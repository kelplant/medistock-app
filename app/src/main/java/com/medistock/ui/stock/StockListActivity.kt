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
import com.medistock.data.entities.Product
import com.medistock.data.entities.Site
import com.medistock.ui.adapters.StockAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockListActivity : AppCompatActivity() {

    private lateinit var spinnerSites: Spinner
    private lateinit var spinnerProducts: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var db: AppDatabase
    private lateinit var summaryText: TextView

    private var sites: List<Site> = emptyList()
    private var products: List<Product> = emptyList()
    private var currentStockItems: List<CurrentStock> = emptyList()
    private var selectedSitePosition: Int = 0
    private var selectedProductPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerSites = findViewById(R.id.spinnerSites)
        spinnerProducts = findViewById(R.id.spinnerProducts)
        recyclerView = findViewById(R.id.recyclerViewStock)
        summaryText = findViewById(R.id.textStockSummary)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList())
        recyclerView.adapter = adapter

        db = AppDatabase.getInstance(this)

        loadSites()
        loadProducts()
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = db.siteDao().getAll().first()
            withContext(Dispatchers.Main) {
                // Add "All Sites" option
                val siteNames = mutableListOf("All sites")
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
                        selectedSitePosition = position
                        loadStockWithFilters()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch(Dispatchers.IO) {
            products = db.productDao().getAll().first()
            withContext(Dispatchers.Main) {
                // Add "All Products" option
                val productNames = mutableListOf("All products")
                productNames.addAll(products.map { it.name })

                val spinnerAdapter = ArrayAdapter(
                    this@StockListActivity,
                    android.R.layout.simple_spinner_item,
                    productNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerProducts.adapter = spinnerAdapter

                spinnerProducts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, view: View?, position: Int, id: Long
                    ) {
                        selectedProductPosition = position
                        loadStockWithFilters()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    private fun loadStockWithFilters() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentStockItems = when {
                // All sites, all products
                selectedSitePosition == 0 && selectedProductPosition == 0 -> {
                    db.stockMovementDao().getCurrentStockAllSites().first()
                }
                // Specific site, all products
                selectedSitePosition > 0 && selectedProductPosition == 0 -> {
                    val selectedSite = sites[selectedSitePosition - 1]
                    db.stockMovementDao().getCurrentStockForSite(selectedSite.id).first()
                }
                // All sites, specific product
                selectedSitePosition == 0 && selectedProductPosition > 0 -> {
                    val selectedProduct = products[selectedProductPosition - 1]
                    db.stockMovementDao().getCurrentStockForProduct(selectedProduct.id).first()
                }
                // Specific site, specific product
                else -> {
                    val selectedSite = sites[selectedSitePosition - 1]
                    val selectedProduct = products[selectedProductPosition - 1]
                    db.stockMovementDao().getCurrentStockForProductAndSite(selectedProduct.id, selectedSite.id).first()
                }
            }
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

        summaryText.text = "Products: $totalProducts | In stock: $itemsInStock | Out of stock: $outOfStock"
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