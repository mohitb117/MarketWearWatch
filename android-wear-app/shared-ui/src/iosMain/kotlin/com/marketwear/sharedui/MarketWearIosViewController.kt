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
import com.marketwear.shared.ui.CoreMarketState
import com.marketwear.shared.ui.MarketStateReducer
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.ThemeAppearance
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.theme.MarketWearTheme
import platform.UIKit.UIViewController

/**
 * iOS UIKit entrypoint hosting the shared Compose Multiplatform [MarketDashboardBody] in Dark Mode.
 */
fun MarketWearIosViewController(): UIViewController = ComposeUIViewController {
    var coreState by remember { mutableStateOf(CoreMarketState()) }
    val quotes = remember { MarketRegistry.getFallbackQuotes() }
    val dashboard = remember(quotes, coreState) {
        MarketUiStateBuilder.buildDashboardState(
            quotes = quotes,
            weightMode = coreState.weightMode,
            disabledHoldingIds = coreState.disabledHoldingIds,
            customWeights = coreState.customWeights
        )
    }

    MarketWearTheme(appearance = ThemeAppearance.DARK) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MarketDashboardBody(
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                onIntent = { intent -> coreState = MarketStateReducer.reduceCoreIntent(coreState, intent) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}
