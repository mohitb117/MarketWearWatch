package com.marketwear.mobile.sync

import android.content.Intent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.ui.RefreshPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Listens for Wearable Data Layer messages and data events from the paired watch.
 * Acts purely as an ingest channel into [MobileMarketRepository], which then reactively
 * drives all UI, widget, notification, and sync subscribers through [PhoneMarketSyncHub].
 */
class MobileWearableListenerService : WearableListenerService(), KoinComponent {

    private val repository: MobileMarketRepository by inject()
    private val selectionStore: PortfolioSelectionStore by inject()
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
            // Background fetch initiated by Watch message.
            // Commits quotes into MobileMarketRepository, triggering PhoneMarketSyncHub reactively.
            runBlocking(Dispatchers.IO) {
                try {
                    repository.fetchAllQuotes(
                        policy = RefreshPolicy.CACHE_FIRST,
                        disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                        customWeights = selectionStore.customWeights.value
                    )
                } catch (_: Exception) {
                    // Ignored on message failure
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path

            when (path) {
                WearConstants.PATH_PORTFOLIO_CONFIG -> {
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

                            repository.recomputePortfolioForSelection(
                                disabledHoldingIds = disabledIds,
                                customWeights = customWeights
                            )
                        } catch (_: Exception) {
                            // Ignore malformed payload
                        }
                    }
                }

                WearConstants.PATH_MARKET_QUOTES -> {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val quotesPayload = dataMap.getString(WearConstants.KEY_QUOTES_PAYLOAD)
                    val intradayPayload = dataMap.getString(WearConstants.KEY_PORTFOLIO_INTRADAY_PAYLOAD)
                    if (quotesPayload.isNullOrBlank()) continue

                    runBlocking(Dispatchers.IO) {
                        try {
                            val quotes = json.decodeFromString<List<IndexQuote>>(quotesPayload)
                            val intraday = if (!intradayPayload.isNullOrBlank()) {
                                json.decodeFromString<Map<String, TickerIntradayData>>(intradayPayload)
                            } else {
                                emptyMap()
                            }
                            repository.commitQuotes(quotes, intraday)
                        } catch (_: Exception) {
                            // Ignore malformed payload
                        }
                    }
                }
            }
        }
    }
}
