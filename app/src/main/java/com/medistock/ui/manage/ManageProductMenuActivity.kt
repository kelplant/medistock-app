package com.medistock.ui.manage

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.databinding.ActivityManageProductMenuBinding
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Module
import com.medistock.ui.category.CategoryListActivity
import com.medistock.ui.product.ProductListActivity
import com.medistock.util.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageProductMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageProductMenuBinding
    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk

        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, CategoryListActivity::class.java))
        }
        binding.btnProducts.setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }

        // Apply permission-based visibility
        applyPermissionVisibility()
    }

    /**
     * Apply permission-based visibility to menu items.
     */
    private fun applyPermissionVisibility() {
        val userId = authManager.getUserId() ?: return // Not logged in
        val isAdmin = authManager.isAdmin()

        lifecycleScope.launch {
            try {
                val permissions = withContext(Dispatchers.IO) {
                    sdk.permissionService.getAllModulePermissions(userId, isAdmin)
                }

                binding.btnCategories.visibility =
                    if (permissions[Module.CATEGORIES]?.canView == true) View.VISIBLE else View.GONE

                binding.btnProducts.visibility =
                    if (permissions[Module.PRODUCTS]?.canView == true) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                // Fail-closed for security: hide all buttons on error
                binding.btnCategories.visibility = View.GONE
                binding.btnProducts.visibility = View.GONE
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