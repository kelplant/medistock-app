package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.MedistockApplication
import com.medistock.shared.domain.model.Customer
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.SaleResult
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for sales using the shared SaleUseCase
 */
class SaleViewModel(application: Application) : AndroidViewModel(application) {

    private val sdk = MedistockApplication.sdk

    // UI State
    sealed class SaleUiState {
        object Idle : SaleUiState()
        object Loading : SaleUiState()
        data class Success(val result: SaleResult, val warnings: List<BusinessWarning>) : SaleUiState()
        data class Error(val error: BusinessError) : SaleUiState()
    }

    private val _uiState = MutableStateFlow<SaleUiState>(SaleUiState.Idle)
    val uiState: StateFlow<SaleUiState> = _uiState.asStateFlow()

    private val _sites = MutableStateFlow<List<Site>>(emptyList())
    val sites: StateFlow<List<Site>> = _sites.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _batches = MutableStateFlow<List<PurchaseBatch>>(emptyList())
    val batches: StateFlow<List<PurchaseBatch>> = _batches.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _sites.value = sdk.siteRepository.getAll()
                _products.value = sdk.productRepository.getAll()
                _customers.value = sdk.customerRepository.getAll()
                _batches.value = sdk.purchaseBatchRepository.getAll()
            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }

    fun loadProductsForSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _products.value = sdk.productRepository.getBySite(siteId)
                _batches.value = sdk.purchaseBatchRepository.getBySite(siteId)
            } catch (e: Exception) {
                println("Error loading products for site: ${e.message}")
            }
        }
    }

    /**
     * Get available stock for a product at a site
     */
    fun getAvailableStock(productId: String, siteId: String): Double {
        return _batches.value
            .filter { it.productId == productId && it.siteId == siteId && !it.isExhausted }
            .sumOf { it.remainingQuantity }
    }

    /**
     * Execute a sale using the shared UseCase
     */
    fun executeSale(
        siteId: String,
        customerName: String,
        customerId: String?,
        items: List<SaleItemInput>,
        userId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SaleUiState.Loading

            val input = SaleInput(
                siteId = siteId,
                customerName = customerName,
                customerId = customerId,
                items = items,
                userId = userId
            )

            when (val result = sdk.saleUseCase.execute(input)) {
                is UseCaseResult.Success -> {
                    _uiState.value = SaleUiState.Success(result.data, result.warnings)
                    // Reload batches to reflect updated stock
                    loadData()
                }
                is UseCaseResult.Error -> {
                    _uiState.value = SaleUiState.Error(result.error)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = SaleUiState.Idle
    }
}
