package com.medistock.ui.category

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.ui.adapters.CategoryAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryListActivity : AppCompatActivity() {
    private lateinit var adapter: CategoryAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)
        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerCategories)
        val btnAdd = findViewById<Button>(R.id.btnAddCategory)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CategoryAdapter { category ->
            val intent = Intent(this, CategoryAddEditActivity::class.java)
            intent.putExtra("CATEGORY_ID", category.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, CategoryAddEditActivity::class.java))
        }
        loadCategories()
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = db.categoryDao().getAll()
            runOnUiThread { adapter.submitList(categories) }
        }
    }
}