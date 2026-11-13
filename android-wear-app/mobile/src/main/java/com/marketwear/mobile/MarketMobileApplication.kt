package com.marketwear.mobile

import android.app.Application
import com.marketwear.mobile.di.mobileModule
import com.marketwear.mobile.sync.MobileSyncWorker
import com.marketwear.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

class MarketMobileApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(extraModules = listOf(mobileModule)) {
            androidContext(this@MarketMobileApplication)
        }
        // Automatically schedule 30-minute background sync to watch
        MobileSyncWorker.schedulePeriodicSync(this)
    }
}
