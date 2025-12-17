package com.medistock.ui.site

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.ui.adapters.SiteAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiteListActivity : AppCompatActivity() {
    private lateinit var adapter: SiteAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Sites"
        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerSites)
        val btnAdd = findViewById<Button>(R.id.btnAddSite)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SiteAdapter { site ->
            val intent = Intent(this, SiteAddEditActivity::class.java)
            intent.putExtra("SITE_ID", site.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, SiteAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadSites()
    }

    private fun loadSites() {
        CoroutineScope(Dispatchers.IO).launch {
            val sites = db.siteDao().getAll().first()
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
