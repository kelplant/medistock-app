package com.medistock.ui.site

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Site
import com.medistock.ui.MainActivity
import com.medistock.util.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SiteSelectorActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var selectedSite: Site
    private var sites: List<Site> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_selector)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        val siteSpinner = findViewById<Spinner>(R.id.spinnerSite)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val newSiteInput = findViewById<EditText>(R.id.editNewSite)
        val btnAddSite = findViewById<Button>(R.id.btnAddSite)

        lifecycleScope.launch {
            sites = withContext(Dispatchers.IO) {
                sdk.siteRepository.getAll()
            }
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
                    val currentTime = System.currentTimeMillis()
                    val newSite = Site(
                        id = UUID.randomUUID().toString(),
                        name = siteName,
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        createdBy = currentUser,
                        updatedBy = currentUser
                    )
                    withContext(Dispatchers.IO) {
                        sdk.siteRepository.insert(newSite)
                    }
                    val refreshedSites = withContext(Dispatchers.IO) {
                        sdk.siteRepository.getAll()
                    }
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
