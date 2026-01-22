package com.medistock.ui.customer

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Customer
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class CustomerAddEditActivity : AppCompatActivity() {
    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var customerId: String? = null
    private var existingCustomer: Customer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        sdk = MedistockApplication.sdk
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
            lifecycleScope.launch {
                val customer = withContext(Dispatchers.IO) {
                    sdk.customerRepository.getById(customerId!!)
                }
                if (customer != null) {
                    existingCustomer = customer
                    editName.setText(customer.name)
                    editPhone.setText(customer.phone ?: "")
                    editAddress.setText(customer.address ?: "")
                    editNotes.setText(customer.notes ?: "")
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

            lifecycleScope.launch {
                val currentUser = authManager.getUsername().ifBlank { "system" }
                val siteId = existingCustomer?.siteId
                    ?: PrefsHelper.getActiveSiteId(this@CustomerAddEditActivity)
                    ?: ""

                withContext(Dispatchers.IO) {
                    if (customerId == null) {
                        val newCustomer = Customer(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            siteId = siteId,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                        sdk.customerRepository.insert(newCustomer)
                    } else {
                        val createdAt = existingCustomer?.createdAt ?: System.currentTimeMillis()
                        val createdBy = existingCustomer?.createdBy?.ifBlank { currentUser } ?: currentUser
                        val updatedCustomer = Customer(
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
                        sdk.customerRepository.update(updatedCustomer)
                    }
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

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sdk.customerRepository.delete(customerId!!)
            }
            Toast.makeText(this@CustomerAddEditActivity, "Customer deleted", Toast.LENGTH_SHORT).show()
            finish()
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
