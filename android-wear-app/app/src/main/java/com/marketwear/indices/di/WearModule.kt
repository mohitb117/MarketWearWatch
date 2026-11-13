package com.marketwear.indices.di

import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.ui.MarketViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val wearModule = module {
    single { MarketDatabase.getInstance(androidContext()) }
    single { get<MarketDatabase>().indexQuoteDao() }
    single {
        MarketRepository(
            context = androidContext(),
            dao = get(),
            sharedClient = get()
        )
    }
    viewModel {
        MarketViewModel(
            application = androidApplication(),
            repository = get()
        )
    }
}
