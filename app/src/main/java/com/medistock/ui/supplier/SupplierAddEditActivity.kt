package com.medistock.ui.supplier

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
import com.medistock.shared.domain.model.Supplier
import com.medistock.shared.domain.validation.DeletionCheck
import com.medistock.shared.domain.validation.EntityType
import com.medistock.util.AuthManager
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SupplierAddEditActivity : AppCompatActivity() {
    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var supplierId: String? = null
    private var existingSupplier: Supplier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        val editName = findViewById<TextInputEditText>(R.id.editSupplierName)
        val editPhone = findViewById<TextInputEditText>(R.id.editSupplierPhone)
        val editEmail = findViewById<TextInputEditText>(R.id.editSupplierEmail)
        val editAddress = findViewById<TextInputEditText>(R.id.editSupplierAddress)
        val editNotes = findViewById<TextInputEditText>(R.id.editSupplierNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveSupplier)
        val btnDelete = findViewById<Button>(R.id.btnDeleteSupplier)

        supplierId = intent.getStringExtra("SUPPLIER_ID")?.takeIf { it.isNotBlank() }
        if (supplierId != null) {
            supportActionBar?.title = L.strings.editSupplier
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val supplier = withContext(Dispatchers.IO) {
                    sdk.supplierRepository.getById(supplierId!!)
                }
                if (supplier != null) {
                    existingSupplier = supplier
                    editName.setText(supplier.name)
                    editPhone.setText(supplier.phone ?: "")
                    editEmail.setText(supplier.email ?: "")
                    editAddress.setText(supplier.address ?: "")
                    editNotes.setText(supplier.notes ?: "")
                }
            }
        } else {
            supportActionBar?.title = L.strings.addSupplier
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, L.strings.required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phone = editPhone.text.toString().trim().takeIf { it.isNotBlank() }
            val email = editEmail.text.toString().trim().takeIf { it.isNotBlank() }
            val address = editAddress.text.toString().trim().takeIf { it.isNotBlank() }
            val notes = editNotes.text.toString().trim().takeIf { it.isNotBlank() }

            lifecycleScope.launch {
                val currentUser = authManager.getUsername().ifBlank { "system" }

                withContext(Dispatchers.IO) {
                    if (supplierId == null) {
                        val newSupplier = Supplier(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            phone = phone,
                            email = email,
                            address = address,
                            notes = notes,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                        sdk.supplierRepository.insert(newSupplier)
                    } else {
                        val createdAt = existingSupplier?.createdAt ?: System.currentTimeMillis()
                        val createdBy = existingSupplier?.createdBy?.ifBlank { currentUser } ?: currentUser
                        val updatedSupplier = Supplier(
                            id = supplierId!!,
                            name = name,
                            phone = phone,
                            email = email,
                            address = address,
                            notes = notes,
                            isActive = existingSupplier?.isActive ?: true,
                            createdAt = createdAt,
                            updatedAt = System.currentTimeMillis(),
                            createdBy = createdBy,
                            updatedBy = currentUser
                        )
                        sdk.supplierRepository.update(updatedSupplier)
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
            .setTitle(L.strings.deleteSupplier)
            .setMessage(L.strings.deleteSupplierConfirm)
            .setPositiveButton(L.strings.delete) { _, _ ->
                deleteSupplier()
            }
            .setNegativeButton(L.strings.cancel, null)
            .show()
    }

    private fun deleteSupplier() {
        if (supplierId == null) return

        lifecycleScope.launch {
            val check = withContext(Dispatchers.IO) {
                sdk.referentialIntegrityService.checkDeletion(EntityType.SUPPLIER, supplierId!!)
            }
            when (check) {
                is DeletionCheck.CanDelete -> {
                    withContext(Dispatchers.IO) {
                        sdk.supplierRepository.delete(supplierId!!)
                    }
                    Toast.makeText(this@SupplierAddEditActivity, L.strings.supplierDeleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is DeletionCheck.MustDeactivate -> {
                    val msg = "${L.strings.cannotDelete}: ${L.strings.entityInUse
                        .replace("{entity}", L.strings.supplier)
                        .replace("{count}", check.usageDetails.totalUsageCount.toString())}"
                    AlertDialog.Builder(this@SupplierAddEditActivity)
                        .setTitle(L.strings.cannotDelete)
                        .setMessage("$msg\n\n${L.strings.supplierDeactivated}?")
                        .setPositiveButton(L.strings.confirm) { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    val currentUser = authManager.getUsername().ifBlank { "system" }
                                    sdk.supplierRepository.deactivate(supplierId!!, currentUser)
                                }
                                Toast.makeText(this@SupplierAddEditActivity, L.strings.supplierDeactivated, Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .setNegativeButton(L.strings.cancel, null)
                        .show()
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
