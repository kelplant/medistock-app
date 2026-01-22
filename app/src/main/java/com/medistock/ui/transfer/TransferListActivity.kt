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
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.ProductTransfer
import com.medistock.shared.domain.model.Module
import com.medistock.util.AuthManager
import com.medistock.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransferListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: TransferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transfer Products"

        db = AppDatabase.getInstance(this)
        authManager = AuthManager.getInstance(this)
        permissionManager = PermissionManager(db.userPermissionDao(), authManager)

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
                        "You don't have permission to create transfers",
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
            val transfers = db.productTransferDao().getAll().first()
            val products = db.productDao().getAll().first().associateBy { it.id }
            val sites = db.siteDao().getAll().first().associateBy { it.id }

            withContext(Dispatchers.Main) {
                adapter = TransferAdapter(
                    transfers = transfers,
                    products = products,
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
                    "Edit transfer functionality is not available. Please create a new transfer instead.",
                    Toast.LENGTH_SHORT
                ).show()
                // Note: Editing transfers is complex because it requires reverting stock movements
                // For now, we disable this feature
            } else {
                Toast.makeText(
                    this@TransferListActivity,
                    "You don't have permission to edit transfers",
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
                    "You don't have permission to delete transfers",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val product = db.productDao().getById(transfer.productId).first()
            val fromSite = db.siteDao().getById(transfer.fromSiteId).first()
            val toSite = db.siteDao().getById(transfer.toSiteId).first()

            AlertDialog.Builder(this@TransferListActivity)
                .setTitle("Delete Transfer")
                .setMessage("Delete transfer of ${product?.name} from ${fromSite?.name} to ${toSite?.name}?\n\nNote: This will NOT revert stock movements.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteTransfer(transfer)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteTransfer(transfer: ProductTransfer) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.productTransferDao().delete(transfer)
                // Note: For simplicity, we don't automatically revert stock movements
                // This should be done manually or in a future enhancement

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferListActivity,
                        "Transfer deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadTransfers()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TransferListActivity,
                        "Error deleting transfer: ${e.message}",
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
