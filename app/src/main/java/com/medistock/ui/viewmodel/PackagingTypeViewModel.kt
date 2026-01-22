package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.MedistockApplication
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.PackagingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class PackagingTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val sdk: MedistockSDK = MedistockApplication.sdk
    private val packagingTypeRepo = sdk.packagingTypeRepository

    private val _packagingTypes = MutableStateFlow<List<PackagingType>>(emptyList())
    val packagingTypes: StateFlow<List<PackagingType>> = _packagingTypes

    private val _activePackagingTypes = MutableStateFlow<List<PackagingType>>(emptyList())
    val activePackagingTypes: StateFlow<List<PackagingType>> = _activePackagingTypes

    init {
        loadPackagingTypes()
        loadActivePackagingTypes()
    }

    fun loadPackagingTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeRepo.observeAll().collect { types ->
                _packagingTypes.value = types
            }
        }
    }

    fun loadActivePackagingTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeRepo.observeActive().collect { types ->
                _activePackagingTypes.value = types
            }
        }
    }

    fun getById(id: String, callback: (PackagingType?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = packagingTypeRepo.getById(id)
            callback(result)
        }
    }

    fun insert(packagingType: PackagingType, callback: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeRepo.insert(packagingType)
            callback(packagingType.id)
        }
    }

    fun update(packagingType: PackagingType, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeRepo.update(packagingType)
            callback()
        }
    }

    fun delete(packagingType: PackagingType, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeRepo.delete(packagingType.id)
            callback()
        }
    }

    fun deactivate(id: String, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            packagingTypeRepo.setActive(id, false, now, "android")
            callback()
        }
    }

    fun activate(id: String, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            packagingTypeRepo.setActive(id, true, now, "android")
            callback()
        }
    }

    fun isUsedByProducts(packagingTypeId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = packagingTypeRepo.isUsedByProducts(packagingTypeId)
            callback(result)
        }
    }
}
