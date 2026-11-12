package com.marketwear.indices.service

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.indices.data.db.IndexQuoteEntity
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MarketWearableListenerService : WearableListenerService() {

    private val db by lazy { MarketDatabase.getInstance(applicationContext) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == WearConstants.PATH_MARKET_QUOTES) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                    // Run synchronously on background binder thread so WakeLock is held
                    // until database insert and complication updates complete
                    runBlocking(Dispatchers.IO) {
                        val entities = mutableListOf<IndexQuoteEntity>()

                        // Extract SPX
                        extractQuoteEntity(dataMap, WearConstants.PREFIX_SPX)?.let {
                            entities.add(it)
                        }

                        // Extract NDX
                        extractQuoteEntity(dataMap, WearConstants.PREFIX_NDX)?.let {
                            entities.add(it)
                        }

                        if (entities.isNotEmpty()) {
                            db.indexQuoteDao().insertOrUpdate(entities)
                            MarketSyncWorker.triggerComplicationUpdates(applicationContext)
                        }
                    }
                }
            }
        }
    }

    private fun extractQuoteEntity(dataMap: DataMap, prefix: String): IndexQuoteEntity? {
        val symbolKey = prefix + WearConstants.FIELD_SYMBOL
        if (!dataMap.containsKey(symbolKey)) return null

        val symbol = dataMap.getString(symbolKey) ?: return null
        val name = dataMap.getString(prefix + WearConstants.FIELD_NAME) ?: (if (symbol == "SPX") "S&P 500" else "Nasdaq 100")
        val price = dataMap.getDouble(prefix + WearConstants.FIELD_PRICE, 0.0)
        val change = dataMap.getDouble(prefix + WearConstants.FIELD_CHANGE, 0.0)
        val percent = dataMap.getDouble(prefix + WearConstants.FIELD_PERCENT, 0.0)
        val high = dataMap.getDouble(prefix + WearConstants.FIELD_HIGH, 0.0)
        val low = dataMap.getDouble(prefix + WearConstants.FIELD_LOW, 0.0)
        val time = dataMap.getLong(prefix + WearConstants.FIELD_TIME, System.currentTimeMillis())

        return IndexQuoteEntity(
            symbol = symbol,
            name = name,
            price = price,
            change = change,
            percentChange = percent,
            dayHigh = high,
            dayLow = low,
            lastUpdated = time
        )
    }
}
