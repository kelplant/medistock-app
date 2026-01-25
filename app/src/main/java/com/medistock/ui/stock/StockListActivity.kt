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
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.CurrentStock
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.Site
import com.medistock.ui.adapters.StockAdapter
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockListActivity : AppCompatActivity() {

    private lateinit var spinnerSites: Spinner
    private lateinit var spinnerProducts: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var sdk: MedistockSDK
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

        sdk = MedistockApplication.sdk

        loadSites()
        loadProducts()
    }

    private fun loadSites() {
        lifecycleScope.launch(Dispatchers.IO) {
            sites = sdk.siteRepository.getAll()
            withContext(Dispatchers.Main) {
                // Add "All Sites" option
                val siteNames = mutableListOf(L.strings.allSites)
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
            products = sdk.productRepository.getAll()
            withContext(Dispatchers.Main) {
                // Add "All Products" option
                val productNames = mutableListOf(L.strings.products)
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
            val rawStockItems = when {
                // All sites, all products
                selectedSitePosition == 0 && selectedProductPosition == 0 -> {
                    sdk.stockRepository.getAllCurrentStock()
                }
                // Specific site, all products
                selectedSitePosition > 0 && selectedProductPosition == 0 -> {
                    val selectedSite = sites[selectedSitePosition - 1]
                    sdk.stockRepository.getCurrentStockForSite(selectedSite.id)
                }
                // All sites, specific product
                selectedSitePosition == 0 && selectedProductPosition > 0 -> {
                    val selectedProduct = products[selectedProductPosition - 1]
                    sdk.stockRepository.getAllCurrentStock().filter { it.productId == selectedProduct.id }
                }
                // Specific site, specific product
                else -> {
                    val selectedSite = sites[selectedSitePosition - 1]
                    val selectedProduct = products[selectedProductPosition - 1]
                    val stock = sdk.stockRepository.getCurrentStockByProductAndSite(selectedProduct.id, selectedSite.id)
                    if (stock != null) listOf(stock) else emptyList()
                }
            }
            // Filter out items with zero stock - only show products actually present
            currentStockItems = rawStockItems.filter { it.quantityOnHand > 0 }
            withContext(Dispatchers.Main) {
                adapter.updateData(currentStockItems)
                updateSummary()
            }
        }
    }

    private fun updateSummary() {
        val totalItems = currentStockItems.size
        val lowStockCount = currentStockItems.count { it.quantityOnHand <= it.minStock && it.minStock > 0 }
        val totalQuantity = currentStockItems.sumOf { it.quantityOnHand }

        summaryText.text = "${L.strings.products}: $totalItems | ${L.strings.lowStock}: $lowStockCount | Total: ${totalQuantity.toInt()}"
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
