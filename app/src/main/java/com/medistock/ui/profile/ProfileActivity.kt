package com.medistock.ui.profile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.auth.ChangePasswordActivity
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.customer.CustomerListActivity
import com.medistock.ui.inventory.InventoryListActivity
import com.medistock.ui.purchase.PurchaseListActivity
import com.medistock.util.AuthManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        authManager = AuthManager.getInstance(this)

        setupUserInfo()
        setupMenuItems()
        setupLogout()
        setupAppVersion()
    }

    private fun setupUserInfo() {
        val textFullName = findViewById<TextView>(R.id.textFullName)
        val textUsername = findViewById<TextView>(R.id.textUsername)
        val textUserRole = findViewById<TextView>(R.id.textUserRole)

        textFullName.text = authManager.getFullName().ifBlank { "User" }
        textUsername.text = "@${authManager.getUsername()}"

        if (authManager.isAdmin()) {
            textUserRole.text = "Administrator"
            textUserRole.setBackgroundColor(0xFF2196F3.toInt()) // Blue
        } else {
            textUserRole.text = "User"
            textUserRole.setBackgroundColor(0xFF4CAF50.toInt()) // Green
        }
    }

    private fun setupMenuItems() {
        // Change Password
        findViewById<android.view.View>(R.id.btnChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Customers
        findViewById<android.view.View>(R.id.btnCustomers).setOnClickListener {
            startActivity(Intent(this, CustomerListActivity::class.java))
        }

        // Purchase History
        findViewById<android.view.View>(R.id.btnPurchaseHistory).setOnClickListener {
            startActivity(Intent(this, PurchaseListActivity::class.java))
        }

        // Inventory History
        findViewById<android.view.View>(R.id.btnInventoryHistory).setOnClickListener {
            startActivity(Intent(this, InventoryListActivity::class.java))
        }
    }

    private fun setupLogout() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        authManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupAppVersion() {
        val textAppVersion = findViewById<TextView>(R.id.textAppVersion)
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            textAppVersion.text = "Version $versionName ($versionCode)"
        } catch (e: Exception) {
            textAppVersion.text = "Version 1.0.0"
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
