package com.marketwear.indices.di

import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.WatchPortfolioSelectionStore
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.indices.sync.WatchMarketSyncHub
import com.marketwear.indices.ui.MarketViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val wearModule = module {
    single { MarketDatabase.getInstance(androidContext()) }
    single { get<MarketDatabase>().indexQuoteDao() }
    single { WatchPortfolioSelectionStore(androidContext()) }
    single { WatchLiveNotificationManager(androidContext()) }
    single {
        MarketRepository(
            context = androidContext(),
            dao = get(),
            sharedClient = get(),
            selectionStore = get()
        )
    }
    single {
        WatchMarketSyncHub(
            context = androidContext(),
            repository = get(),
            selectionStore = get(),
            liveNotificationManager = get()
        )
    }
    viewModel {
        MarketViewModel(
            application = androidApplication(),
            repository = get(),
            selectionStore = get(),
            liveNotificationManager = get(),
            storeFactory = get()
        )
    }
}
