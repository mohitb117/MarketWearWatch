package com.marketwear.indices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.marketwear.shared.model.PortfolioHolding
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.HoldingSelectionPreset
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.WeightAdjustmentStep
import com.marketwear.shared.ui.WeightResetAction
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Navigation mode on Wear OS between toggling holdings on/off and editing % allocation weights.
 */
enum class WatchHoldingsConfigMode {
    TOGGLE_LISTINGS,
    EDIT_WEIGHTS
}

/**
 * Native Wear OS configuration screen letting users toggle any of the 37 predefined
 * UBS & Fidelity holdings on/off or step per-listing % allocation weights with rotary crown scroll.
 */
@Composable
fun PortfolioHoldingsConfigScreen(
    holdings: List<PortfolioHolding>,
    disabledHoldingIds: Set<String>,
    onIntent: (MarketIntent) -> Unit,
    modifier: Modifier = Modifier,
    customWeights: Map<String, Double> = emptyMap()
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    var configMode by remember { mutableStateOf(WatchHoldingsConfigMode.TOGGLE_LISTINGS) }
    val enabledCount = remember(holdings, disabledHoldingIds) {
        holdings.count { it.id !in disabledHoldingIds }
    }
    val activeRawWeight = remember(disabledHoldingIds, customWeights) {
        PortfolioRegistry.rawWeightPercentFor(disabledHoldingIds, customWeights)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(listState),
                    focusRequester = focusRequester
                ),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 26.dp, bottom = 28.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "config_header") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = when (configMode) {
                            WatchHoldingsConfigMode.TOGGLE_LISTINGS -> "Portfolio Holdings"
                            WatchHoldingsConfigMode.EDIT_WEIGHTS -> "Edit Weights (%)"
                        },
                        style = MaterialTheme.typography.title3,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    val customSuffix = if (customWeights.isNotEmpty()) " · ${customWeights.size} custom" else ""
                    Text(
                        text = "$enabledCount/${holdings.size} · ${formatWearWeight(activeRawWeight)}%$customSuffix",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(key = "config_presets") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactChip(
                        onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)) },
                        colors = if (disabledHoldingIds.isEmpty()) {
                            ChipDefaults.primaryChipColors()
                        } else {
                            ChipDefaults.secondaryChipColors()
                        },
                        label = { Text("All ${holdings.size}", fontSize = 11.sp) }
                    )
                    CompactChip(
                        onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)) },
                        colors = if (enabledCount == 10 && disabledHoldingIds == PortfolioRegistry.disabledIdsForTop10()) {
                            ChipDefaults.primaryChipColors()
                        } else {
                            ChipDefaults.secondaryChipColors()
                        },
                        label = { Text("Top 10", fontSize = 11.sp) }
                    )
                }
            }

            item(key = "config_mode_controls") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    CompactChip(
                        onClick = {
                            configMode = when (configMode) {
                                WatchHoldingsConfigMode.TOGGLE_LISTINGS -> WatchHoldingsConfigMode.EDIT_WEIGHTS
                                WatchHoldingsConfigMode.EDIT_WEIGHTS -> WatchHoldingsConfigMode.TOGGLE_LISTINGS
                            }
                        },
                        colors = if (configMode == WatchHoldingsConfigMode.EDIT_WEIGHTS) {
                            ChipDefaults.primaryChipColors()
                        } else {
                            ChipDefaults.secondaryChipColors(backgroundColor = Color(0xFF16202C))
                        },
                        label = {
                            Text(
                                text = when (configMode) {
                                    WatchHoldingsConfigMode.TOGGLE_LISTINGS -> "Edit %"
                                    WatchHoldingsConfigMode.EDIT_WEIGHTS -> "Listings"
                                },
                                fontSize = 11.sp
                            )
                        }
                    )
                    if (customWeights.isNotEmpty()) {
                        CompactChip(
                            onClick = {
                                onIntent(MarketIntent.ResetWeights(WeightResetAction.RESTORE_DEFAULT_WEIGHTS))
                            },
                            colors = ChipDefaults.secondaryChipColors(backgroundColor = Color(0xFF281820)),
                            label = { Text("Reset %", fontSize = 11.sp, color = Color(0xFFFB7185)) }
                        )
                    }
                }
            }

            items(
                items = holdings,
                key = { it.id }
            ) { holding ->
                val isEnabled = holding.id !in disabledHoldingIds
                val canToggle = !(isEnabled && enabledCount <= 1)
                val effectiveWeight = PortfolioRegistry.effectiveWeightPercent(holding, customWeights)
                val isCustomized = holding.id in customWeights

                when (configMode) {
                    WatchHoldingsConfigMode.TOGGLE_LISTINGS -> {
                        ToggleChip(
                            checked = isEnabled,
                            enabled = canToggle,
                            onCheckedChange = { onIntent(MarketIntent.ToggleHolding(holding.id)) },
                            label = {
                                Text(
                                    text = "${holding.displayTicker} · ${formatWearWeight(effectiveWeight)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    text = holding.name,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            toggleControl = {
                                Switch(checked = isEnabled, enabled = canToggle)
                            },
                            colors = ToggleChipDefaults.toggleChipColors(
                                checkedStartBackgroundColor = Color(0xFF132232),
                                checkedEndBackgroundColor = Color(0xFF0E1520),
                                uncheckedStartBackgroundColor = Color(0xFF141414),
                                uncheckedEndBackgroundColor = Color(0xFF141414)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    WatchHoldingsConfigMode.EDIT_WEIGHTS -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isCustomized) Color(0xFF132232) else Color(0xFF0E1520),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${holding.displayTicker} · ${formatWearWeight(effectiveWeight)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomized) Color(0xFF38BDF8) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isCustomized) {
                                    "Default ${formatWearWeight(holding.weightPercent)}%"
                                } else {
                                    holding.name
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                CompactChip(
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.DECREASE_MEDIUM
                                            )
                                        )
                                    },
                                    colors = ChipDefaults.secondaryChipColors(),
                                    label = { Text("-0.5%", fontSize = 10.sp) }
                                )
                                CompactChip(
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.INCREASE_MEDIUM
                                            )
                                        )
                                    },
                                    colors = ChipDefaults.secondaryChipColors(),
                                    label = { Text("+0.5%", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatWearWeight(value: Double): String {
    val rounded = round(value.coerceAtLeast(0.0) * 100.0) / 100.0
    val whole = rounded.toInt()
    val frac = ((rounded - whole) * 100.0).roundToInt().coerceIn(0, 99)
    return "$whole.${frac.toString().padStart(2, '0')}"
}

@Preview(
    name = "Wear OS Holdings Configurator",
    widthDp = 227,
    heightDp = 227,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PortfolioHoldingsConfigScreenPreview() {
    MaterialTheme {
        PortfolioHoldingsConfigScreen(
            holdings = PortfolioRegistry.HOLDINGS,
            disabledHoldingIds = PortfolioRegistry.disabledIdsForTop10(),
            customWeights = mapOf("aapl" to 12.50),
            onIntent = {}
        )
    }
}
