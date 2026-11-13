package com.marketwear.sharedui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.theme.MarketWearTheme
import platform.UIKit.UIViewController

/**
 * iOS UIKit entrypoint hosting the shared Compose Multiplatform [MarketDashboardBody] in Dark Mode.
 */
fun MarketWearIosViewController(): UIViewController = ComposeUIViewController {
    var weightMode by remember { mutableStateOf(PortfolioWeightMode.NORMALIZED_100) }
    val quotes = remember { MarketRegistry.getFallbackQuotes() }
    val dashboard = remember(quotes, weightMode) {
        MarketUiStateBuilder.buildDashboardState(
            quotes = quotes,
            weightMode = weightMode
        )
    }

    MarketWearTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MarketDashboardBody(
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                onToggleWeightMode = { weightMode = weightMode.next() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}
