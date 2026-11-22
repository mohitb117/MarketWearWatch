package com.marketwear.shared.ui

import com.marketwear.shared.model.PortfolioRegistry
import kotlin.math.abs
import kotlin.math.round

/**
 * Fine-grained cache & network refresh policy replacing coarse booleans.
 */
enum class RefreshPolicy {
    /** Reuse cached quotes within active (5m) or off-hours (6h) TTL and 2h portfolio throttle. */
    CACHE_FIRST,

    /** Force fresh core indices/sectors while honoring the 2h portfolio 37-ticker throttle. */
    FORCE_CORE_QUOTES,

    /** Force fresh quotes across core indices, sectors, AND all 37 portfolio holdings. */
    FORCE_ALL_INCLUDING_PORTFOLIO
}

/**
 * Quick selection presets for configuring tracked portfolio holdings.
 */
enum class HoldingSelectionPreset {
    ALL_37,
    TOP_10
}

/**
 * Wearable Data Layer transmission urgency policy.
 */
enum class SyncUrgency {
    IMMEDIATE,
    COALESCED_BACKGROUND
}

/**
 * Typed result of a cross-device Wearable Data Layer synchronization operation.
 */
enum class SyncOutcome {
    SYNCED,
    FAILED
}

/**
 * Theme appearance enum for Compose Multiplatform surfaces.
 */
enum class ThemeAppearance {
    DARK,
    LIGHT
}

/**
 * Live US equities session state shown in header status pills.
 */
enum class MarketSessionStatus(val badgeText: String) {
    LIVE_MARKET("US OPEN"),
    OFF_HOURS("EXTENDED");

    companion object {
        fun fromMarketWindow(isOpen: Boolean): MarketSessionStatus =
            if (isOpen) LIVE_MARKET else OFF_HOURS
    }
}

/**
 * Layout mode for rendering US market sectors.
 */
enum class SectorDisplayMode(val label: String) {
    HEATMAP("Heatmap"),
    LIST("List")
}

/**
 * Sort order for US market sectors.
 */
enum class SectorSortOrder(val label: String) {
    TOP_GAINERS("Gainers"),
    TOP_LOSERS("Losers"),
    ALPHABETICAL("A–Z")
}

/**
 * Granular weight stepper actions for adjusting individual portfolio listing allocations.
 */
enum class WeightAdjustmentStep(val deltaPercent: Double) {
    DECREASE_LARGE(-1.00),
    DECREASE_MEDIUM(-0.50),
    DECREASE_FINE(-0.25),
    INCREASE_FINE(0.25),
    INCREASE_MEDIUM(0.50),
    INCREASE_LARGE(1.00)
}

/**
 * Reset policy for custom holding weight overrides.
 */
enum class WeightResetAction {
    RESTORE_DEFAULT_WEIGHTS,
    RESET_ALL_TO_DEFAULTS
}

/**
 * Unidirectional MVI user intents shared across Mobile, Wear OS, and Desktop surfaces.
 */
sealed interface MarketIntent {
    data object ToggleWeightMode : MarketIntent
    data class SetWeightMode(val mode: PortfolioWeightMode) : MarketIntent
    data class ToggleHolding(val holdingId: String) : MarketIntent
    data class ApplyHoldingPreset(val preset: HoldingSelectionPreset) : MarketIntent
    data class SetHoldingWeight(val holdingId: String, val weightPercent: Double) : MarketIntent
    data class StepHoldingWeight(val holdingId: String, val step: WeightAdjustmentStep) : MarketIntent
    data class ResetWeights(val action: WeightResetAction) : MarketIntent
    data class RequestRefresh(val policy: RefreshPolicy) : MarketIntent
}

/**
 * Pure MVI core state snapshot shared by platform ViewModels.
 */
data class CoreMarketState(
    val weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
    val disabledHoldingIds: Set<String> = emptySet(),
    val customWeights: Map<String, Double> = emptyMap(),
    val refreshState: RefreshState = RefreshState.IDLE
)

/**
 * Pure, side-effect-free state reducer for [MarketIntent] transitions and holding selection/weight invariants.
 */
object MarketStateReducer {

    fun sanitizeDisabledHoldings(ids: Set<String>): Set<String> {
        val validIds = PortfolioRegistry.ALL_HOLDING_IDS
        val sanitized = ids.intersect(validIds)
        return if (sanitized.size >= validIds.size) emptySet() else sanitized
    }

    fun sanitizeCustomWeights(raw: Map<String, Double>): Map<String, Double> {
        if (raw.isEmpty()) return emptyMap()
        return buildMap {
            for ((id, value) in raw) {
                if (id !in PortfolioRegistry.ALL_HOLDING_IDS) continue
                if (!value.isFinite()) continue
                val clamped = value.coerceIn(
                    PortfolioRegistry.MIN_HOLDING_WEIGHT_PERCENT,
                    PortfolioRegistry.MAX_HOLDING_WEIGHT_PERCENT
                )
                val rounded = round(clamped * 100.0) / 100.0
                val defaultWeight = PortfolioRegistry.DEFAULT_WEIGHTS_BY_ID[id] ?: continue
                if (abs(rounded - defaultWeight) >= 0.005) {
                    put(id, rounded)
                }
            }
        }
    }

    fun reduceSetHoldingWeight(
        currentWeights: Map<String, Double>,
        holdingId: String,
        weightPercent: Double
    ): Map<String, Double> {
        if (holdingId !in PortfolioRegistry.ALL_HOLDING_IDS) return currentWeights
        if (!weightPercent.isFinite()) return currentWeights
        return sanitizeCustomWeights(currentWeights + (holdingId to weightPercent))
    }

    fun reduceStepHoldingWeight(
        currentWeights: Map<String, Double>,
        holdingId: String,
        step: WeightAdjustmentStep
    ): Map<String, Double> {
        val defaultWeight = PortfolioRegistry.DEFAULT_WEIGHTS_BY_ID[holdingId] ?: return currentWeights
        val current = currentWeights[holdingId] ?: defaultWeight
        val next = (current + step.deltaPercent).coerceIn(
            PortfolioRegistry.MIN_HOLDING_WEIGHT_PERCENT,
            PortfolioRegistry.MAX_HOLDING_WEIGHT_PERCENT
        )
        return reduceSetHoldingWeight(currentWeights, holdingId, next)
    }

    fun reduceHoldingToggle(
        currentDisabled: Set<String>,
        holdingId: String
    ): Set<String> {
        val validIds = PortfolioRegistry.ALL_HOLDING_IDS
        if (holdingId !in validIds) return currentDisabled
        val candidate = if (holdingId in currentDisabled) {
            currentDisabled - holdingId
        } else {
            val next = currentDisabled + holdingId
            val remainingActive = validIds.count { it !in next }
            if (remainingActive < 1) currentDisabled else next
        }
        return sanitizeDisabledHoldings(candidate)
    }

    fun reduceHoldingPreset(preset: HoldingSelectionPreset): Set<String> = when (preset) {
        HoldingSelectionPreset.ALL_37 -> emptySet()
        HoldingSelectionPreset.TOP_10 -> PortfolioRegistry.disabledIdsForTop10()
    }

    fun reduceCoreIntent(
        state: CoreMarketState,
        intent: MarketIntent
    ): CoreMarketState = when (intent) {
        MarketIntent.ToggleWeightMode ->
            state.copy(weightMode = state.weightMode.next())
        is MarketIntent.SetWeightMode ->
            state.copy(weightMode = intent.mode)
        is MarketIntent.ToggleHolding ->
            state.copy(disabledHoldingIds = reduceHoldingToggle(state.disabledHoldingIds, intent.holdingId))
        is MarketIntent.ApplyHoldingPreset ->
            state.copy(disabledHoldingIds = reduceHoldingPreset(intent.preset))
        is MarketIntent.SetHoldingWeight ->
            state.copy(customWeights = reduceSetHoldingWeight(state.customWeights, intent.holdingId, intent.weightPercent))
        is MarketIntent.StepHoldingWeight ->
            state.copy(customWeights = reduceStepHoldingWeight(state.customWeights, intent.holdingId, intent.step))
        is MarketIntent.ResetWeights -> when (intent.action) {
            WeightResetAction.RESTORE_DEFAULT_WEIGHTS ->
                state.copy(customWeights = emptyMap())
            WeightResetAction.RESET_ALL_TO_DEFAULTS ->
                state.copy(disabledHoldingIds = emptySet(), customWeights = emptyMap())
        }
        is MarketIntent.RequestRefresh ->
            state.copy(refreshState = RefreshState.REFRESHING)
    }
}
