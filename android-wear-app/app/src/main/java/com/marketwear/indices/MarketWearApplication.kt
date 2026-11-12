package com.marketwear.indices

import android.app.Application
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.sync.MarketSyncWorker

class MarketWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Ensure repository and 15-minute background sync are initialized
        MarketRepository.getInstance(this)
        MarketSyncWorker.schedulePeriodicSync(this)
    }
}
