package com.medistock.ui.category

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryAddEditActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var categoryId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_add_edit)
        db = AppDatabase.getInstance(this)

        val editName = findViewById<EditText>(R.id.editCategoryName)
        val btnSave = findViewById<Button>(R.id.btnSaveCategory)

        categoryId = intent.getLongExtra("CATEGORY_ID", -1).takeIf { it != -1L }
        if (categoryId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val cat = db.categoryDao().getById(categoryId!!)
                runOnUiThread {
                    if (cat != null) {
                        editName.setText(cat.name ?: "")
                    }
                }
            }
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
    }
}