package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "medistock-db"
    ).build()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private fun loadProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _products.value = db.productDao().getAll().first()
        }
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _categories.value = db.categoryDao().getAll().first()
        }
    }

    fun addProductWithPrice(product: Product, purchasePrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().insert(product)
            val sellingPrice = when (product.marginType) {
                "fixed" -> purchasePrice + (product.marginValue ?: 0.0)
                "percentage" -> purchasePrice * (1 + (product.marginValue ?: 0.0) / 100)
                else -> purchasePrice
            }
            db.productPriceDao().insert(
                ProductPrice(
                    productId = product.id,
                    effectiveDate = System.currentTimeMillis(),
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    source = "calculated"
                )
            )
            loadProducts()
        }
    }

    fun loadProductsForSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _products.value = db.productDao().getProductsForSite(siteId).first()
        }
    }
}
