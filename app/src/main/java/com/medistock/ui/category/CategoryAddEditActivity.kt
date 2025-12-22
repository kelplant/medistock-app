package com.medistock.ui.category

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CategoryAddEditActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = AppDatabase.getInstance(this)

        val editName = findViewById<EditText>(R.id.editCategoryName)
        val btnSave = findViewById<Button>(R.id.btnSaveCategory)
        val btnDelete = findViewById<Button>(R.id.btnDeleteCategory)

        categoryId = intent.getStringExtra("CATEGORY_ID")?.takeIf { it.isNotBlank() }
        if (categoryId != null) {
            supportActionBar?.title = "Edit Category"
            btnDelete.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val cat = db.categoryDao().getById(categoryId!!).first()
                runOnUiThread {
                    if (cat != null) {
                        editName.setText(cat.name)
                    }
                }
            }
        } else {
            supportActionBar?.title = "Add Category"
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                if (categoryId == null) {
                    db.categoryDao().insert(Category(name = name))
                } else {
                    db.categoryDao().update(Category(id = categoryId!!, name = name))
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
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory() {
        if (categoryId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val category = db.categoryDao().getById(categoryId!!).first()
            if (category != null) {
                db.categoryDao().delete(category)
                runOnUiThread {
                    Toast.makeText(this@CategoryAddEditActivity, "Category deleted", Toast.LENGTH_SHORT).show()
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
