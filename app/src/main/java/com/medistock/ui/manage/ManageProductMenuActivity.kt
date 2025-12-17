package com.medistock.ui.manage

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.medistock.databinding.ActivityManageProductMenuBinding
import com.medistock.ui.category.CategoryListActivity
import com.medistock.ui.product.ProductListActivity

class ManageProductMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageProductMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, CategoryListActivity::class.java))
        }
        binding.btnProducts.setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
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