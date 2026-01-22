package com.medistock.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.ui.adapters.CustomerAdapter
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CustomerListActivity : AppCompatActivity() {
    private lateinit var adapter: CustomerAdapter
    private lateinit var db: AppDatabase
    private var siteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Customers"
        db = AppDatabase.getInstance(this)
        siteId = PrefsHelper.getActiveSiteId(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerCustomers)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddCustomer)
        val editSearch = findViewById<TextInputEditText>(R.id.editSearchCustomer)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CustomerAdapter { customer ->
            val intent = Intent(this, CustomerAddEditActivity::class.java)
            intent.putExtra("CUSTOMER_ID", customer.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, CustomerAddEditActivity::class.java))
        }

        editSearch.addTextChangedListener { text ->
            searchCustomers(text?.toString() ?: "")
        }
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
    }

    private fun loadCustomers() {
        CoroutineScope(Dispatchers.IO).launch {
            val customers = if (siteId != null) {
                db.customerDao().getAllForSite(siteId!!).first()
            } else {
                db.customerDao().getAll().first()
            }
            runOnUiThread { adapter.submitList(customers) }
        }
    }

    private fun searchCustomers(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val customers = if (query.isBlank()) {
                if (siteId != null) {
                    db.customerDao().getAllForSite(siteId!!).first()
                } else {
                    db.customerDao().getAll().first()
                }
            } else {
                if (siteId != null) {
                    db.customerDao().search(siteId!!, query).first()
                } else {
                    db.customerDao().getAll().first().filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.phone?.contains(query, ignoreCase = true) == true
                    }
                }
            }
            runOnUiThread { adapter.submitList(customers) }
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
