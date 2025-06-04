package com.medistock.ui.manage

import android.content.Intent
import android.os.Bundle
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

        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, CategoryListActivity::class.java))
        }
        binding.btnProducts.setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }
    }
}