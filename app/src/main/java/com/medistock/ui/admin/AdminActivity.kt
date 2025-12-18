package com.medistock.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.manage.ManageProductMenuActivity
import com.medistock.ui.site.SiteListActivity
import com.medistock.ui.movement.StockMovementListActivity
import com.medistock.ui.user.UserListActivity
import com.medistock.util.AuthManager

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Administration"

        val authManager = AuthManager.getInstance(this)

        findViewById<android.view.View>(R.id.btnManageSites).setOnClickListener {
            startActivity(Intent(this, SiteListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, ManageProductMenuActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnStockMovement).setOnClickListener {
            startActivity(Intent(this, StockMovementListActivity::class.java))
        }

        // User management button - only visible for admins
        val btnManageUsers = findViewById<android.view.View>(R.id.btnManageUsers)
        if (authManager.isAdmin()) {
            btnManageUsers.visibility = View.VISIBLE
            btnManageUsers.setOnClickListener {
                startActivity(Intent(this, UserListActivity::class.java))
            }
        } else {
            btnManageUsers.visibility = View.GONE
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
