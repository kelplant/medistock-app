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

    suspend fun getById(id: Long): PackagingType? {
        return packagingTypeDao.getByIdSync(id)
    }

    suspend fun insert(packagingType: PackagingType): Long {
        return packagingTypeDao.insert(packagingType)
    }

    suspend fun update(packagingType: PackagingType) {
        packagingTypeDao.update(packagingType)
    }

    suspend fun delete(packagingType: PackagingType) {
        packagingTypeDao.delete(packagingType)
    }

    suspend fun deactivate(id: Long) {
        packagingTypeDao.deactivate(id)
    }

    suspend fun activate(id: Long) {
        packagingTypeDao.activate(id)
    }

    suspend fun isUsedByProducts(packagingTypeId: Long): Boolean {
        return packagingTypeDao.isUsedByProducts(packagingTypeId)
    }
}
