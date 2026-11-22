package com.marketwear.mobile.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.shared.ui.PortfolioWeightMode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles interactive action buttons directly from the Android 16 Live Update notification shade:
 * - [ACTION_TOGGLE_EXPANSION_MODE]: switches between [com.marketwear.shared.ui.NotificationExpansionMode.COLLAPSED]
 *   and [com.marketwear.shared.ui.NotificationExpansionMode.EXPANDED] views in-place.
 * - [ACTION_TOGGLE_WEIGHT_MODE]: toggles between Normalized (100%) and Raw portfolio weight modes.
 */
class PortfolioNotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val selectionStore: PortfolioSelectionStore by inject()
    private val liveNotificationManager: PortfolioLiveNotificationManager by inject()

    companion object {
        const val ACTION_TOGGLE_EXPANSION_MODE =
            "com.marketwear.mobile.action.TOGGLE_NOTIFICATION_EXPANSION_MODE"
        const val ACTION_TOGGLE_WEIGHT_MODE =
            "com.marketwear.mobile.action.TOGGLE_NOTIFICATION_WEIGHT_MODE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_EXPANSION_MODE -> {
                val nextMode = selectionStore.toggleNotificationExpansionMode()
                val quotes = selectionStore.cachedQuotes.value
                if (quotes.isNotEmpty()) {
                    liveNotificationManager.showOrUpdateFromQuotes(
                        quotes = quotes,
                        weightMode = selectionStore.weightMode.value,
                        disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                        customWeights = selectionStore.customWeights.value,
                        expansionMode = nextMode
                    )
                }
            }

            ACTION_TOGGLE_WEIGHT_MODE -> {
                val nextWeightMode: PortfolioWeightMode = selectionStore.weightMode.value.next()
                selectionStore.setWeightMode(nextWeightMode)
                val quotes = selectionStore.cachedQuotes.value
                if (quotes.isNotEmpty()) {
                    liveNotificationManager.showOrUpdateFromQuotes(
                        quotes = quotes,
                        weightMode = nextWeightMode,
                        disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                        customWeights = selectionStore.customWeights.value,
                        expansionMode = selectionStore.notificationExpansionMode.value
                    )
                }
            }
        }
    }
}
