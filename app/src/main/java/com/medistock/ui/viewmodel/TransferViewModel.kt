package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.MedistockApplication
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.PurchaseBatch
import com.medistock.shared.domain.model.Site
import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.TransferResult
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for transfers using the shared TransferUseCase
 */
class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val sdk = MedistockApplication.sdk

    // UI State
    sealed class TransferUiState {
        object Idle : TransferUiState()
        object Loading : TransferUiState()
        data class Success(val result: TransferResult, val warnings: List<BusinessWarning>) : TransferUiState()
        data class Error(val error: BusinessError) : TransferUiState()
    }

    private val _uiState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val _sites = MutableStateFlow<List<Site>>(emptyList())
    val sites: StateFlow<List<Site>> = _sites.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

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
     * Execute a transfer using the shared UseCase
     */
    fun executeTransfer(
        productId: String,
        fromSiteId: String,
        toSiteId: String,
        quantity: Double,
        notes: String?,
        userId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = TransferUiState.Loading

            val input = TransferInput(
                productId = productId,
                fromSiteId = fromSiteId,
                toSiteId = toSiteId,
                quantity = quantity,
                notes = notes,
                userId = userId
            )

            when (val result = sdk.transferUseCase.execute(input)) {
                is UseCaseResult.Success -> {
                    _uiState.value = TransferUiState.Success(result.data, result.warnings)
                    // Reload data to reflect updated stock
                    loadData()
                }
                is UseCaseResult.Error -> {
                    _uiState.value = TransferUiState.Error(result.error)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = TransferUiState.Idle
    }
}
