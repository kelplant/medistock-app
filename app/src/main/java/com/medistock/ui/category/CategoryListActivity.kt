package com.medistock.ui.category

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.ui.adapters.CategoryAdapter
import com.medistock.shared.i18n.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryListActivity : AppCompatActivity() {
    private lateinit var adapter: CategoryAdapter
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.categories
        sdk = MedistockApplication.sdk

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerCategories)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddCategory)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CategoryAdapter { category ->
            val intent = Intent(this, CategoryAddEditActivity::class.java)
            intent.putExtra("CATEGORY_ID", category.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, CategoryAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = sdk.categoryRepository.getAll()
            runOnUiThread { adapter.submitList(categories) }
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