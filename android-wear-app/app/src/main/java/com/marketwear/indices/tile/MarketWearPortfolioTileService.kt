package com.marketwear.indices.tile

import android.content.ComponentName
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.marketwear.indices.MainActivity
import com.marketwear.indices.data.MarketRepository
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.TrendDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Wear OS Tile providing the at-a-glance high-level Portfolio Score preview widget
 * on the watch face carousel.
 */
class MarketWearPortfolioTileService : TileService(), KoinComponent {

    private val repository: MarketRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val RESOURCES_VERSION = "1"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val future = ResolvableFuture.create<TileBuilders.Tile>()
        serviceScope.launch {
            try {
                val tile = buildTile(requestParams)
                future.set(tile)
            } catch (e: Throwable) {
                future.setException(e)
            }
        }
        return future
    }

    private suspend fun buildTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val dashboard = repository.dashboardFlow.value
        val cardState = dashboard.portfolioState
        val spx = dashboard.indices.firstOrNull { it.symbol == "SPX" || it.symbol == "^GSPC" }
        val ndx = dashboard.indices.firstOrNull { it.symbol == "NDX" || it.symbol == "^NDX" }
        val spxText = spx?.formattedPercent ?: "+0.00%"
        val ndxText = ndx?.formattedPercent ?: "+0.00%"

        val scoreText = cardState.activeQuote.formattedScore
        val deltaText = cardState.activeQuote.formattedPercent
        val isPositive = cardState.trendDirection == TrendDirection.POSITIVE

        val accentColor = if (isPositive) 0xFF00E676.toInt() else 0xFFFF3B30.toInt()
        val pillBgColor = if (isPositive) 0xFF062A1B.toInt() else 0xFF2B0C11.toInt()

        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        val tileClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(launchAction)
            .build()

        val rootModifier = ModifiersBuilders.Modifiers.Builder()
            .setClickable(tileClickable)
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(argb(0xFF06090E.toInt()))
                    .build()
            )
            .build()

        val arrowGlyph = if (isPositive) "↗" else "↘"

        // Center Content: Large Score + Delta Chip + Subtext
        val contentColumn = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            // Label / Header
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("MARKETWEAR · 37/37")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(10f))
                            .setColor(argb(0xFF8B9BB4.toInt()))
                            .build()
                    )
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
            // Big Tabular Hero Score
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(scoreText)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(32f))
                            .setColor(argb(0xFFF1F5F9.toInt()))
                            .build()
                    )
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
            // Trend Delta Pill
            .addContent(
                LayoutElementBuilders.Box.Builder()
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setBackground(
                                ModifiersBuilders.Background.Builder()
                                    .setColor(argb(pillBgColor))
                                    .setCorner(
                                        ModifiersBuilders.Corner.Builder()
                                            .setRadius(dp(8f))
                                            .build()
                                    )
                                    .build()
                            )
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setStart(dp(8f))
                                    .setEnd(dp(8f))
                                    .setTop(dp(2f))
                                    .setBottom(dp(2f))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("$arrowGlyph $deltaText")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(accentColor))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
            // Secondary Range / Index Summary
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("SPX $spxText · NDX $ndxText")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(10f))
                            .setColor(argb(0xFF8B9BB4.toInt()))
                            .build()
                    )
                    .build()
            )
            .build()

        val deviceParams = requestParams.deviceConfiguration ?: androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(225)
            .setScreenHeightDp(225)
            .build()

        val layout = PrimaryLayout.Builder(deviceParams)
            .setPrimaryLabelTextContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Portfolio Score")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(11f))
                            .setColor(argb(0xFF38BDF8.toInt()))
                            .build()
                    )
                    .build()
            )
            .setContent(contentColumn)
            .setPrimaryChipContent(
                CompactChip.Builder(this, "Open App", tileClickable, deviceParams)
                    .build()
            )
            .build()

        val rootBox = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(rootModifier)
            .addContent(layout)
            .build()

        val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(
                LayoutElementBuilders.Layout.Builder()
                    .setRoot(rootBox)
                    .build()
            )
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(60_000L) // 1-minute freshness interval
            .build()
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val future = ResolvableFuture.create<ResourceBuilders.Resources>()
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        future.set(resources)
        return future
    }
}
