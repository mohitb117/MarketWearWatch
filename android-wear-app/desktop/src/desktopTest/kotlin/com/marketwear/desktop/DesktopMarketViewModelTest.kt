package com.marketwear.desktop

import com.marketwear.desktop.ui.DesktopMarketViewModel
import com.marketwear.desktop.ui.DesktopSimulationScenario
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.TrendDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMarketViewModelTest {

    @Test
    fun desktopViewModelTogglesWeightModesAndSwitchesScenarios() {
        val vm = DesktopMarketViewModel()

        // Default initial state
        val initial = vm.uiState.value
        assertEquals(DesktopSimulationScenario.DEFAULT_BASKET, initial.scenario)
        assertEquals(PortfolioWeightMode.NORMALIZED_100, initial.portfolioState.weightMode)
        assertEquals(RefreshState.IDLE, initial.refreshState)
        assertEquals(37, initial.topHoldings.size)
        assertEquals(2, initial.indices.size)
        assertEquals(11, initial.sectors.size)

        // Toggle weight mode to RAW_PORTFOLIO
        vm.togglePortfolioWeightMode()
        val rawState = vm.uiState.value
        assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, rawState.portfolioState.weightMode)
        assertTrue(rawState.portfolioState.mobileModePillLabel.contains("59.41%"))

        // Switch scenario to BULL_RALLY
        vm.selectScenario(DesktopSimulationScenario.BULL_RALLY)
        val bullState = vm.uiState.value
        assertEquals(DesktopSimulationScenario.BULL_RALLY, bullState.scenario)
        assertEquals(TrendDirection.POSITIVE, bullState.portfolioState.trendDirection)
        assertEquals(101.85, bullState.portfolioState.normalizedQuote.price, 0.01)

        // Switch scenario to BEAR_PULLBACK
        vm.selectScenario(DesktopSimulationScenario.BEAR_PULLBACK)
        val bearState = vm.uiState.value
        assertEquals(DesktopSimulationScenario.BEAR_PULLBACK, bearState.scenario)
        assertEquals(TrendDirection.NEGATIVE, bearState.portfolioState.trendDirection)
        assertEquals(98.66, bearState.portfolioState.normalizedQuote.price, 0.01)
    }
}
