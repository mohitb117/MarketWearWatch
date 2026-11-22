package com.marketwear.mobile.data

import android.content.Context
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketStateReducer
import com.marketwear.shared.ui.NotificationExpansionMode
import com.marketwear.shared.ui.PortfolioWeightMode
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

    private val _notificationExpansionMode = MutableStateFlow(
        NotificationExpansionMode.fromStorageKey(prefs.getString(KEY_NOTIFICATION_EXPANSION_MODE, null))
    )
    val notificationExpansionMode: StateFlow<NotificationExpansionMode> =
        _notificationExpansionMode.asStateFlow()

    private val _weightMode = MutableStateFlow(loadWeightMode())
    val weightMode: StateFlow<PortfolioWeightMode> = _weightMode.asStateFlow()

    private val _cachedQuotes = MutableStateFlow(loadCachedQuotes())
    val cachedQuotes: StateFlow<List<IndexQuote>> = _cachedQuotes.asStateFlow()

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

    fun setNotificationExpansionMode(mode: NotificationExpansionMode) {
        if (mode != _notificationExpansionMode.value) {
            _notificationExpansionMode.value = mode
            prefs.edit().putString(KEY_NOTIFICATION_EXPANSION_MODE, mode.name).apply()
        }
    }

    fun toggleNotificationExpansionMode(): NotificationExpansionMode {
        val next = _notificationExpansionMode.value.next()
        setNotificationExpansionMode(next)
        return next
    }

    fun setWeightMode(mode: PortfolioWeightMode) {
        if (mode != _weightMode.value) {
            _weightMode.value = mode
            prefs.edit().putString(KEY_WEIGHT_MODE, mode.name).apply()
        }
    }

    fun saveCachedQuotes(quotes: List<IndexQuote>) {
        if (quotes.isEmpty()) return
        _cachedQuotes.value = quotes
        prefs.edit().putString(KEY_CACHED_QUOTES_JSON, json.encodeToString(quotes)).apply()
    }

    private fun loadWeightMode(): PortfolioWeightMode {
        val raw = prefs.getString(KEY_WEIGHT_MODE, null)
        return PortfolioWeightMode.entries.firstOrNull { it.name == raw }
            ?: PortfolioWeightMode.NORMALIZED_100
    }

    private fun loadCachedQuotes(): List<IndexQuote> {
        val raw = prefs.getString(KEY_CACHED_QUOTES_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<IndexQuote>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
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
        private const val KEY_NOTIFICATION_EXPANSION_MODE = "live_notification_expansion_mode"
        private const val KEY_WEIGHT_MODE = "portfolio_weight_mode"
        private const val KEY_CACHED_QUOTES_JSON = "cached_quotes_json_v1"
    }
}
