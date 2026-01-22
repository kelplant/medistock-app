package com.medistock.ui.customer

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Customer
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CustomerAddEditActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private var customerId: String? = null
    private var existingCustomer: Customer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = AppDatabase.getInstance(this)
        authManager = AuthManager.getInstance(this)

        val editName = findViewById<TextInputEditText>(R.id.editCustomerName)
        val editPhone = findViewById<TextInputEditText>(R.id.editCustomerPhone)
        val editAddress = findViewById<TextInputEditText>(R.id.editCustomerAddress)
        val editNotes = findViewById<TextInputEditText>(R.id.editCustomerNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveCustomer)
        val btnDelete = findViewById<Button>(R.id.btnDeleteCustomer)

        customerId = intent.getStringExtra("CUSTOMER_ID")?.takeIf { it.isNotBlank() }
        if (customerId != null) {
            supportActionBar?.title = "Edit Customer"
            btnDelete.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val customer = db.customerDao().getById(customerId!!).first()
                runOnUiThread {
                    if (customer != null) {
                        existingCustomer = customer
                        editName.setText(customer.name)
                        editPhone.setText(customer.phone ?: "")
                        editAddress.setText(customer.address ?: "")
                        editNotes.setText(customer.notes ?: "")
                    }
                }
            }
        } else {
            supportActionBar?.title = "Add Customer"
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Customer name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phone = editPhone.text.toString().trim().takeIf { it.isNotBlank() }
            val address = editAddress.text.toString().trim().takeIf { it.isNotBlank() }
            val notes = editNotes.text.toString().trim().takeIf { it.isNotBlank() }

            CoroutineScope(Dispatchers.IO).launch {
                val currentUser = authManager.getUsername().ifBlank { "system" }
                val siteId = existingCustomer?.siteId
                    ?: PrefsHelper.getActiveSiteId(this@CustomerAddEditActivity)
                    ?: ""

                if (customerId == null) {
                    db.customerDao().insert(
                        Customer(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            siteId = siteId,
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                    )
                } else {
                    val createdAt = existingCustomer?.createdAt ?: System.currentTimeMillis()
                    val createdBy = existingCustomer?.createdBy?.ifBlank { currentUser } ?: currentUser
                    db.customerDao().update(
                        Customer(
                            id = customerId!!,
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            siteId = siteId,
                            createdAt = createdAt,
                            updatedAt = System.currentTimeMillis(),
                            createdBy = createdBy,
                            updatedBy = currentUser
                        )
                    )
                }
                finish()
            }
        }

        btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete this customer?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCustomer()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCustomer() {
        if (customerId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val customer = db.customerDao().getById(customerId!!).first()
            if (customer != null) {
                db.customerDao().delete(customer)
                runOnUiThread {
                    Toast.makeText(this@CustomerAddEditActivity, "Customer deleted", Toast.LENGTH_SHORT).show()
                    finish()
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
