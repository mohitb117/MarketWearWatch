package com.marketwear.mobile.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MobileWearableListenerService : WearableListenerService() {

    private val repository by lazy { MobileMarketRepository.getInstance() }
    private val syncManager by lazy { WearDataSyncManager(applicationContext) }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearConstants.PATH_REQUEST_REFRESH) {
            // Run synchronously on the background binder thread so Google Play Services
            // maintains its WakeLock until the fetch and data push complete.
            runBlocking(Dispatchers.IO) {
                try {
                    val quotes = repository.fetchAllQuotes()
                    syncManager.syncQuotesToWatch(quotes)
                } catch (_: Exception) {
                    // Ignored on message failure
                }
            }
        }
    }
}
