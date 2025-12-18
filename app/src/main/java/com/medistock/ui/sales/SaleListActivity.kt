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
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.SaleWithItems
import com.medistock.ui.adapters.SaleAdapter
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaleListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerSales: RecyclerView
    private lateinit var fabNewSale: FloatingActionButton
    private lateinit var saleAdapter: SaleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales"

        db = AppDatabase.getInstance(this)
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

    private fun loadSales(siteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.saleDao().getAllWithItemsForSite(siteId).collect { sales ->
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
                // Delete the sale (cascade will delete sale items)
                db.saleDao().delete(saleWithItems.sale)

                // Reverse the stock movements by adding back the quantities
                saleWithItems.items.forEach { item ->
                    val movement = com.medistock.data.entities.StockMovement(
                        productId = item.productId,
                        type = "in",
                        quantity = item.quantity,
                        date = System.currentTimeMillis(),
                        siteId = saleWithItems.sale.siteId,
                        purchasePriceAtMovement = 0.0,
                        sellingPriceAtMovement = item.pricePerUnit
                    )
                    db.stockMovementDao().insert(movement)
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
