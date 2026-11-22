package com.marketwear.shared

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.HoldingSelectionPreset
import com.marketwear.shared.ui.MacOsMarketBridge
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketStateReducer
import com.marketwear.shared.ui.MarketStore
import com.marketwear.shared.ui.MarketStoreFactory
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.WeightAdjustmentStep
import com.marketwear.shared.ui.WeightResetAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketStoreTest {

    @Test
    fun marketStoreReducesWeightModeHoldingSelectionAndRefreshPolicies() = runBlocking {
        isAssertOnMainThreadEnabled = false
        val persistedDisabled = mutableListOf<Set<String>>()
        val persistedWeights = mutableListOf<Map<String, Double>>()
        val capturedPolicies = mutableListOf<RefreshPolicy>()

        val store = MarketStoreFactory(
            storeFactory = DefaultStoreFactory(),
            mainContext = Dispatchers.Unconfined
        ).create(
            onPersistPortfolioConfig = { disabledIds, weights, _ ->
                persistedDisabled.add(disabledIds)
                persistedWeights.add(weights)
            },
            onFetchQuotes = { policy, _, _ ->
                capturedPolicies.add(policy)
                if (policy == RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO) {
                    throw IllegalStateException("Simulated upstream timeout")
                }
                MarketRegistry.getFallbackQuotes()
            }
        )

        try {
            assertEquals(PortfolioWeightMode.NORMALIZED_100, store.state.weightMode)
            assertEquals(37, store.state.dashboard.portfolioState.enabledHoldingsCount)

            // Toggle weight mode via MVI User intent
            store.accept(MarketStore.Intent.User(MarketIntent.ToggleWeightMode))
            assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, store.state.weightMode)

            // Apply Top 10 preset
            store.accept(MarketStore.Intent.User(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)))
            assertEquals(10, store.state.dashboard.portfolioState.enabledHoldingsCount)
            assertEquals(PortfolioRegistry.disabledIdsForTop10(), persistedDisabled.last())

            // Edit custom holding weight and step adjustment via MVI intents
            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", 25.0)))
            assertEquals(25.0, store.state.customWeights["aapl"])
            assertEquals(mapOf("aapl" to 25.0), persistedWeights.last())

            store.accept(
                MarketStore.Intent.User(
                    MarketIntent.StepHoldingWeight("aapl", WeightAdjustmentStep.INCREASE_FINE)
                )
            )
            assertEquals(25.25, store.state.customWeights["aapl"])

            // Sanitization: clamping out-of-range values and pruning overrides back to default
            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", 250.0)))
            assertEquals(PortfolioRegistry.MAX_HOLDING_WEIGHT_PERCENT, store.state.customWeights["aapl"])
            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", -5.0)))
            assertEquals(PortfolioRegistry.MIN_HOLDING_WEIGHT_PERCENT, store.state.customWeights["aapl"])
            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", 5.93)))
            assertTrue(store.state.customWeights.isEmpty())

            // RESTORE_DEFAULT_WEIGHTS leaves holding selection intact while RESET_ALL_TO_DEFAULTS clears both
            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", 18.0)))
            store.accept(MarketStore.Intent.User(MarketIntent.ResetWeights(WeightResetAction.RESTORE_DEFAULT_WEIGHTS)))
            assertTrue(store.state.customWeights.isEmpty())
            assertEquals(10, store.state.dashboard.portfolioState.enabledHoldingsCount)

            store.accept(MarketStore.Intent.User(MarketIntent.SetHoldingWeight("aapl", 18.0)))
            store.accept(MarketStore.Intent.User(MarketIntent.ResetWeights(WeightResetAction.RESET_ALL_TO_DEFAULTS)))
            assertTrue(store.state.customWeights.isEmpty())
            assertEquals(37, store.state.dashboard.portfolioState.enabledHoldingsCount)

            // Enforce invariant: toggling off the final active holding is ignored by reducer
            val allExceptOne = PortfolioRegistry.ALL_HOLDING_IDS - "aapl"
            val afterLastGuard = MarketStateReducer.reduceHoldingToggle(allExceptOne, "aapl")
            assertEquals(allExceptOne, afterLastGuard)

            // Apply All 37 preset
            store.accept(MarketStore.Intent.User(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)))
            assertEquals(37, store.state.dashboard.portfolioState.enabledHoldingsCount)

            // Trigger successful refresh policy
            store.accept(MarketStore.Intent.User(MarketIntent.RequestRefresh(RefreshPolicy.FORCE_CORE_QUOTES)))
            assertEquals(RefreshPolicy.FORCE_CORE_QUOTES, capturedPolicies.single())
            assertEquals(RefreshState.IDLE, store.state.refreshState)

            // Trigger failing refresh policy and verify ErrorOccurred label + ERROR state
            store.accept(
                MarketStore.Intent.User(
                    MarketIntent.RequestRefresh(RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO)
                )
            )
            assertEquals(RefreshState.ERROR, store.state.refreshState)
            assertTrue(store.state.errorMessage?.contains("Simulated upstream timeout") == true)
        } finally {
            store.dispose()
        }
    }

    @Test
    fun marketStoreEmitsErrorLabelWhenFetchFails() = runBlocking {
        isAssertOnMainThreadEnabled = false
        val store = MarketStoreFactory(
            storeFactory = DefaultStoreFactory(),
            mainContext = Dispatchers.Unconfined
        ).create(
            onFetchQuotes = { _, _, _ -> throw RuntimeException("Network unavailable") }
        )

        try {
            val receivedLabel: MarketStore.Label
            store.accept(MarketStore.Intent.User(MarketIntent.RequestRefresh(RefreshPolicy.CACHE_FIRST)))
            if (store.state.refreshState == RefreshState.ERROR) {
                receivedLabel = MarketStore.Label.ErrorOccurred(store.state.errorMessage ?: "")
            } else {
                receivedLabel = store.labels.first()
            }
            assertTrue(receivedLabel is MarketStore.Label.ErrorOccurred)
            assertEquals("Network unavailable", receivedLabel.message)
        } finally {
            store.dispose()
        }
    }

    @Test
    fun macOsMarketBridgeExposesStoreStateAndReducesIntents() {
        val bridge = MacOsMarketBridge(observerContext = Dispatchers.Unconfined)
        try {
            val initial = bridge.currentState
            assertEquals(PortfolioWeightMode.NORMALIZED_100, initial.weightMode)
            assertEquals(37, initial.dashboard.portfolioState.enabledHoldingsCount)
            assertTrue(initial.dashboard.portfolioState.liveUpdateSpec.segments.isNotEmpty())
            assertEquals(500, initial.dashboard.portfolioState.liveUpdateSpec.baselinePoint)

            bridge.toggleWeightMode()
            assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, bridge.currentState.weightMode)

            bridge.applyPreset(HoldingSelectionPreset.TOP_10)
            assertEquals(10, bridge.currentState.dashboard.portfolioState.enabledHoldingsCount)

            bridge.setHoldingWeight("aapl", 20.0)
            assertEquals(20.0, bridge.currentState.customWeights["aapl"])
        } finally {
            bridge.dispose()
        }
    }
}
