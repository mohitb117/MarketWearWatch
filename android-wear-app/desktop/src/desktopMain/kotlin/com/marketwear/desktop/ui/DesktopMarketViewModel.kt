package com.marketwear.desktop.ui

import androidx.compose.runtime.Immutable
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioHolding
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.createPlatformHttpClient
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketStateReducer
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.WeightResetAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * Extensible simulation scenarios for testing Portfolio Daily Score (100-baseline) locally on KMP Desktop.
 */
enum class DesktopSimulationScenario(val label: String) {
    DEFAULT_BASKET("Default Intraday (+0.78%)"),
    BULL_RALLY("Bull Rally (+1.85%)"),
    BEAR_PULLBACK("Bear Pullback (-1.34%)"),
    LIVE_NETWORK("Live Yahoo Finance (2h Throttle)")
}

/**
 * Desktop-specific MVI intents wrapping shared [MarketIntent] actions and simulation scenarios.
 */
sealed interface DesktopMarketIntent {
    data class Core(val intent: MarketIntent) : DesktopMarketIntent
    data class SelectScenario(val scenario: DesktopSimulationScenario) : DesktopMarketIntent
}

@Immutable
data class DesktopMarketUiState(
    val scenario: DesktopSimulationScenario = DesktopSimulationScenario.DEFAULT_BASKET,
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val topHoldings: List<PortfolioHolding> = PortfolioRegistry.HOLDINGS,
    val disabledHoldingIds: Set<String> = emptySet(),
    val customWeights: Map<String, Double> = emptyMap(),
    val refreshState: RefreshState = RefreshState.IDLE,
    val formattedLastUpdated: String = "--",
    val errorMessage: String? = null
)

class DesktopMarketViewModel(
    private val marketClient: SharedMarketClient = SharedMarketClient(createPlatformHttpClient()),
    private val mainContext: CoroutineContext = Dispatchers.Unconfined,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    storeFactory: StoreFactory = DefaultStoreFactory().also { isAssertOnMainThreadEnabled = false }
) {
    private sealed interface Message {
        data class QuotesChanged(
            val scenario: DesktopSimulationScenario,
            val quotes: List<IndexQuote>,
            val disabledHoldingIds: Set<String>,
            val customWeights: Map<String, Double>
        ) : Message

        data class WeightModeChanged(val mode: PortfolioWeightMode) : Message
        data class RefreshStarted(val scenario: DesktopSimulationScenario) : Message
        data class RefreshFailed(val errorMessage: String) : Message
    }

    private val viewModelScope = CoroutineScope(SupervisorJob() + mainContext)

    private val store: Store<DesktopMarketIntent, DesktopMarketUiState, Nothing> =
        storeFactory.create(
            name = "DesktopMarketStore",
            initialState = buildUiState(
                scenario = DesktopSimulationScenario.DEFAULT_BASKET,
                quotes = quotesForScenario(DesktopSimulationScenario.DEFAULT_BASKET),
                weightMode = PortfolioWeightMode.NORMALIZED_100,
                refreshState = RefreshState.IDLE,
                errorMessage = null
            ),
            executorFactory = { DesktopExecutor() },
            reducer = DesktopReducer
        )

    val uiState: StateFlow<DesktopMarketUiState> = store.states.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = store.state
    )

    fun onIntent(intent: DesktopMarketIntent) {
        store.accept(intent)
    }

    fun dispose() {
        store.dispose()
        viewModelScope.cancel()
    }

    private inner class DesktopExecutor :
        CoroutineExecutor<DesktopMarketIntent, Nothing, DesktopMarketUiState, Message, Nothing>(
            mainContext = mainContext
        ) {
        override fun executeIntent(intent: DesktopMarketIntent) {
            when (intent) {
                is DesktopMarketIntent.Core -> handleCoreIntent(intent.intent)
                is DesktopMarketIntent.SelectScenario -> handleScenarioSelection(intent.scenario)
            }
        }

        private fun handleCoreIntent(intent: MarketIntent) {
            val current = state()
            when (intent) {
                MarketIntent.ToggleWeightMode ->
                    dispatch(Message.WeightModeChanged(current.portfolioState.weightMode.next()))

                is MarketIntent.SetWeightMode ->
                    dispatch(Message.WeightModeChanged(intent.mode))

                is MarketIntent.ToggleHolding -> {
                    val nextDisabled = MarketStateReducer.reduceHoldingToggle(
                        current.disabledHoldingIds,
                        intent.holdingId
                    )
                    applyPortfolioConfig(nextDisabled, current.customWeights, current)
                }

                is MarketIntent.ApplyHoldingPreset -> {
                    val nextDisabled = MarketStateReducer.reduceHoldingPreset(intent.preset)
                    applyPortfolioConfig(nextDisabled, current.customWeights, current)
                }

                is MarketIntent.SetHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceSetHoldingWeight(
                        current.customWeights,
                        intent.holdingId,
                        intent.weightPercent
                    )
                    applyPortfolioConfig(current.disabledHoldingIds, nextWeights, current)
                }

                is MarketIntent.StepHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceStepHoldingWeight(
                        current.customWeights,
                        intent.holdingId,
                        intent.step
                    )
                    applyPortfolioConfig(current.disabledHoldingIds, nextWeights, current)
                }

                is MarketIntent.ResetWeights -> when (intent.action) {
                    WeightResetAction.RESTORE_DEFAULT_WEIGHTS ->
                        applyPortfolioConfig(current.disabledHoldingIds, emptyMap(), current)
                    WeightResetAction.RESET_ALL_TO_DEFAULTS ->
                        applyPortfolioConfig(emptySet(), emptyMap(), current)
                }

                is MarketIntent.RequestRefresh -> launchLiveNetworkRefresh(intent.policy)
            }
        }

        private fun applyPortfolioConfig(
            nextDisabled: Set<String>,
            nextWeights: Map<String, Double>,
            current: DesktopMarketUiState
        ) {
            val updatedQuotes = recomputeQuotesForScenarioOrLive(
                scenario = current.scenario,
                existingQuotes = current.quotes,
                disabledHoldingIds = nextDisabled,
                customWeights = nextWeights
            )
            dispatch(Message.QuotesChanged(current.scenario, updatedQuotes, nextDisabled, nextWeights))
        }

        private fun handleScenarioSelection(scenario: DesktopSimulationScenario) {
            if (scenario == DesktopSimulationScenario.LIVE_NETWORK) {
                launchLiveNetworkRefresh(RefreshPolicy.CACHE_FIRST)
            } else {
                val current = state()
                val scenarioQuotes = quotesForScenario(
                    scenario = scenario,
                    disabledHoldingIds = current.disabledHoldingIds,
                    customWeights = current.customWeights
                )
                dispatch(
                    Message.QuotesChanged(
                        scenario = scenario,
                        quotes = scenarioQuotes,
                        disabledHoldingIds = current.disabledHoldingIds,
                        customWeights = current.customWeights
                    )
                )
            }
        }

        private fun launchLiveNetworkRefresh(policy: RefreshPolicy) {
            dispatch(Message.RefreshStarted(DesktopSimulationScenario.LIVE_NETWORK))
            scope.launch {
                try {
                    val initialSnapshot = state()
                    val requestedDisabled = initialSnapshot.disabledHoldingIds
                    val requestedWeights = initialSnapshot.customWeights
                    val fetched = withContext(ioContext) {
                        marketClient.fetchAllQuotes(
                            policy = policy,
                            disabledHoldingIds = requestedDisabled,
                            customWeights = requestedWeights
                        )
                    }
                    val combined = fetched.ifEmpty {
                        quotesForScenario(
                            scenario = DesktopSimulationScenario.DEFAULT_BASKET,
                            disabledHoldingIds = requestedDisabled,
                            customWeights = requestedWeights
                        )
                    }
                    val latest = state()
                    val finalQuotes = if (latest.disabledHoldingIds != requestedDisabled || latest.customWeights != requestedWeights) {
                        recomputeQuotesForScenarioOrLive(
                            scenario = DesktopSimulationScenario.LIVE_NETWORK,
                            existingQuotes = combined,
                            disabledHoldingIds = latest.disabledHoldingIds,
                            customWeights = latest.customWeights
                        )
                    } else {
                        combined
                    }
                    dispatch(
                        Message.QuotesChanged(
                            scenario = DesktopSimulationScenario.LIVE_NETWORK,
                            quotes = finalQuotes,
                            disabledHoldingIds = latest.disabledHoldingIds,
                            customWeights = latest.customWeights
                        )
                    )
                } catch (e: Exception) {
                    dispatch(Message.RefreshFailed(e.message ?: "Live network fetch failed"))
                }
            }
        }
    }

    private fun recomputeQuotesForScenarioOrLive(
        scenario: DesktopSimulationScenario,
        existingQuotes: List<IndexQuote>,
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double>
    ): List<IndexQuote> {
        if (scenario == DesktopSimulationScenario.LIVE_NETWORK) {
            val recomputedLive = marketClient.recomputePortfolioQuote(disabledHoldingIds, customWeights)
            if (recomputedLive != null) {
                return listOf(recomputedLive) + existingQuotes.filter { !it.isPortfolio }
            }
        }
        return quotesForScenario(
            scenario = scenario,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
    }

    private object DesktopReducer : Reducer<DesktopMarketUiState, Message> {
        override fun DesktopMarketUiState.reduce(msg: Message): DesktopMarketUiState = when (msg) {
            is Message.QuotesChanged -> buildUiState(
                scenario = msg.scenario,
                quotes = msg.quotes,
                weightMode = portfolioState.weightMode,
                refreshState = RefreshState.IDLE,
                errorMessage = null,
                disabledHoldingIds = msg.disabledHoldingIds,
                customWeights = msg.customWeights
            )

            is Message.WeightModeChanged -> buildUiState(
                scenario = scenario,
                quotes = quotes,
                weightMode = msg.mode,
                refreshState = refreshState,
                errorMessage = errorMessage,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            )

            is Message.RefreshStarted -> copy(
                scenario = msg.scenario,
                refreshState = RefreshState.REFRESHING,
                errorMessage = null
            )

            is Message.RefreshFailed -> copy(
                refreshState = RefreshState.ERROR,
                errorMessage = msg.errorMessage
            )
        }
    }

    companion object {
        private val DESKTOP_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())

        fun quotesForScenario(
            scenario: DesktopSimulationScenario,
            nowMillis: Long = 1763056800000L,
            disabledHoldingIds: Set<String> = emptySet(),
            customWeights: Map<String, Double> = emptyMap()
        ): List<IndexQuote> {
            val baseQuotes = MarketRegistry.getFallbackQuotes()
            val syntheticSeries = when (scenario) {
                DesktopSimulationScenario.BULL_RALLY ->
                    listOf(100.0, 100.28, 100.62, 100.95, 101.24, 101.58, 101.72, 101.85)
                DesktopSimulationScenario.BEAR_PULLBACK ->
                    listOf(100.0, 99.82, 99.54, 99.21, 98.96, 98.78, 98.55, 98.66)
                DesktopSimulationScenario.DEFAULT_BASKET,
                DesktopSimulationScenario.LIVE_NETWORK -> {
                    val basePortfolio = baseQuotes.firstOrNull { it.isPortfolio }
                    if (disabledHoldingIds.isEmpty() && customWeights.isEmpty()) return baseQuotes
                    basePortfolio?.sparkline ?: listOf(100.0, 100.32, 100.54, 100.84)
                }
            }

            // Generate subtle varied per-ticker intraday returns (weighted average = 1.0 across all 37)
            // so full-basket scenario targets remain exact while toggling/re-weighting changes the score.
            val rawMultipliers = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.mapIndexed { idx, ticker ->
                ticker to (1.0 + (((idx % 5) - 2) * 0.15))
            }.toMap()
            val weightedAvgMultiplier = PortfolioRegistry.HOLDINGS.sumOf { holding ->
                holding.weightPercent * (rawMultipliers[holding.yahooTicker] ?: 1.0)
            } / PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT

            val tickerData = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
                val spreadMultiplier = (rawMultipliers[ticker] ?: 1.0) / weightedAvgMultiplier
                val scaledSeries = syntheticSeries.map { pt ->
                    val pctMove = pt - PortfolioRegistry.BASELINE_SCORE
                    PortfolioRegistry.BASELINE_SCORE + (pctMove * spreadMultiplier)
                }
                TickerIntradayData(
                    yahooTicker = ticker,
                    previousClose = PortfolioRegistry.BASELINE_SCORE,
                    currentPrice = scaledSeries.last(),
                    intradayCloses = scaledSeries
                )
            }

            val portfolio = PortfolioRegistry.computePortfolioQuote(
                dataByYahooTicker = tickerData,
                timestampMillis = nowMillis,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            )
            return listOf(portfolio) + baseQuotes.filter { !it.isPortfolio }
        }

        fun buildUiState(
            scenario: DesktopSimulationScenario,
            quotes: List<IndexQuote>,
            weightMode: PortfolioWeightMode,
            refreshState: RefreshState,
            errorMessage: String?,
            disabledHoldingIds: Set<String> = emptySet(),
            customWeights: Map<String, Double> = emptyMap()
        ): DesktopMarketUiState {
            val dashboard = MarketUiStateBuilder.buildDashboardState(
                quotes = quotes,
                weightMode = weightMode,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            )
            val formattedTime = if (dashboard.latestUpdatedMillis > 0L) {
                Instant.ofEpochMilli(dashboard.latestUpdatedMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DESKTOP_TIME_FORMATTER)
            } else {
                "--"
            }
            return DesktopMarketUiState(
                scenario = scenario,
                quotes = quotes,
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                topHoldings = PortfolioRegistry.HOLDINGS,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights,
                refreshState = refreshState,
                formattedLastUpdated = formattedTime,
                errorMessage = errorMessage
            )
        }
    }
}
