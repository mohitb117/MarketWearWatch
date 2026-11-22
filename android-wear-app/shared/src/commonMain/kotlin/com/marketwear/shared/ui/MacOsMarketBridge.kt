package com.marketwear.shared.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.network.SharedMarketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface MacOsCancellable {
    fun cancel()
}

/**
 * Minimal KMP facade over [MarketStore] for native macOS SwiftUI (Dynamic Island, MenuBar, WidgetKit),
 * exposing existing shared MVI state and reducers directly with zero duplicate DTOs.
 */
class MacOsMarketBridge(
    private val client: SharedMarketClient = SharedMarketClient(),
    initialQuotes: List<IndexQuote> = MarketRegistry.getFallbackQuotes(),
    observerContext: CoroutineContext = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + observerContext)

    private val store: MarketStore = run {
        isAssertOnMainThreadEnabled = false
        MarketStoreFactory(DefaultStoreFactory(), Dispatchers.Default).create(
            name = "MacOsMarketStore",
            initialQuotes = initialQuotes,
            onRecomputeLocalQuotes = { existing, disabledIds, customWeights ->
                val recomputed = client.recomputePortfolioQuote(disabledIds, customWeights)
                if (recomputed != null) {
                    listOf(recomputed) + existing.filterNot { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }
                } else {
                    existing
                }
            },
            onFetchQuotes = { policy, disabledIds, customWeights ->
                client.fetchAllQuotes(policy, disabledIds, customWeights)
            }
        )
    }

    val currentState: MarketStore.State
        get() = store.state

    fun observeState(onEach: (MarketStore.State) -> Unit): MacOsCancellable {
        val job = scope.launch {
            store.states.collect { state -> onEach(state) }
        }
        return object : MacOsCancellable {
            override fun cancel() {
                job.cancel()
            }
        }
    }

    fun dispatch(intent: MarketIntent) {
        store.accept(MarketStore.Intent.User(intent))
    }

    fun toggleWeightMode() = dispatch(MarketIntent.ToggleWeightMode)

    fun applyPreset(preset: HoldingSelectionPreset) =
        dispatch(MarketIntent.ApplyHoldingPreset(preset))

    fun setHoldingWeight(holdingId: String, weightPercent: Double) =
        dispatch(MarketIntent.SetHoldingWeight(holdingId, weightPercent))

    fun resetWeights(action: WeightResetAction) =
        dispatch(MarketIntent.ResetWeights(action))

    fun requestRefresh(policy: RefreshPolicy) =
        dispatch(MarketIntent.RequestRefresh(policy))

    fun exportDashboardJson(): String =
        MarketUiStateBuilder.encodeDashboardJson(currentState.dashboard)

    fun handleActionCode(code: String) {
        parseActionCode(code)?.let(::dispatch)
    }

    fun dispose() {
        scope.cancel()
        store.dispose()
    }

    companion object {
        fun parseActionCode(code: String): MarketIntent? = when {
            code == "TOGGLE_MODE" -> MarketIntent.ToggleWeightMode
            code == "PRESET_ALL_37" -> MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)
            code == "PRESET_TOP_10" -> MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)
            code == "RESET_WEIGHTS" -> MarketIntent.ResetWeights(WeightResetAction.RESET_ALL_TO_DEFAULTS)
            code == "REFRESH" -> MarketIntent.RequestRefresh(RefreshPolicy.FORCE_CORE_QUOTES)
            code.startsWith("SET_WEIGHT:") -> {
                val parts = code.split(':')
                val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
                val pct = parts.getOrNull(2)?.toDoubleOrNull() ?: return null
                MarketIntent.SetHoldingWeight(id, pct)
            }
            else -> null
        }
    }
}
