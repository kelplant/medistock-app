package com.medistock.ui.purchase

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
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.ui.adapters.PurchaseBatchAdapter
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchaseListActivity : AppCompatActivity() {
    private lateinit var adapter: PurchaseBatchAdapter
    private lateinit var sdk: MedistockSDK
    private var siteId: String? = null
    private var allBatches = listOf<PurchaseBatch>()
    private var productNames = mutableMapOf<String, String>()

    private enum class Filter { ALL, ACTIVE, EXHAUSTED }
    private var currentFilter = Filter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Purchase History"
        sdk = MedistockApplication.sdk
        siteId = PrefsHelper.getActiveSiteId(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerPurchases)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddPurchase)
        val chipAll = findViewById<Chip>(R.id.chipAll)
        val chipActive = findViewById<Chip>(R.id.chipActive)
        val chipExhausted = findViewById<Chip>(R.id.chipExhausted)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PurchaseBatchAdapter(productNames) { batch ->
            // Click on a batch - can open details or edit in the future
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, PurchaseActivity::class.java))
        }

        chipAll.setOnClickListener {
            currentFilter = Filter.ALL
            chipAll.isChecked = true
            chipActive.isChecked = false
            chipExhausted.isChecked = false
            applyFilter()
        }

        chipActive.setOnClickListener {
            currentFilter = Filter.ACTIVE
            chipAll.isChecked = false
            chipActive.isChecked = true
            chipExhausted.isChecked = false
            applyFilter()
        }

        chipExhausted.setOnClickListener {
            currentFilter = Filter.EXHAUSTED
            chipAll.isChecked = false
            chipActive.isChecked = false
            chipExhausted.isChecked = true
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

            // Load batches
            allBatches = sdk.purchaseBatchRepository.getAll()
                .sortedByDescending { it.purchaseDate }

            runOnUiThread {
                // Recreate adapter with updated product names
                val recyclerView = findViewById<RecyclerView>(R.id.recyclerPurchases)
                adapter = PurchaseBatchAdapter(productNames) { batch ->
                    // Click handler
                }
                recyclerView.adapter = adapter
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> allBatches
            Filter.ACTIVE -> allBatches.filter { !it.isExhausted && it.remainingQuantity > 0 }
            Filter.EXHAUSTED -> allBatches.filter { it.isExhausted || it.remainingQuantity <= 0 }
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
