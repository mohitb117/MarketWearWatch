package com.marketwear.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.marketwear.desktop.ui.DesktopDashboardScreen
import com.marketwear.sharedui.theme.MarketWearTheme

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1180.dp, 820.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "MarketWear — KMP Desktop Portfolio Score & Watch Simulator"
    ) {
        MarketWearTheme {
            DesktopDashboardScreen()
        }
    }
}
