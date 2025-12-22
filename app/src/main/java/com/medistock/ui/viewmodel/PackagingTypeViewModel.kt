package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.PackagingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PackagingTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val packagingTypeDao = db.packagingTypeDao()

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
            packagingTypeDao.getAll().collect { types ->
                _packagingTypes.value = types
            }
        }
    }

    fun loadActivePackagingTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.getAllActive().collect { types ->
                _activePackagingTypes.value = types
            }
        }
    }

    fun getById(id: String, callback: (PackagingType?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = packagingTypeDao.getByIdSync(id)
            callback(result)
        }
    }

    fun insert(packagingType: PackagingType, callback: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.insert(packagingType)
            callback(packagingType.id)
        }
    }

    fun update(packagingType: PackagingType, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.update(packagingType)
            callback()
        }
    }

    fun delete(packagingType: PackagingType, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.delete(packagingType)
            callback()
        }
    }

    fun deactivate(id: String, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.deactivate(id)
            callback()
        }
    }

    fun activate(id: String, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            packagingTypeDao.activate(id)
            callback()
        }
    }

    fun isUsedByProducts(packagingTypeId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = packagingTypeDao.isUsedByProducts(packagingTypeId)
            callback(result)
        }
    }
}
