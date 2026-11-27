package com.marketwear.indices.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.marketwear.indices.MainActivity
import com.marketwear.indices.R
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioLiveUpdateSpec
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import kotlin.math.abs

/**
 * Posts an expandable, graphic-rich ongoing portfolio notification directly on the Wear OS watch
 * (`.setLocalOnly(true)`) using [NotificationCompat.BigPictureStyle] with a high-DPI rendered
 * Obsidian Terminal graphic card (intraday dual-curve sparkline + 50%-centered log-linear green/red
 * gauge bar + Normalized/Raw/SPX/NDX metric grid) — ZERO ASCII art — and TWO separate action buttons:
 *  1. "Open on Watch" -> opens [MainActivity] directly on the watch.
 *  2. "Open on Phone" -> sends [com.marketwear.shared.contracts.WearConstants.PATH_OPEN_PHONE_APP]
 *     via [WatchOpenOnPhoneReceiver] to launch the companion app on the paired phone.
 */
class WatchLiveNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        private const val LEGACY_CHANNEL_V1 = "marketwear_watch_portfolio_live"
        private const val LEGACY_CHANNEL_V2 = "marketwear_watch_portfolio_live_v2"
        private const val LEGACY_CHANNEL_V3 = "marketwear_watch_portfolio_live_v3"
        const val CHANNEL_ID = "marketwear_watch_portfolio_live_v4"
        const val CHANNEL_NAME = "Watch Portfolio Live Score"
        const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE_OPEN_WATCH = 2001
        private const val REQUEST_CODE_OPEN_PHONE = 2002

        @DrawableRes
        fun statusArrowIconRes(band: PortfolioMomentumBand): Int = when (band) {
            PortfolioMomentumBand.FLAT -> R.drawable.ic_stat_arrow_flat
            PortfolioMomentumBand.MILD_GAIN,
            PortfolioMomentumBand.MODERATE_GAIN,
            PortfolioMomentumBand.STRONG_GAIN -> R.drawable.ic_stat_arrow_up
            PortfolioMomentumBand.MILD_LOSS,
            PortfolioMomentumBand.MODERATE_LOSS,
            PortfolioMomentumBand.STRONG_LOSS -> R.drawable.ic_stat_arrow_down
        }

        @DrawableRes
        fun trackerIconRes(band: PortfolioMomentumBand): Int = when (band) {
            PortfolioMomentumBand.FLAT -> R.drawable.ic_tracker_flat
            PortfolioMomentumBand.MILD_GAIN,
            PortfolioMomentumBand.MODERATE_GAIN,
            PortfolioMomentumBand.STRONG_GAIN -> R.drawable.ic_tracker_up
            PortfolioMomentumBand.MILD_LOSS,
            PortfolioMomentumBand.MODERATE_LOSS,
            PortfolioMomentumBand.STRONG_LOSS -> R.drawable.ic_tracker_down
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showOrUpdateFromDashboard(dashboard: com.marketwear.shared.ui.MarketDashboardState) {
        if (!isNotificationPermissionGranted()) return
        val cardState = dashboard.portfolioState
        val spx = dashboard.indices.firstOrNull { it.symbol == "SPX" || it.symbol == "^GSPC" }
        val ndx = dashboard.indices.firstOrNull { it.symbol == "NDX" || it.symbol == "^NDX" }

        val spec = cardState.liveUpdateSpec
        val arrowGlyph = when (cardState.trendDirection) {
            TrendDirection.POSITIVE -> "↗"
            TrendDirection.NEGATIVE -> "↘"
        }

        val collapsedTitle = "Portfolio ${cardState.activeQuote.formattedScore} · $arrowGlyph ${spec.shortCriticalText}"
        val collapsedSummary = buildList {
            add("${cardState.watchSecondaryLabel} (${cardState.secondaryQuote.formattedPercent})")
            if (spx != null) add("SPX ${spx.formattedPercent}")
            if (ndx != null) add("NDX ${ndx.formattedPercent}")
        }.joinToString(" · ")

        val expandedTitle = "Portfolio ${cardState.activeQuote.formattedScore} (${cardState.activeQuote.formattedPercent})"
        val expandedSummary = buildList {
            add("${cardState.watchSecondaryLabel} (${cardState.secondaryQuote.formattedPercent})")
            add("${cardState.enabledHoldingsCount}/${cardState.totalHoldingsCount} Holdings")
        }.joinToString(" · ")

        postWatchNotification(
            spec = spec,
            collapsedTitle = collapsedTitle,
            collapsedText = collapsedSummary,
            expandedTitle = expandedTitle,
            expandedSummary = expandedSummary,
            badgeBitmap = renderWatchBadgeBitmap(cardState),
            expandedCardBitmap = renderWatchExpandedCardBitmap(cardState, spx, ndx)
        )
    }

    fun showOrUpdateFromQuotes(
        quotes: List<IndexQuote>,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ) {
        if (!isNotificationPermissionGranted()) return
        if (quotes.none { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }) return
        val dashboard = MarketUiStateBuilder.buildDashboardState(
            quotes = quotes,
            weightMode = weightMode,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        showOrUpdateFromDashboard(dashboard)
    }

    fun showOrUpdate(spec: PortfolioLiveUpdateSpec) {
        if (!isNotificationPermissionGranted()) return
        val fallbackQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L)
        val fallbackCard = MarketUiStateBuilder.buildPortfolioCardState(fallbackQuote)
        postWatchNotification(
            spec = spec,
            collapsedTitle = spec.collapsedTitle,
            collapsedText = spec.collapsedSubtitle,
            expandedTitle = spec.expandedTitle,
            expandedSummary = spec.subText,
            badgeBitmap = renderWatchBadgeBitmap(fallbackCard),
            expandedCardBitmap = renderWatchExpandedCardBitmap(fallbackCard, null, null)
        )
    }

    private fun postWatchNotification(
        spec: PortfolioLiveUpdateSpec,
        collapsedTitle: String,
        collapsedText: String,
        expandedTitle: String,
        expandedSummary: String,
        badgeBitmap: Bitmap,
        expandedCardBitmap: Bitmap
    ) {
        ensureChannelCreated()

        val watchLaunchIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_WATCH,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TARGET_INDEX, PortfolioRegistry.PORTFOLIO_SYMBOL)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openOnPhoneIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_OPEN_PHONE,
            Intent(context, WatchOpenOnPhoneReceiver::class.java).apply {
                action = WatchOpenOnPhoneReceiver.ACTION_OPEN_ON_PHONE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val arrowIconRes = statusArrowIconRes(spec.momentumBand)

        val bigPictureStyle = NotificationCompat.BigPictureStyle()
            .bigPicture(expandedCardBitmap)
            .bigLargeIcon( null as Bitmap? )
            .setBigContentTitle(expandedTitle)
            .setSummaryText(expandedSummary)

        val openOnWatchAction = NotificationCompat.Action.Builder(
            arrowIconRes,
            "Open on Watch",
            watchLaunchIntent
        ).build()

        val openOnPhoneAction = NotificationCompat.Action.Builder(
            R.drawable.ic_trend_up,
            "Open on Phone",
            openOnPhoneIntent
        ).build()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(arrowIconRes)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(collapsedTitle)
            .setContentText(collapsedText)
            .setSubText(spec.subText)
            .setStyle(bigPictureStyle)
            .setColor(spec.accentArgb)
            .setOngoing(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(watchLaunchIntent)
            .addAction(openOnWatchAction)
            .addAction(openOnPhoneAction)

        // Attach Wear OS OngoingActivity metadata so Wear OS SystemUI registers this as an
        // official ongoing activity (pinned at the top of the notification tray + watch face indicator)
        // while keeping the expandable BigPictureStyle graphic card and 2 separate action buttons.
        runCatching {
            val ongoingStatus = Status.Builder()
                .addTemplate(collapsedTitle)
                .build()
            val ongoingActivity = OngoingActivity.Builder(
                context,
                NOTIFICATION_ID,
                notificationBuilder
            )
                .setStaticIcon(arrowIconRes)
                .setTouchIntent(watchLaunchIntent)
                .setStatus(ongoingStatus)
                .build()
            ongoingActivity.apply(context)
        }

        val notification = notificationBuilder.build()
        runCatching {
            WatchLiveForegroundService.startOrUpdate(context, notification)
        }.onFailure {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Renders a high-resolution 420x252 Obsidian Terminal graphic card for the Wear OS expanded
     * notification view:
     * - Top header: Mode pill (`100% NORM` / `RAW`) + active holdings (`37/37`) + Day Range
     * - Middle telemetry canvas: 4-pass dual-curve intraday sparkline (Normalized solid + Raw dashed + 100.0 baseline)
     * - Center-origin 50%-centered log-linear green/red fluctuation gauge bar with glowing tracker dot & scale ticks (`-25%`, `-5%`, `100.0`, `+5%`, `+25%`)
     * - Bottom 2-column metric tiles: `SPX` and `NDX` live prices & percentages
     */
    private fun renderWatchExpandedCardBitmap(
        cardState: PortfolioCardUiState,
        spx: IndexQuote?,
        ndx: IndexQuote?
    ): Bitmap {
        val width = 420
        val height = 252
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isGain = cardState.trendDirection == TrendDirection.POSITIVE
        val accentColor = if (isGain) 0xFF00E676.toInt() else 0xFFFF3B30.toInt()
        val accentGlowColor = if (isGain) 0x3800E676 else 0x38FF3B30
        val pillBgColor = if (isGain) 0x2400E676 else 0x24FF3B30

        // 1. Obsidian Card Background & Border
        val cardRect = RectF(2f, 2f, width - 2f, height - 2f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                0xFF0C111A.toInt(),
                0xFF06090E.toInt(),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1F2B3E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        val boldSans = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val mediumSans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // 2. Top Header: Mode Pill + Delta Pill + Day Range
        val modePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillBgColor
            style = Paint.Style.FILL
        }
        val modeLabel = if (cardState.weightMode == PortfolioWeightMode.NORMALIZED_100) {
            "100% Normalized"
        } else {
            "Raw Portfolio"
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 18f
            typeface = boldSans
        }
        val pillWidth = labelPaint.measureText(modeLabel) + 24f
        canvas.drawRoundRect(RectF(20f, 16f, 20f + pillWidth, 44f), 14f, 14f, modePillPaint)
        canvas.drawText(modeLabel, 32f, 36f, labelPaint)

        val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 18f
            typeface = mediumSans
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(cardState.mobileRangeSummary, width - 20f, 36f, rangePaint)

        // 3. Intraday Dual-Curve Sparkline Canvas (y = 54..132)
        val chartLeft = 22f
        val chartRight = width - 22f
        val chartTop = 54f
        val chartBottom = 128f
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val primaryPoints = cardState.primarySparkline
        val secondaryPoints = cardState.secondarySparkline
        val allPoints = (primaryPoints + secondaryPoints + listOf(100.0)).filter { !it.isNaN() }
        val minVal = (allPoints.minOrNull() ?: 99.5).coerceAtMost(99.85)
        val maxVal = (allPoints.maxOrNull() ?: 100.5).coerceAtLeast(100.15)
        val valSpan = (maxVal - minVal).coerceAtLeast(0.10)

        fun valueToY(v: Double): Float {
            val norm = ((v - minVal) / valSpan).toFloat().coerceIn(0f, 1f)
            return chartBottom - norm * chartH
        }

        // 100.0 Morning Baseline Dashed Line
        val baselineY = valueToY(100.0)
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x5594A3B8
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
        }
        canvas.drawLine(chartLeft, baselineY, chartRight, baselineY, baselinePaint)

        // Secondary Comparison Curve (Dashed)
        if (secondaryPoints.size >= 2) {
            val secPath = Path()
            secondaryPoints.forEachIndexed { idx, v ->
                val x = chartLeft + (idx.toFloat() / (secondaryPoints.size - 1).coerceAtLeast(1)) * chartW
                val y = valueToY(v)
                if (idx == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
            }
            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x7794A3B8
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawPath(secPath, secPaint)
        }

        // Primary Curve + Gradient Fill + End Beacon
        if (primaryPoints.size >= 2) {
            val linePath = Path()
            val fillPath = Path()
            var lastX = chartRight
            var lastY = baselineY

            primaryPoints.forEachIndexed { idx, v ->
                val x = chartLeft + (idx.toFloat() / (primaryPoints.size - 1).coerceAtLeast(1)) * chartW
                val y = valueToY(v)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, chartBottom)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                lastX = x
                lastY = y
            }
            fillPath.lineTo(lastX, chartBottom)
            fillPath.close()

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    chartTop,
                    0f,
                    chartBottom,
                    accentGlowColor,
                    0x00000000,
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
            }
            canvas.drawPath(fillPath, fillPaint)

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 3.6f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(linePath, linePaint)

            val glowDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentGlowColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(lastX, lastY, 9f, glowDotPaint)
            val solidDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(lastX, lastY, 4.5f, solidDotPaint)
        }

        // 4. 50%-Centered Log-Linear Fluctuation Gauge Bar (y = 144..158)
        drawCenterGaugeOnCanvas(
            canvas = canvas,
            percentChange = cardState.activeQuote.percentChange,
            accentColor = accentColor,
            left = chartLeft,
            right = chartRight,
            barTop = 144f,
            barBottom = 156f,
            drawTickLabels = true
        )

        // 5. Bottom Two-Column Index Pills (SPX & NDX) (y = 186..236)
        val tileTop = 188f
        val tileBottom = 236f
        val tileGap = 12f
        val tileWidth = (chartW - tileGap) / 2f

        drawMiniMetricTile(
            canvas = canvas,
            rect = RectF(chartLeft, tileTop, chartLeft + tileWidth, tileBottom),
            label = spx?.let { "S&P 500" } ?: "RAW SCORE",
            valueText = spx?.formattedPoints ?: cardState.unnormalizedQuote.formattedScore,
            deltaText = spx?.formattedPercent ?: cardState.unnormalizedQuote.formattedPercent,
            isPositive = spx?.isPositive ?: (cardState.unnormalizedQuote.change >= 0.0),
            boldTypeface = boldSans
        )

        drawMiniMetricTile(
            canvas = canvas,
            rect = RectF(chartLeft + tileWidth + tileGap, tileTop, chartRight, tileBottom),
            label = ndx?.let { "NASDAQ 100" } ?: "HOLDINGS",
            valueText = ndx?.formattedPoints ?: "${cardState.enabledHoldingsCount}/${cardState.totalHoldingsCount}",
            deltaText = ndx?.formattedPercent ?: "100.0 Base",
            isPositive = ndx?.isPositive ?: true,
            boldTypeface = boldSans
        )

        return bitmap
    }

    private fun drawMiniMetricTile(
        canvas: Canvas,
        rect: RectF,
        label: String,
        valueText: String,
        deltaText: String,
        isPositive: Boolean,
        boldTypeface: Typeface
    ) {
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF172131.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 14f, 14f, tilePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 15f
            typeface = boldTypeface
        }
        canvas.drawText(label, rect.left + 12f, rect.top + 19f, labelPaint)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 18f
            typeface = boldTypeface
        }
        canvas.drawText(valueText, rect.left + 12f, rect.bottom - 8f, valuePaint)

        val deltaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPositive) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
            textSize = 17f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(deltaText, rect.right - 12f, rect.bottom - 8f, deltaPaint)
    }

    /**
     * Renders a crisp 96x96 circular Obsidian Terminal badge bitmap for the collapsed watch
     * notification card showing the intraday sparkline and 50%-centered log-linear gauge.
     */
    private fun renderWatchBadgeBitmap(cardState: PortfolioCardUiState): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = when (cardState.trendDirection) {
            TrendDirection.POSITIVE -> 0xFF00E676.toInt()
            TrendDirection.NEGATIVE -> 0xFFFF3B30.toInt()
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0C111A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1F2B3E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f, ringPaint)

        val points = cardState.primarySparkline
        if (points.size >= 2) {
            val minV = minOf(points.minOrNull() ?: 100.0, 100.0)
            val maxV = maxOf(points.maxOrNull() ?: 100.0, 100.0)
            val span = (maxV - minV).coerceAtLeast(0.05)
            val left = 18f
            val right = size - 18f
            val top = 22f
            val bottom = 54f
            val w = right - left
            val h = bottom - top

            val path = Path()
            points.forEachIndexed { idx, v ->
                val x = left + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * w
                val y = bottom - (((v - minV) / span).toFloat().coerceIn(0f, 1f) * h)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, linePaint)
        }

        drawCenterGaugeOnCanvas(
            canvas = canvas,
            percentChange = cardState.activeQuote.percentChange,
            accentColor = accentColor,
            left = 16f,
            right = size - 16f,
            barTop = 64f,
            barBottom = 72f,
            drawTickLabels = false
        )

        return bitmap
    }

    private fun drawCenterGaugeOnCanvas(
        canvas: Canvas,
        percentChange: Double,
        accentColor: Int,
        left: Float,
        right: Float,
        barTop: Float,
        barBottom: Float,
        drawTickLabels: Boolean
    ) {
        val width = right - left
        val centerX = left + width * 0.5f
        val barHeight = barBottom - barTop
        val barCenterY = (barTop + barBottom) * 0.5f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF202A3A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(left, barTop, right, barBottom), barHeight / 2f, barHeight / 2f, trackPaint)

        // Subtle log-scale tick marks (±1%, ±5%, ±25%)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x5594A3B8
            strokeWidth = 1.5f
        }
        for (tickPct in PortfolioLiveUpdateFormatter.LOG_SCALE_TICK_PERCENTS) {
            val f = PortfolioLiveUpdateFormatter.logLinearHalfFraction(tickPct).toFloat()
            val dx = (width * 0.5f) * f
            canvas.drawLine(centerX - dx, barTop, centerX - dx, barBottom, tickPaint)
            canvas.drawLine(centerX + dx, barTop, centerX + dx, barBottom, tickPaint)
        }

        val halfFraction = PortfolioLiveUpdateFormatter.logLinearHalfFraction(abs(percentChange)).toFloat()
        val fillDistance = (width * 0.5f) * halfFraction
        val trackerX = if (percentChange >= 0.0) {
            (centerX + fillDistance).coerceAtMost(right)
        } else {
            (centerX - fillDistance).coerceAtLeast(left)
        }

        if (fillDistance >= 1.5f) {
            val fillLeft = if (percentChange >= 0.0) centerX else trackerX
            val fillRight = if (percentChange >= 0.0) trackerX else centerX
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(fillLeft, barTop, fillRight, barBottom),
                barHeight / 2f,
                barHeight / 2f,
                fillPaint
            )
        }

        // Center 100.0 baseline marker
        val centerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            strokeWidth = 2.5f
        }
        canvas.drawLine(centerX, barTop - 3f, centerX, barBottom + 3f, centerTickPaint)

        // Glowing tracker thumb on the gauge
        val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(trackerX, barCenterY, barHeight * 0.65f, thumbPaint)

        if (drawTickLabels) {
            val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF94A3B8.toInt()
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("-25%", left, barBottom + 18f, scalePaint)

            scalePaint.textAlign = Paint.Align.CENTER
            val fiveFrac = PortfolioLiveUpdateFormatter.logLinearHalfFraction(5.0).toFloat()
            val fiveDx = (width * 0.5f) * fiveFrac
            canvas.drawText("-5%", centerX - fiveDx, barBottom + 18f, scalePaint)
            canvas.drawText("100.0", centerX, barBottom + 18f, scalePaint)
            canvas.drawText("+5%", centerX + fiveDx, barBottom + 18f, scalePaint)

            scalePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("+25%", right, barBottom + 18f, scalePaint)
        }
    }

    private fun ensureChannelCreated() {
        runCatching {
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_V1)
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_V2)
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_V3)
        }
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Graphical on-wrist portfolio score notification with Open on Watch and Open on Phone actions"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
