package com.marketwear.indices.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.WatchPortfolioSelectionStore
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketStore
import com.marketwear.shared.ui.MarketStoreFactory
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class MarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val disabledHoldingIds: Set<String> = emptySet(),
    val customWeights: Map<String, Double> = emptyMap(),
    val refreshState: RefreshState = RefreshState.IDLE,
    val lastUpdated: Long = 0L,
    val formattedLastUpdated: String = "--"
)

class MarketViewModel(
    application: Application,
    private val repository: MarketRepository,
    private val selectionStore: WatchPortfolioSelectionStore = WatchPortfolioSelectionStore(application),
    private val liveNotificationManager: WatchLiveNotificationManager = WatchLiveNotificationManager(application),
    storeFactory: StoreFactory = DefaultStoreFactory()
) : AndroidViewModel(application) {

    private val store: MarketStore = MarketStoreFactory(
        storeFactory = storeFactory
    ).create(
        name = "WearMarketStore",
        initialDisabledHoldingIds = selectionStore.disabledHoldingIds.value,
        initialCustomWeights = selectionStore.customWeights.value,
        initialWeightMode = selectionStore.weightMode.value,
        onRecomputeLocalQuotes = { existingQuotes, disabledIds, customWeights ->
            if (existingQuotes.any { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL } &&
                disabledIds.isEmpty() && customWeights.isEmpty()) {
                existingQuotes
            } else {
                recomputeWithCachedIntraday(existingQuotes, disabledIds, customWeights)
            }
        },
        onPersistPortfolioConfig = { disabledIds, customWeights, updatedQuotes ->
            selectionStore.setDisabledHoldings(disabledIds)
            selectionStore.setCustomWeights(customWeights)
            val portfolioQuote = updatedQuotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }
            if (portfolioQuote != null && selectionStore.cachedTickerIntradayData.value.isNotEmpty()) {
                repository.persistRecomputedPortfolioQuote(portfolioQuote)
            }
            repository.syncDisabledHoldingsToPhone(disabledIds, customWeights)
        },
        onFetchQuotes = { policy, _, _ ->
            repository.refreshWithPhoneFallback(policy = policy)
            emptyList()
        }
    )

    val uiState: StateFlow<MarketUiState> = store.states
        .map { it.toWearUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = store.state.toWearUiState()
        )

    init {
        viewModelScope.launch {
            repository.quotesFlow.collect { quotes ->
                store.accept(MarketStore.Intent.ExternalQuotesUpdated(quotes))
            }
        }
        viewModelScope.launch {
            selectionStore.disabledHoldingIds.drop(1).collect { disabledIds ->
                if (disabledIds != store.state.disabledHoldingIds) {
                    store.accept(MarketStore.Intent.ExternalDisabledHoldingsUpdated(disabledIds))
                }
            }
        }
        viewModelScope.launch {
            selectionStore.customWeights.drop(1).collect { customWeights ->
                if (customWeights != store.state.customWeights) {
                    store.accept(MarketStore.Intent.ExternalCustomWeightsUpdated(customWeights))
                }
            }
        }
        viewModelScope.launch {
            selectionStore.cachedTickerIntradayData.drop(1).collect { intraday ->
                if (intraday.isNotEmpty()) {
                    store.accept(MarketStore.Intent.ExternalQuotesUpdated(store.state.quotes))
                }
            }
        }
        viewModelScope.launch {
            store.states.collect { state ->
                selectionStore.setWeightMode(state.weightMode)
                if (state.quotes.isNotEmpty()) {
                    liveNotificationManager.showOrUpdateFromQuotes(
                        quotes = state.quotes,
                        weightMode = state.weightMode,
                        disabledHoldingIds = state.disabledHoldingIds,
                        customWeights = state.customWeights
                    )
                }
            }
        }
        // Initial silent load if Room DB cache is stale or empty
        onIntent(MarketIntent.RequestRefresh(RefreshPolicy.CACHE_FIRST))
    }

    fun onIntent(intent: MarketIntent) {
        store.accept(MarketStore.Intent.User(intent))
    }

    override fun onCleared() {
        store.dispose()
        super.onCleared()
    }

    private fun recomputeWithCachedIntraday(
        existingQuotes: List<IndexQuote>,
        disabledIds: Set<String>,
        customWeights: Map<String, Double>
    ): List<IndexQuote> {
        val intradayData = selectionStore.cachedTickerIntradayData.value
        if (intradayData.isEmpty()) return existingQuotes
        val latestTs = existingQuotes.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
        val recomputedPortfolio = PortfolioRegistry.computePortfolioQuote(
            dataByYahooTicker = intradayData,
            timestampMillis = latestTs,
            disabledHoldingIds = disabledIds,
            customWeights = customWeights
        )
        return listOf(recomputedPortfolio) + existingQuotes.filter { it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL }
    }

    private fun MarketStore.State.toWearUiState(): MarketUiState {
        val formattedTime = if (dashboard.latestUpdatedMillis > 0L) {
            Instant.ofEpochMilli(dashboard.latestUpdatedMillis)
                .atZone(ZoneId.systemDefault())
                .format(WATCH_TIME_FORMATTER)
        } else {
            "--"
        }
        return MarketUiState(
            quotes = quotes,
            portfolioState = dashboard.portfolioState,
            indices = dashboard.indices,
            sectors = dashboard.sectors,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights,
            refreshState = refreshState,
            lastUpdated = dashboard.latestUpdatedMillis,
            formattedLastUpdated = formattedTime
        )
    }

    companion object {
        private val WATCH_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }
}
