package com.medistock.ui.sales

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.SaleWithItems
import com.medistock.ui.adapters.SaleAdapter
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaleListActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var recyclerSales: RecyclerView
    private lateinit var fabNewSale: FloatingActionButton
    private lateinit var saleAdapter: SaleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales"

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)
        val siteId = PrefsHelper.getActiveSiteId(this)

        recyclerSales = findViewById(R.id.recyclerSales)
        fabNewSale = findViewById(R.id.fabNewSale)

        recyclerSales.layoutManager = LinearLayoutManager(this)

        saleAdapter = SaleAdapter(
            onEdit = { saleWithItems -> editSale(saleWithItems) },
            onDelete = { saleWithItems -> confirmDeleteSale(saleWithItems) }
        )
        recyclerSales.adapter = saleAdapter

        loadSales(siteId)

        fabNewSale.setOnClickListener {
            val intent = Intent(this, SaleActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val siteId = PrefsHelper.getActiveSiteId(this)
        loadSales(siteId)
    }

    private fun loadSales(siteId: String?) {
        if (siteId.isNullOrBlank()) return
        lifecycleScope.launch(Dispatchers.IO) {
            sdk.saleRepository.observeAllWithItemsForSite(siteId).collect { sales ->
                withContext(Dispatchers.Main) {
                    saleAdapter.submitList(sales)
                }
            }
        }
    }

    private fun editSale(saleWithItems: SaleWithItems) {
        val intent = Intent(this, SaleActivity::class.java)
        intent.putExtra("SALE_ID", saleWithItems.sale.id)
        startActivity(intent)
    }

    private fun confirmDeleteSale(saleWithItems: SaleWithItems) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sale")
            .setMessage("Are you sure you want to delete this sale for ${saleWithItems.sale.customerName}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSale(saleWithItems)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSale(saleWithItems: SaleWithItems) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete the sale and its items
                sdk.saleRepository.delete(saleWithItems.sale.id)

                // Reverse the stock movements by adding back the quantities
                val currentUser = authManager.getUsername().ifBlank { "system" }
                saleWithItems.items.forEach { item ->
                    val movement = sdk.createStockMovement(
                        productId = item.productId,
                        siteId = saleWithItems.sale.siteId,
                        quantity = item.quantity,
                        movementType = "in",
                        referenceId = "sale-reversal-${saleWithItems.sale.id}",
                        notes = "Sale deleted - stock restored",
                        userId = currentUser
                    )
                    sdk.stockMovementRepository.insert(movement)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SaleListActivity,
                        "Sale deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SaleListActivity,
                        "Error deleting sale: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
