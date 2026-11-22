package com.marketwear.mobile.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.ui.SyncOutcome
import com.marketwear.shared.ui.SyncUrgency
import com.marketwear.shared.ui.WatchConnectionState
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WearDataSyncManager(private val context: Context) {

    private val dataClient = Wearable.getDataClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncQuotesToWatch(
        quotes: List<IndexQuote>,
        urgency: SyncUrgency = SyncUrgency.IMMEDIATE,
        intradayData: Map<String, TickerIntradayData> = emptyMap()
    ): SyncOutcome {
        return try {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_MARKET_QUOTES)
            val dataMap = putDataMapReq.dataMap

            // Serialize full quote list into JSON payload
            val jsonPayload = json.encodeToString(quotes)
            dataMap.putString(WearConstants.KEY_QUOTES_PAYLOAD, jsonPayload)
            dataMap.putLong(WearConstants.KEY_SYNC_TIMESTAMP, System.currentTimeMillis())

            if (intradayData.isNotEmpty()) {
                dataMap.putString(
                    WearConstants.KEY_PORTFOLIO_INTRADAY_PAYLOAD,
                    json.encodeToString(intradayData)
                )
            }

            // Only populate legacy SPX/NDX prefix fields for major indices
            quotes.forEach { quote ->
                val prefix = when (quote.symbol) {
                    "SPX" -> WearConstants.PREFIX_SPX
                    "NDX" -> WearConstants.PREFIX_NDX
                    else -> null
                }
                if (prefix != null) {
                    dataMap.putString(prefix + WearConstants.FIELD_SYMBOL, quote.symbol)
                    dataMap.putString(prefix + WearConstants.FIELD_NAME, quote.name)
                    dataMap.putDouble(prefix + WearConstants.FIELD_PRICE, quote.price)
                    dataMap.putDouble(prefix + WearConstants.FIELD_CHANGE, quote.change)
                    dataMap.putDouble(prefix + WearConstants.FIELD_PERCENT, quote.percentChange)
                    dataMap.putDouble(prefix + WearConstants.FIELD_HIGH, quote.dayHigh)
                    dataMap.putDouble(prefix + WearConstants.FIELD_LOW, quote.dayLow)
                    dataMap.putLong(prefix + WearConstants.FIELD_TIME, quote.lastUpdated)
                }
            }

            val putRequest = putDataMapReq.asPutDataRequest()
            when (urgency) {
                SyncUrgency.IMMEDIATE -> putRequest.setUrgent()
                SyncUrgency.COALESCED_BACKGROUND -> Unit
            }
            dataClient.putDataItem(putRequest).await()
            SyncOutcome.SYNCED
        } catch (_: Exception) {
            SyncOutcome.FAILED
        }
    }

    suspend fun syncDisabledHoldingsToWatch(
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double> = emptyMap()
    ): SyncOutcome {
        return try {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_PORTFOLIO_CONFIG)
            val dataMap = putDataMapReq.dataMap
            dataMap.putString(
                WearConstants.KEY_DISABLED_HOLDING_IDS,
                json.encodeToString(disabledHoldingIds.toList())
            )
            dataMap.putString(
                WearConstants.KEY_CUSTOM_WEIGHTS,
                json.encodeToString(customWeights)
            )
            dataMap.putLong(WearConstants.KEY_CONFIG_TIMESTAMP, System.currentTimeMillis())

            val putRequest = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putRequest).await()
            SyncOutcome.SYNCED
        } catch (_: Exception) {
            SyncOutcome.FAILED
        }
    }

    suspend fun getWatchConnectionState(): WatchConnectionState {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WearConstants.CAPABILITY_WATCH, CapabilityClient.FILTER_REACHABLE)
                .await()
            if (capabilityInfo.nodes.isNotEmpty()) {
                WatchConnectionState.CONNECTED
            } else {
                WatchConnectionState.DISCONNECTED
            }
        } catch (_: Exception) {
            WatchConnectionState.DISCONNECTED
        }
    }
}
