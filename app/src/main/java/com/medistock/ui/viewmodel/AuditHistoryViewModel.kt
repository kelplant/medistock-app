package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.MedistockApplication
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.AuditHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuditHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val sdk: MedistockSDK = MedistockApplication.sdk
    private val auditRepo = sdk.auditRepository

    private val _auditEntries = MutableStateFlow<List<AuditHistory>>(emptyList())
    val auditEntries: StateFlow<List<AuditHistory>> = _auditEntries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _totalCount = MutableStateFlow(0L)
    val totalCount: StateFlow<Long> = _totalCount

    init {
        loadAllEntries()
        loadCount()
    }

    fun loadAllEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                auditRepo.observeAllHistory().collect { entries ->
                    _auditEntries.value = entries
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByEntityType(entityType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditRepo.getHistoryByEntityType(entityType)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByUser(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditRepo.getHistoryByUser(username)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByDateRange(startTime: Long, endTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditRepo.getHistoryByDateRange(startTime, endTime)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _totalCount.value = auditRepo.getHistoryCount()
        }
    }
}
