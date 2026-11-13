package com.marketwear.indices

import android.app.Application
import com.marketwear.indices.di.wearModule
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

class MarketWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(extraModules = listOf(wearModule)) {
            androidContext(this@MarketWearApplication)
        }
        // Ensure 15-minute background sync is initialized
        MarketSyncWorker.schedulePeriodicSync(this)
    }
}
