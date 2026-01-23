package com.medistock.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.customer.CustomerListActivity
import com.medistock.ui.manage.ManageProductMenuActivity
import com.medistock.ui.site.SiteListActivity
import com.medistock.ui.movement.StockMovementListActivity
import com.medistock.ui.user.UserListActivity
import com.medistock.ui.packaging.PackagingTypeListActivity
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

        findViewById<android.view.View>(R.id.btnManagePackagingTypes).setOnClickListener {
            startActivity(Intent(this, PackagingTypeListActivity::class.java))
        }

        // Customers management - visible for all admins
        findViewById<android.view.View>(R.id.btnManageCustomers).setOnClickListener {
            startActivity(Intent(this, CustomerListActivity::class.java))
        }

        // User management button - only visible for admins
        val btnManageUsers = findViewById<android.view.View>(R.id.btnManageUsers)
        val btnAuditHistory = findViewById<android.view.View>(R.id.btnAuditHistory)

        if (authManager.isAdmin()) {
            btnManageUsers.visibility = View.VISIBLE
            btnManageUsers.setOnClickListener {
                startActivity(Intent(this, UserListActivity::class.java))
            }

            btnAuditHistory.visibility = View.VISIBLE
            btnAuditHistory.setOnClickListener {
                startActivity(Intent(this, AuditHistoryActivity::class.java))
            }
        } else {
            btnManageUsers.visibility = View.GONE
            btnAuditHistory.visibility = View.GONE
        }

        // Supabase configuration - visible for all admins
        findViewById<android.view.View>(R.id.btnSupabaseConfig).setOnClickListener {
            startActivity(Intent(this, SupabaseConfigActivity::class.java))
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
