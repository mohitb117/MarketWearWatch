package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.HoldingSelectionPreset
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.NotificationExpansionMode
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioLiveUpdateSpec
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme

/**
 * Visual density mode for [PortfolioFluctuationBar].
 */
enum class FluctuationBarDensity {
    STANDARD,
    COMPACT
}

/**
 * 50%-centered bipolar portfolio fluctuation bar mirroring the Android 16 Progress-Centric
 * Notification: gains extend rightward above 50% in intensity-scaled green, while losses
 * extend leftward below 50% in intensity-scaled red.
 */
@Composable
fun PortfolioFluctuationBar(
    spec: PortfolioLiveUpdateSpec,
    modifier: Modifier = Modifier,
    density: FluctuationBarDensity = FluctuationBarDensity.STANDARD
) {
    val accentColor = Color(spec.accentArgb)
    val trackHeight = when (density) {
        FluctuationBarDensity.COMPACT -> 8.dp
        FluctuationBarDensity.STANDARD -> 10.dp
    }
    val canvasHeight = when (density) {
        FluctuationBarDensity.COMPACT -> 14.dp
        FluctuationBarDensity.STANDARD -> 18.dp
    }
    val dotDp = when (density) {
        FluctuationBarDensity.COMPACT -> 4.dp
        FluctuationBarDensity.STANDARD -> 5.dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val barH = trackHeight.toPx()
            val barTop = (h - barH) / 2f
            val corner = barH / 2f

            // Background rounded capsule
            drawRoundRect(
                color = MarketWearPalette.TrackBackground,
                topLeft = Offset(0f, barTop),
                size = Size(w, barH),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Proportional bipolar segments clipped cleanly inside rounded capsule bounds
            val totalUnits = spec.segments.sumOf { it.length }.coerceAtLeast(1).toFloat()
            val pillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = barTop,
                        right = w,
                        bottom = barTop + barH,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                )
            }
            clipPath(pillPath) {
                var cursorX = 0f
                for (segment in spec.segments) {
                    val segWidth = (segment.length / totalUnits) * w
                    if (segWidth > 0f) {
                        drawRect(
                            color = Color(segment.argbColor),
                            topLeft = Offset(cursorX, barTop),
                            size = Size(segWidth, barH)
                        )
                        cursorX += segWidth
                    }
                }
            }

            // Symmetric log-linear milestone ticks (±1%, ±5%, ±25%)
            val midX = w * (spec.baselinePoint.toFloat() / spec.maxProgress.coerceAtLeast(1))
            val scaleTickColor = Color(0xFF64748B).copy(alpha = 0.45f)
            val scaleTickStroke = 1.dp.toPx()
            for (tickPct in PortfolioLiveUpdateFormatter.LOG_SCALE_TICK_PERCENTS) {
                val halfFrac = PortfolioLiveUpdateFormatter.logLinearHalfFraction(tickPct).toFloat()
                val leftTickX = midX * (1f - halfFrac)
                val rightTickX = midX * (1f + halfFrac)
                drawLine(
                    color = scaleTickColor,
                    start = Offset(leftTickX, barTop),
                    end = Offset(leftTickX, barTop + barH),
                    strokeWidth = scaleTickStroke
                )
                drawLine(
                    color = scaleTickColor,
                    start = Offset(rightTickX, barTop),
                    end = Offset(rightTickX, barTop + barH),
                    strokeWidth = scaleTickStroke
                )
            }

            // Crisp 50% morning baseline tick
            val tickWidth = 2.dp.toPx()
            drawLine(
                color = MarketWearPalette.BaselineTick,
                start = Offset(midX, (barTop - 2.dp.toPx()).coerceAtLeast(0f)),
                end = Offset(midX, (barTop + barH + 2.dp.toPx()).coerceAtMost(h)),
                strokeWidth = tickWidth
            )

            // Active tracker indicator halo + core dot
            val trackerX = (w * (spec.progress.toFloat() / spec.maxProgress.coerceAtLeast(1)))
                .coerceIn(barH / 2f, (w - barH / 2f).coerceAtLeast(barH / 2f))
            val centerY = h / 2f
            val dotRadius = dotDp.toPx()
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = dotRadius * 1.55f,
                center = Offset(trackerX, centerY)
            )
            drawCircle(
                color = accentColor,
                radius = dotRadius,
                center = Offset(trackerX, centerY)
            )
        }

        if (density == FluctuationBarDensity.STANDARD) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-25%+",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-5%",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "0% · ${spec.baselineLabel}",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+5%",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+25%+",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LiveUpdateNotificationPreviewCard(
    spec: PortfolioLiveUpdateSpec,
    onIntent: (MarketIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    initialMode: NotificationExpansionMode = NotificationExpansionMode.EXPANDED,
    selectedMode: NotificationExpansionMode? = null,
    onSelectMode: ((NotificationExpansionMode) -> Unit)? = null
) {
    var localMode by remember { mutableStateOf(initialMode) }
    val mode = selectedMode ?: localMode
    val updateMode: (NotificationExpansionMode) -> Unit = { next ->
        localMode = next
        onSelectMode?.invoke(next)
    }
    val accentColor = Color(spec.accentArgb)
    val arrowGlyph = when (spec.momentumBand) {
        PortfolioMomentumBand.FLAT -> "→"
        PortfolioMomentumBand.MILD_GAIN,
        PortfolioMomentumBand.MODERATE_GAIN,
        PortfolioMomentumBand.STRONG_GAIN -> "↗"
        PortfolioMomentumBand.MILD_LOSS,
        PortfolioMomentumBand.MODERATE_LOSS,
        PortfolioMomentumBand.STRONG_LOSS -> "↘"
    }

    Card(
        shape = MarketWearShapes.MetricCard,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top bar: Simulated Status Bar Mini Chip (9:30 [ ↗ +0.84% ]) + Collapsed/Expanded Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "9:30",
                        style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Status bar directional arrow mini chip tile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(MarketWearShapes.TogglePill)
                            .background(accentColor)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = arrowGlyph,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF06130D)
                        )
                        Text(
                            text = spec.shortCriticalText,
                            style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF06130D)
                        )
                    }
                    Text(
                        text = "· ${spec.subText}",
                        style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Collapsed / Expanded toggle pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NotificationExpansionMode.entries.forEach { entry ->
                        val isSelected = mode == entry
                        val pillBg = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        val pillFg = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val label = when (entry) {
                            NotificationExpansionMode.COLLAPSED -> "Collapsed"
                            NotificationExpansionMode.EXPANDED -> "Expanded"
                        }
                        Box(
                            modifier = Modifier
                                .clip(MarketWearShapes.TogglePill)
                                .background(pillBg)
                                .clickable { updateMode(entry) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillFg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (mode) {
                NotificationExpansionMode.COLLAPSED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateMode(NotificationExpansionMode.EXPANDED) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = spec.collapsedTitle,
                                style = MaterialTheme.typography.titleSmall.merge(MarketWearTextStyles.Tabular),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = spec.collapsedSubtitle,
                                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "⌄",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    PortfolioFluctuationBar(
                        spec = spec,
                        density = FluctuationBarDensity.COMPACT
                    )
                }

                NotificationExpansionMode.EXPANDED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateMode(NotificationExpansionMode.COLLAPSED) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = spec.expandedTitle,
                                style = MaterialTheme.typography.titleSmall.merge(MarketWearTextStyles.Tabular),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = spec.expandedSubtitle,
                                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "⌃",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PortfolioFluctuationBar(
                        spec = spec,
                        density = FluctuationBarDensity.STANDARD
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NotificationActionPill(
                            label = "100% / Raw",
                            onClick = { onIntent(MarketIntent.ToggleWeightMode) }
                        )
                        NotificationActionPill(
                            label = "All 37",
                            onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)) }
                        )
                        NotificationActionPill(
                            label = "Top 10",
                            onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)) }
                        )
                        NotificationActionPill(
                            label = "Sync",
                            onClick = { onIntent(MarketIntent.RequestRefresh(RefreshPolicy.FORCE_CORE_QUOTES)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationActionPill(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MarketWearShapes.TogglePill)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MarketWearShapes.TogglePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
fun PortfolioFluctuationBarPreview() {
    val quote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L)
    val spec = PortfolioLiveUpdateFormatter.buildSpec(quote)
    MarketWearTheme {
        PortfolioFluctuationBar(spec = spec)
    }
}
