package com.medistock.ui.inventory

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.InventoryItem
import com.medistock.ui.adapters.InventoryAdapter
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class InventoryListActivity : AppCompatActivity() {
    private lateinit var adapter: InventoryAdapter
    private lateinit var sdk: MedistockSDK
    private var siteId: String? = null
    private var allInventories = listOf<InventoryItem>()
    private var productNames = mutableMapOf<String, String>()

    private enum class Filter { ALL, WITH_DISCREPANCY, NO_DISCREPANCY }
    private var currentFilter = Filter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.inventory
        sdk = MedistockApplication.sdk
        siteId = PrefsHelper.getActiveSiteId(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerInventories)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddInventory)
        val chipAll = findViewById<Chip>(R.id.chipAllInventory)
        val chipWithDiscrepancy = findViewById<Chip>(R.id.chipWithDiscrepancy)
        val chipNoDiscrepancy = findViewById<Chip>(R.id.chipNoDiscrepancy)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = InventoryAdapter(productNames) { inventory ->
            // Click on an inventory - could open details in the future
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        chipAll.setOnClickListener {
            currentFilter = Filter.ALL
            chipAll.isChecked = true
            chipWithDiscrepancy.isChecked = false
            chipNoDiscrepancy.isChecked = false
            applyFilter()
        }

        chipWithDiscrepancy.setOnClickListener {
            currentFilter = Filter.WITH_DISCREPANCY
            chipAll.isChecked = false
            chipWithDiscrepancy.isChecked = true
            chipNoDiscrepancy.isChecked = false
            applyFilter()
        }

        chipNoDiscrepancy.setOnClickListener {
            currentFilter = Filter.NO_DISCREPANCY
            chipAll.isChecked = false
            chipWithDiscrepancy.isChecked = false
            chipNoDiscrepancy.isChecked = true
            applyFilter()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Load product names
            val products = sdk.productRepository.getAll()
            productNames.clear()
            products.forEach { productNames[it.id] = it.name }

            // Load inventories
            allInventories = if (siteId != null) {
                sdk.inventoryItemRepository.getBySite(siteId!!)
            } else {
                sdk.inventoryItemRepository.getRecent(100)
            }

            runOnUiThread {
                // Recreate adapter with updated product names
                val recyclerView = findViewById<RecyclerView>(R.id.recyclerInventories)
                adapter = InventoryAdapter(productNames) { inventory ->
                    // Click handler
                }
                recyclerView.adapter = adapter
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> allInventories
            Filter.WITH_DISCREPANCY -> allInventories.filter { abs(it.discrepancy) > 0 }
            Filter.NO_DISCREPANCY -> allInventories.filter { abs(it.discrepancy) == 0.0 }
        }
        adapter.submitList(filtered)
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
