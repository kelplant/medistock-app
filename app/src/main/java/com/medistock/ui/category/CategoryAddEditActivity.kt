package com.medistock.ui.category

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Category
import com.medistock.util.AuthManager
import com.medistock.shared.i18n.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryAddEditActivity : AppCompatActivity() {
    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var categoryId: String? = null
    private var existingCategory: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        val editName = findViewById<EditText>(R.id.editCategoryName)
        val btnSave = findViewById<Button>(R.id.btnSaveCategory)
        val btnDelete = findViewById<Button>(R.id.btnDeleteCategory)

        categoryId = intent.getStringExtra("CATEGORY_ID")?.takeIf { it.isNotBlank() }
        if (categoryId != null) {
            supportActionBar?.title = L.strings.editCategory
            btnDelete.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val cat = sdk.categoryRepository.getById(categoryId!!)
                runOnUiThread {
                    if (cat != null) {
                        existingCategory = cat
                        editName.setText(cat.name)
                    }
                }
            }
        } else {
            supportActionBar?.title = L.strings.addCategory
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, L.strings.required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val currentUser = authManager.getUsername().ifBlank { "system" }
                if (categoryId == null) {
                    val newCategory = sdk.createCategory(name = name, userId = currentUser)
                    sdk.categoryRepository.insert(newCategory)
                } else {
                    val updatedCategory = Category(
                        id = categoryId!!,
                        name = name,
                        createdAt = existingCategory?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        createdBy = existingCategory?.createdBy?.ifBlank { currentUser } ?: currentUser,
                        updatedBy = currentUser
                    )
                    sdk.categoryRepository.update(updatedCategory)
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
            .setTitle(L.strings.deleteCategory)
            .setMessage("${L.strings.confirm}?")
            .setPositiveButton(L.strings.delete) { _, _ ->
                deleteCategory()
            }
            .setNegativeButton(L.strings.cancel, null)
            .show()
    }

    private fun deleteCategory() {
        if (categoryId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            sdk.categoryRepository.delete(categoryId!!)
            runOnUiThread {
                Toast.makeText(this@CategoryAddEditActivity, L.strings.categoryDeleted, Toast.LENGTH_SHORT).show()
                finish()
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
