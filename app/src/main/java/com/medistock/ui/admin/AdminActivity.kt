package com.medistock.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.manage.ManageProductMenuActivity
import com.medistock.ui.site.SiteListActivity
import com.medistock.ui.movement.StockMovementListActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Administration"

        findViewById<android.view.View>(R.id.btnManageSites).setOnClickListener {
            startActivity(Intent(this, SiteListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, ManageProductMenuActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnStockMovement).setOnClickListener {
            startActivity(Intent(this, StockMovementListActivity::class.java))
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
