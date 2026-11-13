package com.marketwear.mobile.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MobileWearableListenerService : WearableListenerService(), KoinComponent {

    private val repository: MobileMarketRepository by inject()
    private val syncManager: WearDataSyncManager by inject()

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
