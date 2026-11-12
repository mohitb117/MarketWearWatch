package com.marketwear.mobile.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.IndexQuote
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WearDataSyncManager(private val context: Context) {

    private val dataClient = Wearable.getDataClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    suspend fun syncQuotesToWatch(quotes: List<IndexQuote>): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_MARKET_QUOTES)
            val dataMap = putDataMapReq.dataMap

            // Serialize full quote list into JSON payload
            val jsonPayload = Json.encodeToString(quotes)
            dataMap.putString(WearConstants.KEY_QUOTES_PAYLOAD, jsonPayload)
            dataMap.putLong(WearConstants.KEY_SYNC_TIMESTAMP, System.currentTimeMillis())

            // Also put individual fields for fast field-level extraction if needed
            quotes.forEach { quote ->
                val prefix = if (quote.symbol == "SPX") WearConstants.PREFIX_SPX else WearConstants.PREFIX_NDX
                dataMap.putString(prefix + WearConstants.FIELD_SYMBOL, quote.symbol)
                dataMap.putString(prefix + WearConstants.FIELD_NAME, quote.name)
                dataMap.putDouble(prefix + WearConstants.FIELD_PRICE, quote.price)
                dataMap.putDouble(prefix + WearConstants.FIELD_CHANGE, quote.change)
                dataMap.putDouble(prefix + WearConstants.FIELD_PERCENT, quote.percentChange)
                dataMap.putDouble(prefix + WearConstants.FIELD_HIGH, quote.dayHigh)
                dataMap.putDouble(prefix + WearConstants.FIELD_LOW, quote.dayLow)
                dataMap.putLong(prefix + WearConstants.FIELD_TIME, quote.lastUpdated)
            }

            val request = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isWatchConnected(): Boolean {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WearConstants.CAPABILITY_WATCH, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
