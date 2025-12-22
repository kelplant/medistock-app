package com.medistock.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.AuditHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuditHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val auditHistoryDao = db.auditHistoryDao()

    private val _auditEntries = MutableStateFlow<List<AuditHistory>>(emptyList())
    val auditEntries: StateFlow<List<AuditHistory>> = _auditEntries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    init {
        loadAllEntries()
        loadCount()
    }

    fun loadAllEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getAll().first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByEntityType(entityType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getByEntityType(entityType).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByEntity(entityType: String, entityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getByEntity(entityType, entityId).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByUser(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getByUser(username).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBySite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getBySite(siteId).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadByDateRange(startTime: Long, endTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getByDateRange(startTime, endTime).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFiltered(
        entityType: String?,
        actionType: String?,
        username: String?,
        siteId: String?,
        startTime: Long?,
        endTime: Long?,
        limit: Int = 100,
        offset: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _auditEntries.value = auditHistoryDao.getFiltered(
                    entityType,
                    actionType,
                    username,
                    siteId,
                    startTime,
                    endTime,
                    limit,
                    offset
                ).first()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _totalCount.value = auditHistoryDao.getCount().first()
        }
    }
}
