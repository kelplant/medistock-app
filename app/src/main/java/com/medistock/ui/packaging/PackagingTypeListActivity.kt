package com.medistock.ui.packaging

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.R
import com.medistock.ui.viewmodel.PackagingTypeViewModel
import com.medistock.shared.i18n.L
import kotlinx.coroutines.launch

class PackagingTypeListActivity : AppCompatActivity() {

    private lateinit var viewModel: PackagingTypeViewModel
    private lateinit var adapter: PackagingTypeAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packaging_type_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.packagingTypes

        viewModel = ViewModelProvider(this)[PackagingTypeViewModel::class.java]

        setupRecyclerView()
        setupAddButton()
        observePackagingTypes()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewPackagingTypes)
        adapter = PackagingTypeAdapter(
            onEdit = { packagingType ->
                val intent = Intent(this, PackagingTypeAddEditActivity::class.java)
                intent.putExtra("PACKAGING_TYPE_ID", packagingType.id)
                startActivity(intent)
            },
            onDelete = { packagingType ->
                confirmDelete(packagingType.id, packagingType.name)
            },
            onToggleActive = { packagingType ->
                toggleActive(packagingType.id, packagingType.isActive)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupAddButton() {
        findViewById<FloatingActionButton>(R.id.fabAddPackagingType).setOnClickListener {
            startActivity(Intent(this, PackagingTypeAddEditActivity::class.java))
        }
    }

    private fun observePackagingTypes() {
        lifecycleScope.launch {
            viewModel.packagingTypes.collect { types ->
                adapter.submitList(types)
            }
        }
    }

    private fun confirmDelete(id: String, name: String) {
        viewModel.isUsedByProducts(id) { isUsed ->
            runOnUiThread {
                if (isUsed) {
                    AlertDialog.Builder(this@PackagingTypeListActivity)
                        .setTitle(L.strings.cannotDelete)
                        .setMessage("${L.strings.entityInUse}. ${L.strings.deactivateInstead}")
                        .setPositiveButton(L.strings.ok, null)
                        .show()
                } else {
                    AlertDialog.Builder(this@PackagingTypeListActivity)
                        .setTitle(L.strings.confirm)
                        .setMessage("${L.strings.confirm}?")
                        .setPositiveButton(L.strings.delete) { _, _ ->
                            viewModel.getById(id) { packagingType ->
                                packagingType?.let {
                                    viewModel.delete(it) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@PackagingTypeListActivity,
                                                L.strings.success,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                        .setNegativeButton(L.strings.cancel, null)
                        .show()
                }
            }
        }
    }

    private fun toggleActive(id: String, currentlyActive: Boolean) {
        val callback = {
            runOnUiThread {
                val message = if (currentlyActive) L.strings.inactive else L.strings.active
                Toast.makeText(this@PackagingTypeListActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        if (currentlyActive) {
            viewModel.deactivate(id, callback)
        } else {
            viewModel.activate(id, callback)
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

    override fun onResume() {
        super.onResume()
        viewModel.loadPackagingTypes()
    }
}
