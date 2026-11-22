package com.marketwear.mobile.sync

import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.SyncUrgency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MobileWearableListenerService : WearableListenerService(), KoinComponent {

    private val repository: MobileMarketRepository by inject()
    private val syncManager: WearDataSyncManager by inject()
    private val selectionStore: PortfolioSelectionStore by inject()
    private val liveNotificationManager: PortfolioLiveNotificationManager by inject()
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearConstants.PATH_OPEN_PHONE_APP) {
            try {
                val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                startActivity(launchIntent)
            } catch (_: Exception) {
                // Ignore if activity launch is restricted
            }
            return
        }
        if (messageEvent.path == WearConstants.PATH_REQUEST_REFRESH) {
            // Run synchronously on the background binder thread so Google Play Services
            // maintains its WakeLock until the fetch and data push complete.
            runBlocking(Dispatchers.IO) {
                try {
                    val disabledIds = selectionStore.disabledHoldingIds.value
                    val customWeights = selectionStore.customWeights.value
                    val quotes = repository.fetchAllQuotes(
                        policy = RefreshPolicy.CACHE_FIRST,
                        disabledHoldingIds = disabledIds,
                        customWeights = customWeights
                    )
                    selectionStore.saveCachedQuotes(quotes)
                    syncManager.syncQuotesToWatch(
                        quotes = quotes,
                        urgency = SyncUrgency.IMMEDIATE,
                        intradayData = repository.getCachedPortfolioTickerData()
                    )
                    if (selectionStore.liveNotificationPreference.value == LiveNotificationPreference.ACTIVE) {
                        liveNotificationManager.showOrUpdateFromQuotes(
                            quotes = quotes,
                            weightMode = selectionStore.weightMode.value,
                            disabledHoldingIds = disabledIds,
                            customWeights = customWeights,
                            expansionMode = selectionStore.notificationExpansionMode.value
                        )
                    }
                    try {
                        MarketGlanceWidget().updateAll(applicationContext)
                    } catch (_: Exception) {
                        // Ignore if no widgets placed
                    }
                } catch (_: Exception) {
                    // Ignored on message failure
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearConstants.PATH_PORTFOLIO_CONFIG
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val payload = dataMap.getString(WearConstants.KEY_DISABLED_HOLDING_IDS)
                val weightsPayload = dataMap.getString(WearConstants.KEY_CUSTOM_WEIGHTS)
                if (payload == null && weightsPayload == null) continue
                runBlocking(Dispatchers.IO) {
                    try {
                        val disabledIds = payload?.let {
                            json.decodeFromString<List<String>>(it).toSet()
                        } ?: selectionStore.disabledHoldingIds.value
                        val customWeights = weightsPayload?.let {
                            json.decodeFromString<Map<String, Double>>(it)
                        } ?: selectionStore.customWeights.value

                        selectionStore.setDisabledHoldings(disabledIds)
                        selectionStore.setCustomWeights(customWeights)

                        val updatedQuotes = repository.recomputePortfolioForSelection(
                            disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                            customWeights = selectionStore.customWeights.value
                        )
                        if (updatedQuotes.isNotEmpty()) {
                            selectionStore.saveCachedQuotes(updatedQuotes)
                            syncManager.syncQuotesToWatch(
                                quotes = updatedQuotes,
                                urgency = SyncUrgency.COALESCED_BACKGROUND,
                                intradayData = repository.getCachedPortfolioTickerData()
                            )
                            if (selectionStore.liveNotificationPreference.value == LiveNotificationPreference.ACTIVE) {
                                liveNotificationManager.showOrUpdateFromQuotes(
                                    quotes = updatedQuotes,
                                    weightMode = selectionStore.weightMode.value,
                                    disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                                    customWeights = selectionStore.customWeights.value,
                                    expansionMode = selectionStore.notificationExpansionMode.value
                                )
                            }
                            try {
                                MarketGlanceWidget().updateAll(applicationContext)
                            } catch (_: Exception) {
                                // Ignore if no widgets placed
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore malformed payload
                    }
                }
            }
        }
    }
}
