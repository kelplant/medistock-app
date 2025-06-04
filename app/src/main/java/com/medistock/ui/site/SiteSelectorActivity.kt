package com.medistock.ui.site

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Site
import com.medistock.ui.MainActivity
import kotlinx.coroutines.launch

class SiteSelectorActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var selectedSite: Site
    private lateinit var sites: List<Site>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_selector)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "medistock-db").build()

        val siteSpinner = findViewById<Spinner>(R.id.spinnerSite)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val newSiteInput = findViewById<EditText>(R.id.editNewSite)
        val btnAddSite = findViewById<Button>(R.id.btnAddSite)

        lifecycleScope.launch {
            sites = db.siteDao().getAll()
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
                    db.siteDao().insert(Site(name = siteName))
                    val refreshedSites = db.siteDao().getAll()
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
}