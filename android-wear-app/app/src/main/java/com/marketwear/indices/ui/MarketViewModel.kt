package com.marketwear.indices.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class MarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val refreshState: RefreshState = RefreshState.IDLE,
    val lastUpdated: Long = 0L,
    val formattedLastUpdated: String = "--"
)

class MarketViewModel(
    application: Application,
    private val repository: MarketRepository
) : AndroidViewModel(application) {

    private val _refreshState = MutableStateFlow(RefreshState.IDLE)
    private val _weightMode = MutableStateFlow(PortfolioWeightMode.NORMALIZED_100)

    val uiState: StateFlow<MarketUiState> = combine(
        repository.quotesFlow,
        _refreshState,
        _weightMode
    ) { quotes, refreshState, weightMode ->
        val dashboard = MarketUiStateBuilder.buildDashboardState(
            quotes = quotes,
            weightMode = weightMode
        )
        val formattedTime = if (dashboard.latestUpdatedMillis > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dashboard.latestUpdatedMillis))
        } else {
            "--"
        }
        MarketUiState(
            quotes = quotes,
            portfolioState = dashboard.portfolioState,
            indices = dashboard.indices,
            sectors = dashboard.sectors,
            refreshState = refreshState,
            lastUpdated = dashboard.latestUpdatedMillis,
            formattedLastUpdated = formattedTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarketUiState()
    )

    init {
        // Initial silent load if Room DB has no data
        refresh(force = false)
    }

    fun togglePortfolioWeightMode() {
        _weightMode.update { it.next() }
    }

    fun setPortfolioWeightMode(mode: PortfolioWeightMode) {
        _weightMode.value = mode
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _refreshState.value = RefreshState.REFRESHING
            try {
                repository.refreshWithPhoneFallback(force = force)
                _refreshState.value = RefreshState.IDLE
            } catch (_: Exception) {
                _refreshState.value = RefreshState.ERROR
            }
        }
    }
}
