package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.MedistockApplication
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.usecase.PurchaseInput
import com.medistock.shared.domain.usecase.PurchaseResult
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for purchases using the shared PurchaseUseCase
 */
class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val sdk = MedistockApplication.sdk

    // UI State
    sealed class PurchaseUiState {
        object Idle : PurchaseUiState()
        object Loading : PurchaseUiState()
        data class Success(val result: PurchaseResult, val warnings: List<BusinessWarning>) : PurchaseUiState()
        data class Error(val error: BusinessError) : PurchaseUiState()
    }

    private val _uiState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private val _sites = MutableStateFlow<List<Site>>(emptyList())
    val sites: StateFlow<List<Site>> = _sites.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _sites.value = sdk.siteRepository.getAll()
                _products.value = sdk.productRepository.getAll()
            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }

    fun loadProductsForSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _products.value = sdk.productRepository.getBySite(siteId)
            } catch (e: Exception) {
                println("Error loading products for site: ${e.message}")
            }
        }
    }

    /**
     * Execute a purchase using the shared UseCase
     */
    fun executePurchase(
        productId: String,
        siteId: String,
        quantity: Double,
        purchasePrice: Double,
        supplierName: String,
        batchNumber: String?,
        expiryDate: Long?,
        userId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = PurchaseUiState.Loading

            val input = PurchaseInput(
                productId = productId,
                siteId = siteId,
                quantity = quantity,
                purchasePrice = purchasePrice,
                supplierName = supplierName,
                batchNumber = batchNumber,
                expiryDate = expiryDate,
                userId = userId
            )

            when (val result = sdk.purchaseUseCase.execute(input)) {
                is UseCaseResult.Success -> {
                    _uiState.value = PurchaseUiState.Success(result.data, result.warnings)
                }
                is UseCaseResult.Error -> {
                    _uiState.value = PurchaseUiState.Error(result.error)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = PurchaseUiState.Idle
    }
}
