package com.marketwear.mobile.data

import android.content.Context
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketStateReducer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists user-configured portfolio holding exclusions (`disabledHoldingIds`) and
 * Android 16 Live Update notification preferences via enum states in SharedPreferences.
 */
class PortfolioSelectionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _disabledHoldingIds = MutableStateFlow(loadDisabledIds())
    val disabledHoldingIds: StateFlow<Set<String>> = _disabledHoldingIds.asStateFlow()

    private val _customWeights = MutableStateFlow(loadCustomWeights())
    val customWeights: StateFlow<Map<String, Double>> = _customWeights.asStateFlow()

    private val _liveNotificationPreference = MutableStateFlow(
        LiveNotificationPreference.fromStorageKey(prefs.getString(KEY_LIVE_NOTIFICATION_PREF, null))
    )
    val liveNotificationPreference: StateFlow<LiveNotificationPreference> =
        _liveNotificationPreference.asStateFlow()

    fun toggleHolding(holdingId: String): Set<String> {
        val next = MarketStateReducer.reduceHoldingToggle(_disabledHoldingIds.value, holdingId)
        setDisabledHoldings(next)
        return next
    }

    fun setDisabledHoldings(ids: Set<String>) {
        val finalSet = MarketStateReducer.sanitizeDisabledHoldings(ids)
        if (finalSet != _disabledHoldingIds.value) {
            _disabledHoldingIds.value = finalSet
            prefs.edit()
                .putString(KEY_DISABLED_HOLDING_IDS, json.encodeToString(finalSet.toList()))
                .apply()
        }
    }

    fun setCustomWeights(weights: Map<String, Double>) {
        val finalWeights = MarketStateReducer.sanitizeCustomWeights(weights)
        if (finalWeights != _customWeights.value) {
            _customWeights.value = finalWeights
            prefs.edit()
                .putString(KEY_CUSTOM_WEIGHTS, json.encodeToString(finalWeights))
                .apply()
        }
    }

    fun setLiveNotificationPreference(preference: LiveNotificationPreference) {
        _liveNotificationPreference.value = preference
        prefs.edit().putString(KEY_LIVE_NOTIFICATION_PREF, preference.name).apply()
    }

    fun toggleLiveNotificationPreference(): LiveNotificationPreference {
        val next = _liveNotificationPreference.value.next()
        setLiveNotificationPreference(next)
        return next
    }

    private fun loadDisabledIds(): Set<String> {
        val raw = prefs.getString(KEY_DISABLED_HOLDING_IDS, null) ?: return emptySet()
        return try {
            MarketStateReducer.sanitizeDisabledHoldings(json.decodeFromString<List<String>>(raw).toSet())
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun loadCustomWeights(): Map<String, Double> {
        val raw = prefs.getString(KEY_CUSTOM_WEIGHTS, null) ?: return emptyMap()
        return try {
            MarketStateReducer.sanitizeCustomWeights(json.decodeFromString<Map<String, Double>>(raw))
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "marketwear_portfolio_selection"
        private const val KEY_DISABLED_HOLDING_IDS = "disabled_holding_ids"
        private const val KEY_CUSTOM_WEIGHTS = "custom_holding_weights"
        private const val KEY_LIVE_NOTIFICATION_PREF = "live_notification_preference"
    }
}
