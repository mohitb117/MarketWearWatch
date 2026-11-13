package com.marketwear.shared.di

import com.marketwear.shared.network.SharedMarketClient
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin with [sharedModule] and any platform-specific [extraModules].
 */
fun initKoin(
    extraModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
): KoinApplication = startKoin {
    appDeclaration()
    modules(listOf(sharedModule) + extraModules)
}

/**
 * Zero-argument entry point for iOS Swift initialization (`KoinHelperKt.initKoin()`).
 */
fun initKoin() {
    initKoin(extraModules = emptyList())
}

/**
 * Convenience accessor for iOS Swift callers that cannot use reified Kotlin generics.
 */
object SharedDi : KoinComponent {
    val marketClient: SharedMarketClient by inject()
}
