package com.medistock.ui.site

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
import com.medistock.data.entities.Site
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiteAddEditActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private var siteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = AppDatabase.getInstance(this)

        val editName = findViewById<EditText>(R.id.editSiteName)
        val btnSave = findViewById<Button>(R.id.btnSaveSite)
        val btnDelete = findViewById<Button>(R.id.btnDeleteSite)

        siteId = intent.getStringExtra("SITE_ID")?.takeIf { it.isNotBlank() }
        if (siteId != null) {
            supportActionBar?.title = "Edit Site"
            btnDelete.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val site = db.siteDao().getById(siteId!!).first()
                runOnUiThread {
                    if (site != null) {
                        editName.setText(site.name)
                    }
                }
            }
        } else {
            supportActionBar?.title = "Add Site"
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Site name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                if (siteId == null) {
                    db.siteDao().insert(Site(name = name))
                } else {
                    db.siteDao().update(Site(id = siteId!!, name = name))
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
            .setTitle("Delete Site")
            .setMessage("Are you sure you want to delete this site?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSite()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSite() {
        if (siteId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val site = db.siteDao().getById(siteId!!).first()
            if (site != null) {
                db.siteDao().delete(site)
                runOnUiThread {
                    Toast.makeText(this@SiteAddEditActivity, "Site deleted", Toast.LENGTH_SHORT).show()
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
