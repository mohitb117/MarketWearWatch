package com.marketwear.mobile.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MobileWearableListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = MobileMarketRepository()
    private val syncManager by lazy { WearDataSyncManager(applicationContext) }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearConstants.PATH_REQUEST_REFRESH) {
            serviceScope.launch {
                val quotes = repository.fetchAllQuotes()
                syncManager.syncQuotesToWatch(quotes)
            }
        }
    }
}
