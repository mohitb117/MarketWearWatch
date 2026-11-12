package com.marketwear.mobile.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MobileMarketRepository.getInstance()
    private val syncManager = WearDataSyncManager(application)

    private val _uiState = MutableStateFlow(MobileMarketUiState())
    val uiState: StateFlow<MobileMarketUiState> = _uiState.asStateFlow()

    init {
        refreshAndSync()
        checkWatchConnection()
    }

    fun checkWatchConnection() {
        viewModelScope.launch {
            val isConnected = syncManager.isWatchConnected()
            _uiState.value = _uiState.value.copy(isWatchConnected = isConnected)
        }
    }

    fun refreshAndSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)
            try {
                val quotes = repository.fetchAllQuotes()
                try {
                    MarketGlanceWidget().updateAll(getApplication())
                } catch (e: Exception) {
                    // Ignore widget update errors if no widgets placed
                }
                val syncSuccess = syncManager.syncQuotesToWatch(quotes)
                val isWatchConnected = syncManager.isWatchConnected()

                _uiState.value = _uiState.value.copy(
                    quotes = quotes,
                    isSyncing = false,
                    isWatchConnected = isWatchConnected,
                    lastSyncedTimestamp = if (syncSuccess) System.currentTimeMillis() else _uiState.value.lastSyncedTimestamp,
                    errorMessage = if (!syncSuccess) "Watch sync failed (watch may be offline)" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Failed to fetch market quotes"
                )
            }
        }
    }
}
