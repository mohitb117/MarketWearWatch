package com.marketwear.mobile

import android.app.Application
import com.marketwear.mobile.sync.MobileSyncWorker

class MarketMobileApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Automatically schedule 15-minute background sync to watch
        MobileSyncWorker.schedulePeriodicSync(this)
    }
}
