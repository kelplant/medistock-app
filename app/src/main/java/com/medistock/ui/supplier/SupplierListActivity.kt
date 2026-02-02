package com.medistock.ui.supplier

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.ui.adapters.SupplierAdapter
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupplierListActivity : AppCompatActivity() {
    private lateinit var adapter: SupplierAdapter
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.suppliers
        sdk = MedistockApplication.sdk

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerSuppliers)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddSupplier)
        val editSearch = findViewById<TextInputEditText>(R.id.editSearchSupplier)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SupplierAdapter { supplier ->
            val intent = Intent(this, SupplierAddEditActivity::class.java)
            intent.putExtra("SUPPLIER_ID", supplier.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, SupplierAddEditActivity::class.java))
        }

        editSearch.addTextChangedListener { text ->
            searchSuppliers(text?.toString() ?: "")
        }
    }

    override fun onResume() {
        super.onResume()
        loadSuppliers()
    }

    private fun loadSuppliers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val suppliers = sdk.supplierRepository.getAll()
            runOnUiThread { adapter.submitList(suppliers) }
        }
    }

    private fun searchSuppliers(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val suppliers = if (query.isBlank()) {
                sdk.supplierRepository.getAll()
            } else {
                sdk.supplierRepository.search(query)
            }
            runOnUiThread { adapter.submitList(suppliers) }
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
