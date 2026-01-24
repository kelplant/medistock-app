package com.medistock.ui.site

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.ui.adapters.SiteAdapter
import com.medistock.shared.i18n.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SiteListActivity : AppCompatActivity() {
    private lateinit var adapter: SiteAdapter
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.sites
        sdk = MedistockApplication.sdk

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerSites)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddSite)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SiteAdapter { site ->
            val intent = Intent(this, SiteAddEditActivity::class.java)
            intent.putExtra("SITE_ID", site.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, SiteAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadSites()
    }

    private fun loadSites() {
        CoroutineScope(Dispatchers.IO).launch {
            val sites = sdk.siteRepository.getAll()
            runOnUiThread { adapter.submitList(sites) }
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
