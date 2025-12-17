package com.medistock.ui.site

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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
    private var siteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = AppDatabase.getInstance(this)

        val editName = findViewById<EditText>(R.id.editSiteName)
        val btnSave = findViewById<Button>(R.id.btnSaveSite)

        siteId = intent.getLongExtra("SITE_ID", -1).takeIf { it != -1L }
        if (siteId != null) {
            supportActionBar?.title = "Modifier le site"
            CoroutineScope(Dispatchers.IO).launch {
                val site = db.siteDao().getById(siteId!!).first()
                runOnUiThread {
                    if (site != null) {
                        editName.setText(site.name ?: "")
                    }
                }
            }
        } else {
            supportActionBar?.title = "Ajouter un site"
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Nom du site requis", Toast.LENGTH_SHORT).show()
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
