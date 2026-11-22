package com.marketwear.desktop

import com.marketwear.desktop.ui.DesktopMarketIntent
import com.marketwear.desktop.ui.DesktopMarketViewModel
import com.marketwear.desktop.ui.DesktopSimulationScenario
import com.marketwear.shared.ui.HoldingSelectionPreset
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.shared.ui.WeightAdjustmentStep
import com.marketwear.shared.ui.WeightResetAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMarketViewModelTest {

    @Test
    fun desktopViewModelTogglesWeightModesAndSwitchesScenarios() {
        val vm = DesktopMarketViewModel()
        try {
            // Default initial state
            val initial = vm.uiState.value
            assertEquals(DesktopSimulationScenario.DEFAULT_BASKET, initial.scenario)
            assertEquals(PortfolioWeightMode.NORMALIZED_100, initial.portfolioState.weightMode)
            assertEquals(RefreshState.IDLE, initial.refreshState)
            assertEquals(37, initial.topHoldings.size)
            assertEquals(2, initial.indices.size)
            assertEquals(11, initial.sectors.size)

            // Toggle weight mode to RAW_PORTFOLIO via MVIKotlin MVI Intent
            vm.onIntent(DesktopMarketIntent.Core(MarketIntent.ToggleWeightMode))
            val rawState = vm.uiState.value
            assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, rawState.portfolioState.weightMode)
            assertTrue(rawState.portfolioState.mobileModePillLabel.contains("59.41%"))

            // Switch scenario to BULL_RALLY
            vm.onIntent(DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.BULL_RALLY))
            val bullState = vm.uiState.value
            assertEquals(DesktopSimulationScenario.BULL_RALLY, bullState.scenario)
            assertEquals(TrendDirection.POSITIVE, bullState.portfolioState.trendDirection)
            assertEquals(101.85, bullState.portfolioState.normalizedQuote.price, 0.01)
            assertTrue(bullState.portfolioState.liveUpdateSpec.progress > 500)
            assertEquals(1000, bullState.portfolioState.liveUpdateSpec.segments.sumOf { it.length })

            // Switch scenario to BEAR_PULLBACK and verify center-origin red gauge extending left
            vm.onIntent(DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.BEAR_PULLBACK))
            val bearState = vm.uiState.value
            assertEquals(DesktopSimulationScenario.BEAR_PULLBACK, bearState.scenario)
            assertEquals(TrendDirection.NEGATIVE, bearState.portfolioState.trendDirection)
            assertEquals(98.66, bearState.portfolioState.normalizedQuote.price, 0.01)
            val bearSpec = bearState.portfolioState.liveUpdateSpec
            assertTrue(bearSpec.progress < 500)
            assertEquals(3, bearSpec.segments.size)
            assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, bearSpec.segments[0].argbColor)
            assertEquals(bearSpec.accentArgb, bearSpec.segments[1].argbColor)
            assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, bearSpec.segments[2].argbColor)
            assertEquals(1000, bearSpec.segments.sumOf { it.length })

            // Configure holdings to Top 10 and verify dynamic active counts + live update subtitle
            vm.onIntent(DesktopMarketIntent.Core(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)))
            val top10State = vm.uiState.value
            assertEquals(10, top10State.portfolioState.enabledHoldingsCount)
            assertTrue(top10State.portfolioState.liveUpdateSpec.subtitle.startsWith("10/37 holdings"))

            // Re-enable all 37
            vm.onIntent(DesktopMarketIntent.Core(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)))
            assertEquals(37, vm.uiState.value.portfolioState.enabledHoldingsCount)

            // Switch to DEFAULT_BASKET and test editable per-listing allocation weights
            vm.onIntent(DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.DEFAULT_BASKET))
            vm.onIntent(DesktopMarketIntent.Core(MarketIntent.SetHoldingWeight("aapl", 25.0)))
            assertEquals(25.0, vm.uiState.value.customWeights["aapl"])
            assertEquals(78.48, vm.uiState.value.portfolioState.activeRawWeightPercent, 0.01)

            vm.onIntent(
                DesktopMarketIntent.Core(
                    MarketIntent.StepHoldingWeight("aapl", WeightAdjustmentStep.INCREASE_LARGE)
                )
            )
            assertEquals(26.0, vm.uiState.value.customWeights["aapl"])
            assertEquals(79.48, vm.uiState.value.portfolioState.activeRawWeightPercent, 0.01)

            vm.onIntent(DesktopMarketIntent.Core(MarketIntent.ResetWeights(WeightResetAction.RESTORE_DEFAULT_WEIGHTS)))
            assertTrue(vm.uiState.value.customWeights.isEmpty())
            assertEquals(59.41, vm.uiState.value.portfolioState.activeRawWeightPercent, 0.01)
        } finally {
            vm.dispose()
        }
    }
}
