package com.medistock.ui.site

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Site
import com.medistock.ui.MainActivity
import com.medistock.util.AuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiteSelectorActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private lateinit var selectedSite: Site
    private lateinit var sites: List<Site>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_selector)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        authManager = AuthManager.getInstance(this)

        val siteSpinner = findViewById<Spinner>(R.id.spinnerSite)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val newSiteInput = findViewById<EditText>(R.id.editNewSite)
        val btnAddSite = findViewById<Button>(R.id.btnAddSite)

        lifecycleScope.launch {
            sites = db.siteDao().getAll().first()
            siteSpinner.adapter = ArrayAdapter(
                this@SiteSelectorActivity,
                android.R.layout.simple_spinner_item,
                sites.map { it.name }
            )
        }

        btnAddSite.setOnClickListener {
            val siteName = newSiteInput.text.toString().trim()
            if (siteName.isNotEmpty()) {
                lifecycleScope.launch {
                    val currentUser = authManager.getUsername().ifBlank { "system" }
                    db.siteDao().insert(
                        Site(
                            name = siteName,
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                    )
                    val refreshedSites = db.siteDao().getAll().first()
                    sites = refreshedSites
                    siteSpinner.adapter = ArrayAdapter(
                        this@SiteSelectorActivity,
                        android.R.layout.simple_spinner_item,
                        refreshedSites.map { it.name }
                    )
                    newSiteInput.text.clear()
                }
            }
        }

        btnContinue.setOnClickListener {
            val selectedIndex = siteSpinner.selectedItemPosition
            if (selectedIndex >= 0) {
                selectedSite = sites[selectedIndex]
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("activeSiteId", selectedSite.id)
                com.medistock.util.PrefsHelper.saveActiveSiteId(this, selectedSite.id)
                startActivity(intent)
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
