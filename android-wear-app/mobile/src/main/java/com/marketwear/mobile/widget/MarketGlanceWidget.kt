package com.marketwear.mobile.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketDashboardState
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WidgetSizeTier {
    COMPACT_2X2,
    WIDE_RIBBON,
    EXPANDED_FULL;

    companion object {
        fun fromSize(size: DpSize): WidgetSizeTier = when {
            size.height >= 210.dp -> EXPANDED_FULL
            size.width >= 225.dp -> WIDE_RIBBON
            else -> COMPACT_2X2
        }
    }
}

private object WidgetObsidianTokens {
    val Background = Color(0xFF06090E)
    val Surface = Color(0xFF0C111A)
    val SurfaceElevated = Color(0xFF131A26)
    val Primary = Color(0xFF38BDF8)
    val PrimaryContainer = Color(0xFF0E3047)
    val OnPrimaryContainer = Color(0xFFBAE6FD)
    val OnSurface = Color(0xFFF1F5F9)
    val OnSurfaceVariant = Color(0xFF8B9BB4)
    val Positive = Color(0xFF00E676)
    val PositiveContainer = Color(0xFF062A1B)
    val Negative = Color(0xFFFF3B30)
    val NegativeContainer = Color(0xFF2B0C11)
}

class MarketGlanceWidget : GlanceAppWidget(), KoinComponent {

    override val sizeMode: SizeMode = SizeMode.Exact

    private val repository: MobileMarketRepository by inject()
    private val selectionStore: PortfolioSelectionStore by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = repository.snapshotFlow.value
        val rawQuotes = snapshot.quotes
            .ifEmpty { repository.quotes.value }
            .ifEmpty { selectionStore.cachedQuotes.value }
            .ifEmpty { MarketRegistry.getFallbackQuotes() }
        val disabledIds = snapshot.disabledHoldingIds.ifEmpty { selectionStore.disabledHoldingIds.value }
        val customWeights = snapshot.customWeights.ifEmpty { selectionStore.customWeights.value }
        val weightMode = selectionStore.weightMode.value

        val dashboard = MarketUiStateBuilder.buildDashboardState(
            quotes = rawQuotes,
            weightMode = weightMode,
            disabledHoldingIds = disabledIds,
            customWeights = customWeights
        )

        provideContent {
            val size = LocalSize.current
            WidgetContent(
                dashboard = dashboard,
                tier = WidgetSizeTier.fromSize(size)
            )
        }
    }
}

@Composable
private fun WidgetContent(
    dashboard: MarketDashboardState,
    tier: WidgetSizeTier
) {
    val timeStr = if (dashboard.latestUpdatedMillis > 0L) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dashboard.latestUpdatedMillis))
    } else {
        "Live"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetObsidianTokens.Background)
            .cornerRadius(18.dp)
            .padding(if (tier == WidgetSizeTier.COMPACT_2X2) 10.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when (tier) {
            WidgetSizeTier.COMPACT_2X2 -> Compact2x2WidgetView(
                portfolio = dashboard.portfolioState,
                indices = dashboard.indices
            )
            WidgetSizeTier.WIDE_RIBBON -> WideRibbonWidgetView(
                portfolio = dashboard.portfolioState,
                indices = dashboard.indices,
                timeStr = timeStr
            )
            WidgetSizeTier.EXPANDED_FULL -> ExpandedFullWidgetView(
                dashboard = dashboard,
                timeStr = timeStr
            )
        }
    }
}

@Composable
private fun Compact2x2WidgetView(
    portfolio: PortfolioCardUiState,
    indices: List<IndexQuote>
) {
    val spec = portfolio.liveUpdateSpec
    val isPositive = portfolio.trendDirection == TrendDirection.POSITIVE
    val signalColor = if (isPositive) WidgetObsidianTokens.Positive else WidgetObsidianTokens.Negative
    val pillBg = if (isPositive) WidgetObsidianTokens.PositiveContainer else WidgetObsidianTokens.NegativeContainer
    val arrow = arrowGlyphFor(spec.momentumBand)
    val shortModeBadge = when (portfolio.weightMode) {
        PortfolioWeightMode.NORMALIZED_100 -> "100%"
        PortfolioWeightMode.RAW_PORTFOLIO -> "${portfolio.activeRawWeightPercent}%"
    }
    val telemetryBitmap = remember(portfolio) {
        renderPortfolioTelemetryBitmap(portfolio = portfolio, widthPx = 360, heightPx = 104)
    }

    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Row 1: Brand / Title + Holdings count on left, weight mode pill on right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Portfolio",
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurface),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "· ${portfolio.enabledHoldingsCount}/${portfolio.totalHoldingsCount}",
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(WidgetObsidianTokens.SurfaceElevated)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = shortModeBadge,
                    style = TextStyle(
                        color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Row 2: Large Hero Score + Directional Arrow % Pill
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = portfolio.activeQuote.formattedScore,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurface),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(pillBg)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$arrow ${spec.shortCriticalText}",
                    style = TextStyle(
                        color = ColorProvider(signalColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Row 3: 50%-Centered Log-Linear Bipolar Bar + Dual-Curve Sparkline
        Image(
            provider = ImageProvider(telemetryBitmap),
            contentDescription = "Portfolio center-origin gauge and intraday chart",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Row 4: Secondary comparison or SPX/NDX summary line
        val footerText = if (indices.size >= 2) {
            "${portfolio.watchSecondaryLabel} · ${indices[0].symbol} ${indices[0].formattedPercent}"
        } else {
            "${portfolio.watchSecondaryLabel} (${portfolio.secondaryQuote.formattedPercent})"
        }
        Text(
            text = footerText,
            style = TextStyle(
                color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                fontSize = 9.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun WideRibbonWidgetView(
    portfolio: PortfolioCardUiState,
    indices: List<IndexQuote>,
    timeStr: String
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        PortfolioHeroGlanceCard(
            portfolio = portfolio,
            timeStr = timeStr,
            chartHeight = 52.dp
        )
        if (indices.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                indices.take(2).forEachIndexed { idx, quote ->
                    if (idx > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(modifier = GlanceModifier.defaultWeight()) {
                        CompactIndexPill(quote = quote)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedFullWidgetView(
    dashboard: MarketDashboardState,
    timeStr: String
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        PortfolioHeroGlanceCard(
            portfolio = dashboard.portfolioState,
            timeStr = timeStr,
            chartHeight = 64.dp
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(dashboard.indices) { quote ->
                IndexRowCard(quote = quote)
                Spacer(modifier = GlanceModifier.height(5.dp))
            }
            if (dashboard.sectors.isNotEmpty()) {
                item {
                    Text(
                        text = "US Market Sectors",
                        style = TextStyle(
                            color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(dashboard.sectors) { quote ->
                    IndexRowCard(quote = quote)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PortfolioHeroGlanceCard(
    portfolio: PortfolioCardUiState,
    timeStr: String,
    chartHeight: Dp
) {
    val spec = portfolio.liveUpdateSpec
    val isPositive = portfolio.trendDirection == TrendDirection.POSITIVE
    val signalColor = if (isPositive) WidgetObsidianTokens.Positive else WidgetObsidianTokens.Negative
    val pillBg = if (isPositive) WidgetObsidianTokens.PositiveContainer else WidgetObsidianTokens.NegativeContainer
    val arrow = arrowGlyphFor(spec.momentumBand)
    val telemetryBitmap = remember(portfolio) {
        renderPortfolioTelemetryBitmap(portfolio = portfolio, widthPx = 560, heightPx = 128)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetObsidianTokens.Surface)
            .cornerRadius(14.dp)
            .padding(10.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Portfolio · ${portfolio.enabledHoldingsCount}/${portfolio.totalHoldingsCount} Holdings",
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(WidgetObsidianTokens.SurfaceElevated)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = portfolio.mobileModePillLabel,
                    style = TextStyle(
                        color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = portfolio.activeQuote.formattedScore,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurface),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .background(pillBg)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$arrow ${portfolio.activeQuote.formattedChange} (${portfolio.activeQuote.formattedPercent})",
                    style = TextStyle(
                        color = ColorProvider(signalColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = timeStr,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = portfolio.mobileSecondarySummary,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = portfolio.mobileRangeSummary,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurfaceVariant),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Image(
            provider = ImageProvider(telemetryBitmap),
            contentDescription = "Portfolio center-origin gauge and intraday chart",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(chartHeight)
        )
    }
}

@Composable
private fun CompactIndexPill(quote: IndexQuote) {
    val isPositive = quote.change >= 0
    val signalColor = if (isPositive) WidgetObsidianTokens.Positive else WidgetObsidianTokens.Negative
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetObsidianTokens.Surface)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quote.symbol,
            style = TextStyle(
                color = ColorProvider(WidgetObsidianTokens.Primary),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(5.dp))
        Text(
            text = quote.formattedPoints,
            style = TextStyle(
                color = ColorProvider(WidgetObsidianTokens.OnSurface),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = quote.formattedPercent,
            style = TextStyle(
                color = ColorProvider(signalColor),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun IndexRowCard(quote: IndexQuote) {
    val isPositive = quote.change >= 0
    val signalColor = if (isPositive) WidgetObsidianTokens.Positive else WidgetObsidianTokens.Negative
    val pillBg = if (isPositive) WidgetObsidianTokens.PositiveContainer else WidgetObsidianTokens.NegativeContainer

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetObsidianTokens.Surface)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = quote.name,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.OnSurface),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = quote.symbol,
                style = TextStyle(
                    color = ColorProvider(WidgetObsidianTokens.Primary),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
        Text(
            text = quote.formattedPoints,
            style = TextStyle(
                color = ColorProvider(WidgetObsidianTokens.OnSurface),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .background(pillBg)
                .cornerRadius(7.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = quote.formattedPercent,
                style = TextStyle(
                    color = ColorProvider(signalColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

private fun arrowGlyphFor(band: PortfolioMomentumBand): String = when (band) {
    PortfolioMomentumBand.FLAT -> "→"
    PortfolioMomentumBand.MILD_GAIN,
    PortfolioMomentumBand.MODERATE_GAIN,
    PortfolioMomentumBand.STRONG_GAIN -> "↗"
    PortfolioMomentumBand.MILD_LOSS,
    PortfolioMomentumBand.MODERATE_LOSS,
    PortfolioMomentumBand.STRONG_LOSS -> "↘"
}

/**
 * Renders both the 50%-centered log-linear bipolar bar (`spec.segments`) and the 4-pass
 * Obsidian Terminal dual-curve sparkline directly into a crisp bitmap for Glance `RemoteViews`.
 */
private fun renderPortfolioTelemetryBitmap(
    portfolio: PortfolioCardUiState,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val spec = portfolio.liveUpdateSpec
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    // 1. Top section: 50%-centered log-linear bipolar bar
    val barHeight = 12f
    val barTop = 4f
    val barBottom = barTop + barHeight
    val barRadius = barHeight / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.style = Paint.Style.FILL
    paint.color = 0xFF141C27.toInt()
    canvas.drawRoundRect(RectF(0f, barTop, w, barBottom), barRadius, barRadius, paint)

    val totalUnits = spec.segments.sumOf { it.length }.coerceAtLeast(1).toFloat()
    var cursorX = 0f
    val clipPath = Path().apply {
        addRoundRect(RectF(0f, barTop, w, barBottom), barRadius, barRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)
    for (seg in spec.segments) {
        val segW = (seg.length / totalUnits) * w
        if (segW > 0f) {
            paint.color = seg.argbColor
            canvas.drawRect(cursorX, barTop, cursorX + segW, barBottom, paint)
            cursorX += segW
        }
    }
    canvas.restore()

    // Center 50% (100.0 baseline) tick
    val midX = w * (spec.baselinePoint.toFloat() / spec.maxProgress.coerceAtLeast(1))
    paint.color = PortfolioLiveUpdateFormatter.BASELINE_POINT_WHITE
    paint.strokeWidth = 3f
    canvas.drawLine(midX, 1f, midX, barBottom + 3f, paint)

    // Active progress dot
    val trackerX = (w * (spec.progress.toFloat() / spec.maxProgress.coerceAtLeast(1)))
        .coerceIn(barRadius, w - barRadius)
    val barCenterY = (barTop + barBottom) / 2f
    paint.color = (spec.accentArgb and 0x00FFFFFF) or 0x55000000
    canvas.drawCircle(trackerX, barCenterY, 9f, paint)
    paint.color = spec.accentArgb
    canvas.drawCircle(trackerX, barCenterY, 5.5f, paint)

    // 2. Bottom section: 4-pass Obsidian Terminal dual-curve intraday sparkline
    val chartTop = barBottom + 10f
    val chartBottom = h - 4f
    val chartHeight = (chartBottom - chartTop).coerceAtLeast(16f)

    val primary = portfolio.primarySparkline
    val secondary = portfolio.secondarySparkline
    val allValues = primary + secondary + listOf(100.0)
    val minVal = (allValues.minOrNull() ?: 99.0) - 0.08
    val maxVal = (allValues.maxOrNull() ?: 101.0) + 0.08
    val span = (maxVal - minVal).coerceAtLeast(0.20)

    fun yFor(v: Double): Float {
        val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
        return chartTop + (1f - norm) * (chartHeight - 8f) + 4f
    }

    // Pass 1: Dashed 100.0 morning baseline rule
    val baseY = yFor(100.0)
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF64748B.toInt()
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    canvas.drawLine(0f, baseY, w, baseY, basePaint)

    // Pass 2: Dashed secondary curve (Raw vs Normalized)
    if (secondary.size >= 2) {
        val secPath = buildSmoothAndroidPath(secondary, w, ::yFor)
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xB838BDF8.toInt()
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        canvas.drawPath(secPath, secPaint)
    }

    // Pass 3: Smooth primary curve + vertical area gradient fill
    if (primary.size >= 2) {
        val signalArgb = if (portfolio.trendDirection == TrendDirection.POSITIVE) {
            0xFF34D399.toInt()
        } else {
            0xFFFB7185.toInt()
        }
        val strokePath = buildSmoothAndroidPath(primary, w, ::yFor)
        val fillPath = Path(strokePath).apply {
            lineTo(w, chartBottom)
            lineTo(0f, chartBottom)
            close()
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                chartTop,
                0f,
                chartBottom,
                (signalArgb and 0x00FFFFFF) or 0x48000000,
                signalArgb and 0x00FFFFFF,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = signalArgb
        }
        canvas.drawPath(strokePath, strokePaint)

        // Endpoint pulse dot
        val lastY = yFor(primary.last())
        paint.style = Paint.Style.FILL
        paint.color = (signalArgb and 0x00FFFFFF) or 0x48000000
        canvas.drawCircle(w - 5f, lastY, 9f, paint)
        paint.color = signalArgb
        canvas.drawCircle(w - 5f, lastY, 4.5f, paint)
    }

    return bmp
}

private fun buildSmoothAndroidPath(
    values: List<Double>,
    widthPx: Float,
    yFor: (Double) -> Float
): Path {
    val path = Path()
    if (values.isEmpty()) return path
    val lastIdx = (values.size - 1).coerceAtLeast(1)
    fun xFor(idx: Int): Float = (idx.toFloat() / lastIdx) * (widthPx - 6f) + 3f

    var prevX = xFor(0)
    var prevY = yFor(values[0])
    path.moveTo(prevX, prevY)
    for (i in 1 until values.size) {
        val currX = xFor(i)
        val currY = yFor(values[i])
        val midX = (prevX + currX) / 2f
        path.cubicTo(midX, prevY, midX, currY, currX, currY)
        prevX = currX
        prevY = currY
    }
    return path
}
