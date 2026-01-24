package com.medistock.ui.site

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Site
import com.medistock.util.AuthManager
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SiteAddEditActivity : AppCompatActivity() {
    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var siteId: String? = null
    private var existingSite: Site? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        val editName = findViewById<EditText>(R.id.editSiteName)
        val btnSave = findViewById<Button>(R.id.btnSaveSite)
        val btnDelete = findViewById<Button>(R.id.btnDeleteSite)

        siteId = intent.getStringExtra("SITE_ID")?.takeIf { it.isNotBlank() }
        if (siteId != null) {
            supportActionBar?.title = L.strings.editSite
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val site = withContext(Dispatchers.IO) {
                    sdk.siteRepository.getById(siteId!!)
                }
                if (site != null) {
                    existingSite = site
                    editName.setText(site.name)
                }
            }
        } else {
            supportActionBar?.title = L.strings.addSite
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, L.strings.required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val currentUser = authManager.getUsername().ifBlank { "system" }
                withContext(Dispatchers.IO) {
                    if (siteId == null) {
                        val newSite = Site(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                        sdk.siteRepository.insert(newSite)
                    } else {
                        val createdAt = existingSite?.createdAt ?: System.currentTimeMillis()
                        val createdBy = existingSite?.createdBy?.ifBlank { currentUser } ?: currentUser
                        val updatedSite = Site(
                            id = siteId!!,
                            name = name,
                            createdAt = createdAt,
                            updatedAt = System.currentTimeMillis(),
                            createdBy = createdBy,
                            updatedBy = currentUser
                        )
                        sdk.siteRepository.update(updatedSite)
                    }
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
            .setTitle(L.strings.deleteSite)
            .setMessage("${L.strings.confirm}?")
            .setPositiveButton(L.strings.delete) { _, _ ->
                deleteSite()
            }
            .setNegativeButton(L.strings.cancel, null)
            .show()
    }

    private fun deleteSite() {
        if (siteId == null) return

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sdk.siteRepository.delete(siteId!!)
            }
            Toast.makeText(this@SiteAddEditActivity, L.strings.siteDeleted, Toast.LENGTH_SHORT).show()
            finish()
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
