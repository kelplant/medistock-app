package com.medistock.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
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
import com.medistock.shared.domain.model.ProductTransfer
import com.medistock.shared.domain.model.Module
import com.medistock.util.AuthManager
import com.medistock.util.PermissionManager
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransferListActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: TransferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.transfers

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)
        permissionManager = PermissionManager(sdk.userPermissionRepository, authManager)

        recyclerView = findViewById(R.id.recyclerTransfers)
        fab = findViewById(R.id.fabNewTransfer)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Check create permission
        lifecycleScope.launch {
            val canCreate = permissionManager.canCreate(Module.TRANSFERS)
            fab.isEnabled = canCreate
            if (!canCreate) {
                fab.hide()
            }
        }

        fab.setOnClickListener {
            lifecycleScope.launch {
                if (permissionManager.canCreate(Module.TRANSFERS)) {
                    startActivity(Intent(this@TransferListActivity, TransferActivity::class.java))
                } else {
                    Toast.makeText(
                        this@TransferListActivity,
                        L.strings.error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        loadTransfers()
    }

    override fun onResume() {
        super.onResume()
        loadTransfers()
    }

    private fun loadTransfers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transfers = sdk.productTransferRepository.getAll()
            val products = sdk.productRepository.getAll().associateBy { it.id }
            val packagingTypes = sdk.packagingTypeRepository.getAll().associateBy { it.id }
            val sites = sdk.siteRepository.getAll().associateBy { it.id }

            withContext(Dispatchers.Main) {
                adapter = TransferAdapter(
                    transfers = transfers,
                    products = products,
                    packagingTypes = packagingTypes,
                    sites = sites,
                    onEditClick = { transfer -> editTransfer(transfer) },
                    onDeleteClick = { transfer -> confirmDeleteTransfer(transfer) }
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun editTransfer(transfer: ProductTransfer) {
        lifecycleScope.launch {
            if (permissionManager.canEdit(Module.TRANSFERS)) {
                Toast.makeText(
                    this@TransferListActivity,
                    L.strings.warning,
                    Toast.LENGTH_SHORT
                ).show()
                // Note: Editing transfers is complex because it requires reverting stock movements
                // For now, we disable this feature
            } else {
                Toast.makeText(
                    this@TransferListActivity,
                    L.strings.error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteTransfer(transfer: ProductTransfer) {
        lifecycleScope.launch {
            if (!permissionManager.canDelete(Module.TRANSFERS)) {
                Toast.makeText(
                    this@TransferListActivity,
                    L.strings.error,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val (product, fromSite, toSite) = withContext(Dispatchers.IO) {
                Triple(
                    sdk.productRepository.getById(transfer.productId),
                    sdk.siteRepository.getById(transfer.fromSiteId),
                    sdk.siteRepository.getById(transfer.toSiteId)
                )
            }

            AlertDialog.Builder(this@TransferListActivity)
                .setTitle("${L.strings.delete} ${L.strings.transfer}")
                .setMessage("${L.strings.confirm}?")
                .setPositiveButton(L.strings.delete) { _, _ ->
                    deleteTransfer(transfer)
                }
                .setNegativeButton(L.strings.cancel, null)
                .show()
        }
    }

    private fun deleteTransfer(transfer: ProductTransfer) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                sdk.productTransferRepository.delete(transfer.id)
                // Note: For simplicity, we don't automatically revert stock movements
                // This should be done manually or in a future enhancement

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferListActivity,
                        L.strings.success,
                        Toast.LENGTH_SHORT
                    ).show()
                    loadTransfers()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferListActivity,
                        "${L.strings.error}: ${e.message}",
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
