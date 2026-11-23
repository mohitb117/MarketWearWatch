package com.marketwear.indices.service

import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.WatchPortfolioSelectionStore
import com.marketwear.indices.data.db.IndexQuoteEntity
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.indices.tile.MarketWearPortfolioTileService
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MarketWearableListenerService : WearableListenerService(), KoinComponent {

    private val db: MarketDatabase by inject()
    private val selectionStore: WatchPortfolioSelectionStore by inject()
    private val liveNotificationManager: WatchLiveNotificationManager by inject()
    private val json = Json { ignoreUnknownKeys = true }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val uri = event.dataItem.uri
            when (uri.path) {
                WearConstants.PATH_MARKET_QUOTES -> handleMarketQuotes(DataMapItem.fromDataItem(event.dataItem).dataMap)
                WearConstants.PATH_PORTFOLIO_CONFIG -> handlePortfolioConfig(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
    }

    private fun handleMarketQuotes(dataMap: DataMap) {
        // Run synchronously on background binder thread so WakeLock is held
        // until database insert and complication updates complete
        runBlocking(Dispatchers.IO) {
            val intradayPayload = dataMap.getString(WearConstants.KEY_PORTFOLIO_INTRADAY_PAYLOAD)
            if (!intradayPayload.isNullOrBlank()) {
                try {
                    val intradayMap = json.decodeFromString<Map<String, TickerIntradayData>>(intradayPayload)
                    selectionStore.saveTickerIntradayData(intradayMap)
                } catch (_: Exception) {
                    // Ignore malformed intraday snapshot
                }
            }

            val entities = mutableListOf<IndexQuoteEntity>()
            val payload = dataMap.getString(WearConstants.KEY_QUOTES_PAYLOAD)
            if (!payload.isNullOrBlank()) {
                try {
                    val quotes = json.decodeFromString<List<IndexQuote>>(payload)
                    val disabledIds = selectionStore.disabledHoldingIds.value
                    val customWeights = selectionStore.customWeights.value
                    val cachedIntraday = selectionStore.cachedTickerIntradayData.value
                    val adjustedQuotes = if ((disabledIds.isNotEmpty() || customWeights.isNotEmpty()) && cachedIntraday.isNotEmpty()) {
                        val latestTs = quotes.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
                        val customPortfolio = PortfolioRegistry.computePortfolioQuote(
                            dataByYahooTicker = cachedIntraday,
                            timestampMillis = latestTs,
                            disabledHoldingIds = disabledIds,
                            customWeights = customWeights
                        )
                        listOf(customPortfolio) + quotes.filter { it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL }
                    } else {
                        quotes
                    }
                    entities.addAll(adjustedQuotes.map { IndexQuoteEntity.fromDomainModel(it) })
                } catch (_: Exception) {
                    // Fallback to individual fields
                }
            }

            if (entities.isEmpty()) {
                extractQuoteEntity(dataMap, WearConstants.PREFIX_SPX)?.let { entities.add(it) }
                extractQuoteEntity(dataMap, WearConstants.PREFIX_NDX)?.let { entities.add(it) }
            }

            if (entities.isNotEmpty()) {
                db.indexQuoteDao().insertOrUpdate(entities)
                liveNotificationManager.showOrUpdateFromQuotes(
                    quotes = entities.map { it.toDomainModel() },
                    disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                    customWeights = selectionStore.customWeights.value
                )
                MarketSyncWorker.triggerComplicationUpdates(applicationContext)
                try {
                    TileService.getUpdater(applicationContext).requestUpdate(MarketWearPortfolioTileService::class.java)
                } catch (_: Exception) {
                    // Ignored
                }
            }
        }
    }

    private fun handlePortfolioConfig(dataMap: DataMap) {
        val payload = dataMap.getString(WearConstants.KEY_DISABLED_HOLDING_IDS)
        val weightsPayload = dataMap.getString(WearConstants.KEY_CUSTOM_WEIGHTS)
        if (payload == null && weightsPayload == null) return
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

                val cachedIntraday = selectionStore.cachedTickerIntradayData.value
                if (cachedIntraday.isNotEmpty()) {
                    val recomputed = PortfolioRegistry.computePortfolioQuote(
                        dataByYahooTicker = cachedIntraday,
                        timestampMillis = System.currentTimeMillis(),
                        disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                        customWeights = selectionStore.customWeights.value
                    )
                    db.indexQuoteDao().insertOrUpdate(IndexQuoteEntity.fromDomainModel(recomputed))
                    liveNotificationManager.showOrUpdateFromQuotes(
                        quotes = listOf(recomputed),
                        disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                        customWeights = selectionStore.customWeights.value
                    )
                    MarketSyncWorker.triggerComplicationUpdates(applicationContext)
                }
            } catch (_: Exception) {
                // Ignore malformed config payload
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
