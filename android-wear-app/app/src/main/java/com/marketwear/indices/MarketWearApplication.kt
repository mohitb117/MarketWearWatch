package com.marketwear.indices

import android.app.Application
import com.marketwear.indices.di.wearModule
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.indices.sync.WatchMarketSyncHub
import com.marketwear.shared.di.initKoin
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext

class MarketWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(extraModules = listOf(wearModule)) {
            androidContext(this@MarketWearApplication)
        }
        // Initialize Single Source of Truth reactive hub across complications, tiles, and notifications
        getKoin().get<WatchMarketSyncHub>()
        // Ensure 15-minute background sync is initialized
        MarketSyncWorker.schedulePeriodicSync(this)
    }
}
