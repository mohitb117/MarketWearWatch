package com.marketwear.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.marketwear.desktop.ui.DesktopDashboardScreen
import com.marketwear.desktop.ui.DesktopMarketIntent
import com.marketwear.desktop.ui.DesktopMarketUiState
import com.marketwear.desktop.ui.DesktopMarketViewModel
import com.marketwear.desktop.ui.DesktopSimulationScenario
import com.marketwear.shared.ui.MacOsMarketBridge
import com.marketwear.shared.ui.MarketDashboardState
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.sharedui.theme.MarketWearTheme
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.Executors

private const val ISLAND_STATE_PATH = "/tmp/marketwear_island_state.json"
private const val ISLAND_BRIDGE_PORT = 49172

private fun DesktopMarketUiState.encodeDashboardJson(): String =
    MarketUiStateBuilder.encodeDashboardJson(
        MarketDashboardState(
            portfolioState = portfolioState,
            indices = indices,
            sectors = sectors,
            latestUpdatedMillis = portfolioState.activeQuote.lastUpdated
        )
    )

private fun startIslandBridgeServer(viewModel: DesktopMarketViewModel): Pair<CoroutineScope, HttpServer?> {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch {
        viewModel.uiState.collect { uiState ->
            runCatching { File(ISLAND_STATE_PATH).writeText(uiState.encodeDashboardJson()) }
        }
    }

    val server = runCatching {
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), ISLAND_BRIDGE_PORT), 0).apply {
            executor = Executors.newCachedThreadPool()
            createContext("/state") { exchange ->
                try {
                    val payload = viewModel.uiState.value.encodeDashboardJson().toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, payload.size.toLong())
                    exchange.responseBody.use { it.write(payload) }
                } catch (e: Exception) {
                    val errorPayload = """{"error": "${e.message?.replace("\"", "\\\"")}"}""".toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(500, errorPayload.size.toLong())
                    exchange.responseBody.use { it.write(errorPayload) }
                }
            }
            createContext("/intent") { exchange ->
                try {
                    val query = exchange.requestURI.rawQuery.orEmpty()
                    val code = query.split('&')
                        .firstOrNull { it.startsWith("code=") }
                        ?.substringAfter("code=")
                        ?.let { URLDecoder.decode(it, "UTF-8") }
                        .orEmpty()

                    val targetState = if (code == "REFRESH") {
                        runBlocking(Dispatchers.IO) {
                            viewModel.refreshLive(RefreshPolicy.FORCE_CORE_QUOTES)
                        }
                    } else {
                        val desktopIntent = when (code) {
                            "SCENARIO_DEFAULT" -> DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.DEFAULT_BASKET)
                            "SCENARIO_BULL" -> DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.BULL_RALLY)
                            "SCENARIO_BEAR" -> DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.BEAR_PULLBACK)
                            "SCENARIO_LIVE" -> DesktopMarketIntent.SelectScenario(DesktopSimulationScenario.LIVE_NETWORK)
                            else -> MacOsMarketBridge.parseActionCode(code)?.let { DesktopMarketIntent.Core(it) }
                        }
                        if (desktopIntent != null) {
                            runBlocking(Dispatchers.Main) {
                                viewModel.onIntent(desktopIntent)
                            }
                        }
                        viewModel.uiState.value
                    }

                    val payload = targetState.encodeDashboardJson().toByteArray()
                    runCatching { File(ISLAND_STATE_PATH).writeText(String(payload, Charsets.UTF_8)) }

                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, payload.size.toLong())
                    exchange.responseBody.use { it.write(payload) }
                } catch (e: Exception) {
                    val errorPayload = """{"error": "${e.message?.replace("\"", "\\\"")}"}""".toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(500, errorPayload.size.toLong())
                    exchange.responseBody.use { it.write(errorPayload) }
                }
            }
            start()
        }
    }.getOrNull()

    return scope to server
}

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1180.dp, 820.dp))
    val viewModel = DesktopMarketViewModel(mainContext = Dispatchers.Main.immediate)
    val (bridgeScope, bridgeServer) = startIslandBridgeServer(viewModel)

    Window(
        onCloseRequest = {
            bridgeServer?.stop(0)
            bridgeScope.cancel()
            viewModel.dispose()
            exitApplication()
        },
        state = windowState,
        title = "MarketWear — KMP Desktop Portfolio Score & Dynamic Island Sync"
    ) {
        MarketWearTheme {
            DesktopDashboardScreen(viewModel = viewModel)
        }
    }
}
