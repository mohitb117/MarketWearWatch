package com.marketwear.indices.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class MarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MarketRepository.getInstance(application)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MarketUiState> = combine(
        repository.quotesFlow,
        _isRefreshing
    ) { quotes, isRefreshing ->
        val latestUpdate = quotes.maxOfOrNull { it.lastUpdated } ?: 0L
        MarketUiState(
            quotes = quotes,
            isRefreshing = isRefreshing,
            lastUpdated = latestUpdate
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

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshWithPhoneFallback(force = force)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
