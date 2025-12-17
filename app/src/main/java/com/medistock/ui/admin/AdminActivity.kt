package com.medistock.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.category.CategoryListActivity
import com.medistock.ui.site.SiteListActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Administration"

        findViewById<android.view.View>(R.id.btnManageSites).setOnClickListener {
            startActivity(Intent(this, SiteListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnManageCategories).setOnClickListener {
            startActivity(Intent(this, CategoryListActivity::class.java))
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
