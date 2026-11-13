package com.marketwear.mobile

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.marketwear.mobile.appfunctions.MarketAppFunctions
import com.marketwear.mobile.di.mobileModule
import com.marketwear.mobile.sync.MobileSyncWorker
import com.marketwear.shared.di.initKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class MarketMobileApplication : Application(), AppFunctionConfiguration.Provider {

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(MarketAppFunctions::class.java) {
                get<MarketAppFunctions>()
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        initKoin(extraModules = listOf(mobileModule)) {
            androidContext(this@MarketMobileApplication)
        }
        // Automatically schedule 30-minute background sync to watch
        MobileSyncWorker.schedulePeriodicSync(this)
    }
}
