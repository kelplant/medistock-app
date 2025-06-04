package com.medistock.ui

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.*
import com.medistock.ui.adapter.ProductAdapter
import com.medistock.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nameInput = findViewById<EditText>(R.id.editProductName)
        val unitInput = findViewById<EditText>(R.id.editProductUnit)
        val priceInput = findViewById<EditText>(R.id.editPurchasePrice)
        val marginInput = findViewById<EditText>(R.id.editMarginValue)
        val categorySpinner = findViewById<Spinner>(R.id.spinnerCategory)
        val marginSpinner = findViewById<Spinner>(R.id.spinnerMarginType)
        val addButton = findViewById<Button>(R.id.btnAddProduct)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProducts)

        adapter = ProductAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        marginSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("fixed", "percentage"))

        viewModel.loadCategories()
        lifecycleScope.launchWhenStarted {
            viewModel.categories.collectLatest {
                categories = it
                categorySpinner.adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    it.map { cat -> cat.name }
                )
            }
        }

        addButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val unit = unitInput.text.toString().trim()
            val price = priceInput.text.toString().toDoubleOrNull()
            val margin = marginInput.text.toString().toDoubleOrNull()
            val marginType = marginSpinner.selectedItem.toString()
            val selectedCategory = categorySpinner.selectedItemPosition

            if (name.isNotEmpty() && unit.isNotEmpty() && price != null && margin != null && selectedCategory >= 0) {
                val categoryId = categories[selectedCategory].id
                val product = Product(
                    name = name,
                    unit = unit,
                    categoryId = categoryId,
                    marginType = marginType,
                    marginValue = margin
                )
                viewModel.addProductWithPrice(product, price)
                nameInput.text.clear()
                unitInput.text.clear()
                priceInput.text.clear()
                marginInput.text.clear()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.products.collectLatest { adapter.updateData(it) }
        }

        
        val siteId = com.medistock.util.PrefsHelper.getActiveSiteId(this)
        viewModel.loadProductsForSite(siteId)
        
    }
}

override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_switch_site -> {
            com.medistock.util.PrefsHelper.clearActiveSite(this)
            startActivity(Intent(this, com.medistock.ui.site.SiteSelectorActivity::class.java))
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
