package com.marketwear.mobile.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.WatchConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MobileViewModel(
    application: Application,
    private val repository: MobileMarketRepository,
    private val syncManager: WearDataSyncManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        buildUiState(
            quotes = emptyList(),
            weightMode = PortfolioWeightMode.NORMALIZED_100,
            refreshState = RefreshState.IDLE,
            watchConnectionState = WatchConnectionState.DISCONNECTED,
            lastSyncedTimestamp = 0L,
            errorMessage = null
        )
    )
    val uiState: StateFlow<MobileMarketUiState> = _uiState.asStateFlow()

    init {
        refreshAndSync()
        checkWatchConnection()
    }

    fun togglePortfolioWeightMode() {
        _uiState.update { current ->
            val nextMode = current.portfolioState.weightMode.next()
            buildUiState(
                quotes = current.quotes,
                weightMode = nextMode,
                refreshState = current.refreshState,
                watchConnectionState = current.watchConnectionState,
                lastSyncedTimestamp = current.lastSyncedTimestamp,
                errorMessage = current.errorMessage
            )
        }
    }

    fun checkWatchConnection() {
        viewModelScope.launch {
            val connectionState = if (syncManager.isWatchConnected()) {
                WatchConnectionState.CONNECTED
            } else {
                WatchConnectionState.DISCONNECTED
            }
            _uiState.update { it.copy(watchConnectionState = connectionState) }
        }
    }

    fun refreshAndSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshState = RefreshState.REFRESHING, errorMessage = null) }
            try {
                val quotes = repository.fetchAllQuotes()
                try {
                    MarketGlanceWidget().updateAll(getApplication())
                } catch (_: Exception) {
                    // Ignore widget update errors if no widgets placed
                }
                val syncSuccess = syncManager.syncQuotesToWatch(quotes)
                val connectionState = if (syncManager.isWatchConnected()) {
                    WatchConnectionState.CONNECTED
                } else {
                    WatchConnectionState.DISCONNECTED
                }
                val current = _uiState.value
                val newTimestamp = if (syncSuccess) System.currentTimeMillis() else current.lastSyncedTimestamp

                _uiState.value = buildUiState(
                    quotes = quotes,
                    weightMode = current.portfolioState.weightMode,
                    refreshState = RefreshState.IDLE,
                    watchConnectionState = connectionState,
                    lastSyncedTimestamp = newTimestamp,
                    errorMessage = if (!syncSuccess) "Watch sync failed (watch may be offline)" else null
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        refreshState = RefreshState.ERROR,
                        errorMessage = e.message ?: "Failed to fetch market quotes"
                    )
                }
            }
        }
    }

    internal companion object {
        fun buildUiState(
            quotes: List<IndexQuote>,
            weightMode: PortfolioWeightMode,
            refreshState: RefreshState,
            watchConnectionState: WatchConnectionState,
            lastSyncedTimestamp: Long,
            errorMessage: String?
        ): MobileMarketUiState {
            val dashboard = MarketUiStateBuilder.buildDashboardState(
                quotes = quotes,
                weightMode = weightMode,
                fallbackTimestampMillis = lastSyncedTimestamp
            )
            val formattedTime = if (lastSyncedTimestamp > 0L) {
                SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(lastSyncedTimestamp))
            } else {
                "Not synced"
            }
            return MobileMarketUiState(
                quotes = quotes,
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                refreshState = refreshState,
                watchConnectionState = watchConnectionState,
                lastSyncedTimestamp = lastSyncedTimestamp,
                formattedLastSynced = formattedTime,
                errorMessage = errorMessage
            )
        }
    }
}
