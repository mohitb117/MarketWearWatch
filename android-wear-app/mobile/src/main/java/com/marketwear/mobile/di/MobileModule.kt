package com.marketwear.mobile.di

import com.marketwear.mobile.data.KtorMarketClient
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.mobile.ui.MobileViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mobileModule = module {
    single { KtorMarketClient(client = get()) }
    single { MobileMarketRepository(client = get()) }
    single { WearDataSyncManager(androidContext()) }
    viewModel {
        MobileViewModel(
            application = androidApplication(),
            repository = get(),
            syncManager = get()
        )
    }
}
