package com.medistock.ui.packaging

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.ui.viewmodel.PackagingTypeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackagingTypeListActivity : AppCompatActivity() {

    private lateinit var viewModel: PackagingTypeViewModel
    private lateinit var adapter: PackagingTypeAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packaging_type_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Types de conditionnement"

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
        findViewById<Button>(R.id.btnAddPackagingType).setOnClickListener {
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

    private fun confirmDelete(id: Long, name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val isUsed = viewModel.isUsedByProducts(id)

            withContext(Dispatchers.Main) {
                if (isUsed) {
                    AlertDialog.Builder(this@PackagingTypeListActivity)
                        .setTitle("Impossible de supprimer")
                        .setMessage("Le type de conditionnement '$name' est utilisé par des produits. Désactivez-le plutôt.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@PackagingTypeListActivity)
                        .setTitle("Confirmer la suppression")
                        .setMessage("Voulez-vous vraiment supprimer le type de conditionnement '$name' ?")
                        .setPositiveButton("Supprimer") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val packagingType = viewModel.getById(id)
                                packagingType?.let {
                                    viewModel.delete(it)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@PackagingTypeListActivity,
                                            "Type de conditionnement supprimé",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        .setNegativeButton("Annuler", null)
                        .show()
                }
            }
        }
    }

    private fun toggleActive(id: Long, currentlyActive: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (currentlyActive) {
                viewModel.deactivate(id)
            } else {
                viewModel.activate(id)
            }
            withContext(Dispatchers.Main) {
                val message = if (currentlyActive) "Désactivé" else "Activé"
                Toast.makeText(this@PackagingTypeListActivity, message, Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        viewModel.loadPackagingTypes()
    }
}
