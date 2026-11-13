package com.marketwear.desktop.ui

import androidx.compose.runtime.Immutable
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioHolding
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.createPlatformHttpClient
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extensible simulation scenarios for testing Portfolio Daily Score (100-baseline) locally on KMP Desktop.
 */
enum class DesktopSimulationScenario(val label: String) {
    DEFAULT_BASKET("Default Intraday (+0.78%)"),
    BULL_RALLY("Bull Rally (+1.85%)"),
    BEAR_PULLBACK("Bear Pullback (-1.34%)"),
    LIVE_NETWORK("Live Yahoo Finance (2h Throttle)")
}

@Immutable
data class DesktopMarketUiState(
    val scenario: DesktopSimulationScenario = DesktopSimulationScenario.DEFAULT_BASKET,
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val topHoldings: List<PortfolioHolding> = PortfolioRegistry.HOLDINGS,
    val refreshState: RefreshState = RefreshState.IDLE,
    val formattedLastUpdated: String = "--",
    val errorMessage: String? = null
)

class DesktopMarketViewModel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val marketClient: SharedMarketClient = SharedMarketClient(createPlatformHttpClient())
) {
    private val _scenario = MutableStateFlow(DesktopSimulationScenario.DEFAULT_BASKET)
    private val _weightMode = MutableStateFlow(PortfolioWeightMode.NORMALIZED_100)
    private val _refreshState = MutableStateFlow(RefreshState.IDLE)

    private val _uiState = MutableStateFlow(
        buildUiState(
            scenario = DesktopSimulationScenario.DEFAULT_BASKET,
            quotes = quotesForScenario(DesktopSimulationScenario.DEFAULT_BASKET),
            weightMode = PortfolioWeightMode.NORMALIZED_100,
            refreshState = RefreshState.IDLE,
            errorMessage = null
        )
    )
    val uiState: StateFlow<DesktopMarketUiState> = _uiState.asStateFlow()

    fun togglePortfolioWeightMode() {
        _weightMode.update { it.next() }
        val nextMode = _weightMode.value
        _uiState.update { current ->
            buildUiState(
                scenario = current.scenario,
                quotes = current.quotes,
                weightMode = nextMode,
                refreshState = current.refreshState,
                errorMessage = current.errorMessage
            )
        }
    }

    fun setPortfolioWeightMode(mode: PortfolioWeightMode) {
        _weightMode.value = mode
        _uiState.update { current ->
            buildUiState(
                scenario = current.scenario,
                quotes = current.quotes,
                weightMode = mode,
                refreshState = current.refreshState,
                errorMessage = current.errorMessage
            )
        }
    }

    fun selectScenario(scenario: DesktopSimulationScenario) {
        _scenario.value = scenario
        if (scenario == DesktopSimulationScenario.LIVE_NETWORK) {
            refreshLiveNetworkQuotes()
        } else {
            val scenarioQuotes = quotesForScenario(scenario)
            _refreshState.value = RefreshState.IDLE
            _uiState.update { current ->
                buildUiState(
                    scenario = scenario,
                    quotes = scenarioQuotes,
                    weightMode = current.portfolioState.weightMode,
                    refreshState = RefreshState.IDLE,
                    errorMessage = null
                )
            }
        }
    }

    fun refreshLiveNetworkQuotes() {
        scope.launch {
            _refreshState.value = RefreshState.REFRESHING
            _uiState.update { it.copy(refreshState = RefreshState.REFRESHING, errorMessage = null) }
            try {
                val fetched = marketClient.fetchAllQuotes()
                val combined = fetched.ifEmpty { MarketRegistry.getFallbackQuotes() }
                _refreshState.value = RefreshState.IDLE
                _uiState.update { current ->
                    buildUiState(
                        scenario = DesktopSimulationScenario.LIVE_NETWORK,
                        quotes = combined,
                        weightMode = current.portfolioState.weightMode,
                        refreshState = RefreshState.IDLE,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _refreshState.value = RefreshState.ERROR
                _uiState.update { current ->
                    current.copy(
                        refreshState = RefreshState.ERROR,
                        errorMessage = e.message ?: "Live network fetch failed"
                    )
                }
            }
        }
    }

    companion object {
        fun quotesForScenario(
            scenario: DesktopSimulationScenario,
            nowMillis: Long = 1763056800000L
        ): List<IndexQuote> {
            val baseQuotes = MarketRegistry.getFallbackQuotes()
            return when (scenario) {
                DesktopSimulationScenario.DEFAULT_BASKET,
                DesktopSimulationScenario.LIVE_NETWORK -> baseQuotes

                DesktopSimulationScenario.BULL_RALLY -> {
                    val bullSeries = listOf(100.0, 100.28, 100.62, 100.95, 101.24, 101.58, 101.72, 101.85)
                    val bullData = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
                        TickerIntradayData(
                            yahooTicker = ticker,
                            previousClose = 100.0,
                            currentPrice = 101.85,
                            intradayCloses = bullSeries
                        )
                    }
                    val bullPortfolio = PortfolioRegistry.computePortfolioQuote(
                        dataByYahooTicker = bullData,
                        timestampMillis = nowMillis
                    )
                    listOf(bullPortfolio) + baseQuotes.filter { !it.isPortfolio }
                }

                DesktopSimulationScenario.BEAR_PULLBACK -> {
                    val bearSeries = listOf(100.0, 99.82, 99.54, 99.21, 98.96, 98.78, 98.55, 98.66)
                    val bearData = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
                        TickerIntradayData(
                            yahooTicker = ticker,
                            previousClose = 100.0,
                            currentPrice = 98.66,
                            intradayCloses = bearSeries
                        )
                    }
                    val bearPortfolio = PortfolioRegistry.computePortfolioQuote(
                        dataByYahooTicker = bearData,
                        timestampMillis = nowMillis
                    )
                    listOf(bearPortfolio) + baseQuotes.filter { !it.isPortfolio }
                }
            }
        }

        fun buildUiState(
            scenario: DesktopSimulationScenario,
            quotes: List<IndexQuote>,
            weightMode: PortfolioWeightMode,
            refreshState: RefreshState,
            errorMessage: String?
        ): DesktopMarketUiState {
            val dashboard = MarketUiStateBuilder.buildDashboardState(
                quotes = quotes,
                weightMode = weightMode
            )
            val formattedTime = if (dashboard.latestUpdatedMillis > 0L) {
                SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(dashboard.latestUpdatedMillis))
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
                refreshState = refreshState,
                formattedLastUpdated = formattedTime,
                errorMessage = errorMessage
            )
        }
    }
}
