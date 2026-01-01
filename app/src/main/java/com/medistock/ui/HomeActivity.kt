package com.medistock.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.sales.SaleListActivity
import com.medistock.ui.stock.StockListActivity
import com.medistock.ui.purchase.PurchaseActivity
import com.medistock.ui.inventory.InventoryActivity
import com.medistock.ui.admin.AdminActivity
import com.medistock.ui.admin.SupabaseConfigActivity
import com.medistock.ui.transfer.TransferListActivity
import com.medistock.util.AuthManager

class HomeActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)

        // Check if user is logged in
        if (!authManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Set title with user name
        supportActionBar?.title = "MediStock - ${authManager.getFullName()}"

        findViewById<android.view.View>(R.id.viewStockButton).setOnClickListener {
            startActivity(Intent(this, StockListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.sellProductButton).setOnClickListener {
            startActivity(Intent(this, SaleListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.transferProductButton).setOnClickListener {
            startActivity(Intent(this, TransferListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.purchaseButton).setOnClickListener {
            startActivity(Intent(this, PurchaseActivity::class.java))
        }

        findViewById<android.view.View>(R.id.inventoryButton).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        findViewById<android.view.View>(R.id.adminButton).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        findViewById<android.view.View>(R.id.supabaseConfigButton).setOnClickListener {
            val intent = Intent(this, SupabaseConfigActivity::class.java)
            intent.putExtra(SupabaseConfigActivity.EXTRA_HIDE_KEY, true)
            startActivity(intent)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
