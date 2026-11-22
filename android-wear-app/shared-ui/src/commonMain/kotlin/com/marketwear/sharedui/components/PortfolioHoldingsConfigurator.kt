package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.PortfolioHolding
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.HoldingSelectionPreset
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.WeightAdjustmentStep
import com.marketwear.shared.ui.WeightResetAction
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Filter mode for listing view inside [PortfolioHoldingsConfigurator].
 */
private enum class HoldingsListFilter(val label: String) {
    ALL("All"),
    CUSTOM_ONLY("Custom %")
}

/**
 * Reusable Compose Multiplatform configurator allowing users to enable/disable individual
 * holdings and edit per-listing % allocation weights (defaulting to predefined weights).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioHoldingsConfigurator(
    holdings: List<PortfolioHolding>,
    disabledHoldingIds: Set<String>,
    onIntent: (MarketIntent) -> Unit,
    modifier: Modifier = Modifier,
    customWeights: Map<String, Double> = emptyMap()
) {
    val enabledCount = remember(holdings, disabledHoldingIds) {
        holdings.count { it.id !in disabledHoldingIds }
    }
    val activeRawWeight = remember(disabledHoldingIds, customWeights) {
        PortfolioRegistry.rawWeightPercentFor(disabledHoldingIds, customWeights)
    }
    var editingHoldingId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var listFilter by remember { mutableStateOf(HoldingsListFilter.ALL) }
    val activeFilter = if (customWeights.isEmpty()) HoldingsListFilter.ALL else listFilter

    val filteredHoldings = remember(holdings, searchQuery, activeFilter, customWeights) {
        val q = searchQuery.trim().lowercase()
        holdings.filter { holding ->
            val matchesFilter = when (activeFilter) {
                HoldingsListFilter.ALL -> true
                HoldingsListFilter.CUSTOM_ONLY -> holding.id in customWeights
            }
            val matchesSearch = q.isEmpty() ||
                holding.displayTicker.lowercase().contains(q) ||
                holding.yahooTicker.lowercase().contains(q) ||
                holding.name.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tracked Portfolio Listings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val customLabel = if (customWeights.isNotEmpty()) " · ${customWeights.size} custom" else ""
            Text(
                text = "Active: $enabledCount / ${holdings.size} · ${formatWeightPct(activeRawWeight)}% raw$customLabel",
                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search ticker or holding...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(MarketWearShapes.CompactRow)
                                .clickable { searchQuery = "" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MarketWearShapes.MetricCard,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MarketWearDimens.SmallSpacing),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = disabledHoldingIds.isEmpty(),
                    onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.ALL_37)) },
                    label = {
                        Text(
                            text = "All ${holdings.size}",
                            fontSize = MarketWearTextSizes.FilterChip,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
                FilterChip(
                    selected = enabledCount == 10 && disabledHoldingIds == PortfolioRegistry.disabledIdsForTop10(),
                    onClick = { onIntent(MarketIntent.ApplyHoldingPreset(HoldingSelectionPreset.TOP_10)) },
                    label = {
                        Text(
                            text = "Top 10",
                            fontSize = MarketWearTextSizes.FilterChip,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
                if (customWeights.isNotEmpty()) {
                    FilterChip(
                        selected = activeFilter == HoldingsListFilter.CUSTOM_ONLY,
                        onClick = {
                            listFilter = if (activeFilter == HoldingsListFilter.CUSTOM_ONLY) {
                                HoldingsListFilter.ALL
                            } else {
                                HoldingsListFilter.CUSTOM_ONLY
                            }
                        },
                        label = {
                            Text(
                                text = "Custom (${customWeights.size})",
                                fontSize = MarketWearTextSizes.FilterChip,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            listFilter = HoldingsListFilter.ALL
                            onIntent(MarketIntent.ResetWeights(WeightResetAction.RESTORE_DEFAULT_WEIGHTS))
                        },
                        label = {
                            Text(
                                text = "Reset %",
                                fontSize = MarketWearTextSizes.FilterChip,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MarketWearDimens.SmallSpacing))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(MarketWearDimens.SmallSpacing)
        ) {
            if (filteredHoldings.isEmpty()) {
                item(key = "empty_search_state") {
                    Surface(
                        shape = MarketWearShapes.CompactRow,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No matching portfolio listings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(MarketWearShapes.CompactRow)
                                    .clickable {
                                        searchQuery = ""
                                        listFilter = HoldingsListFilter.ALL
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredHoldings,
                    key = { it.id }
                ) { holding ->
                val isEnabled = holding.id !in disabledHoldingIds
                val canToggle = !(isEnabled && enabledCount <= 1)
                val effectiveWeight = PortfolioRegistry.effectiveWeightPercent(holding, customWeights)
                val isCustomized = holding.id in customWeights
                val isEditing = editingHoldingId == holding.id
                val normalizedShare = if (isEnabled && activeRawWeight > 0.0) {
                    (effectiveWeight / activeRawWeight) * 100.0
                } else {
                    0.0
                }

                Surface(
                    shape = MarketWearShapes.CompactRow,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isEditing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            isEnabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = MarketWearDimens.SmallSpacing)
                                    .clickable {
                                        editingHoldingId = if (isEditing) null else holding.id
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = holding.displayTicker,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val pillBg = when {
                                        isEditing || isCustomized -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    }
                                    val pillBorderColor = when {
                                        isEditing || isCustomized -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    }
                                    Text(
                                        text = "${formatWeightPct(effectiveWeight)}%",
                                        style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                                        fontWeight = if (isCustomized || isEditing) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(MarketWearShapes.CompactRow)
                                            .background(pillBg, MarketWearShapes.CompactRow)
                                            .border(
                                                width = if (isCustomized || isEditing) 1.dp else 0.5.dp,
                                                color = pillBorderColor,
                                                shape = MarketWearShapes.CompactRow
                                            )
                                            .clickable {
                                                editingHoldingId = if (isEditing) null else holding.id
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = holding.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isEnabled) {
                                    val shareSuffix = if (isCustomized) {
                                        " · Default ${formatWeightPct(holding.weightPercent)}%"
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "Basket share: ${formatWeightPct(normalizedShare)}%$shareSuffix",
                                        style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                                        color = if (isCustomized) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                enabled = canToggle,
                                onCheckedChange = { onIntent(MarketIntent.ToggleHolding(holding.id)) }
                            )
                        }

                        if (isEditing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(8.dp))

                            var draftText by remember(holding.id) {
                                mutableStateOf(formatWeightPct(effectiveWeight))
                            }
                            LaunchedEffect(effectiveWeight) {
                                val draftValue = draftText.toDoubleOrNull()
                                if (draftValue == null || abs(draftValue - effectiveWeight) >= 0.005) {
                                    draftText = formatWeightPct(effectiveWeight)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeightStepPill(
                                    label = "-1",
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.DECREASE_LARGE
                                            )
                                        )
                                    }
                                )
                                WeightStepPill(
                                    label = "-.25",
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.DECREASE_FINE
                                            )
                                        )
                                    }
                                )
                                OutlinedTextField(
                                    value = draftText,
                                    onValueChange = { raw ->
                                        val filtered = sanitizeDecimalDraft(raw)
                                        draftText = filtered
                                        val parsed = filtered.toDoubleOrNull()
                                        if (parsed != null &&
                                            parsed >= PortfolioRegistry.MIN_HOLDING_WEIGHT_PERCENT &&
                                            parsed <= PortfolioRegistry.MAX_HOLDING_WEIGHT_PERCENT
                                        ) {
                                            onIntent(MarketIntent.SetHoldingWeight(holding.id, parsed))
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.labelMedium.merge(MarketWearTextStyles.Tabular),
                                    suffix = { Text("%", style = MaterialTheme.typography.labelSmall) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val parsed = draftText.toDoubleOrNull()
                                            if (parsed != null) {
                                                onIntent(MarketIntent.SetHoldingWeight(holding.id, parsed))
                                            }
                                            editingHoldingId = null
                                        }
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                WeightStepPill(
                                    label = "+.25",
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.INCREASE_FINE
                                            )
                                        )
                                    }
                                )
                                WeightStepPill(
                                    label = "+1",
                                    onClick = {
                                        onIntent(
                                            MarketIntent.StepHoldingWeight(
                                                holding.id,
                                                WeightAdjustmentStep.INCREASE_LARGE
                                            )
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCustomized) {
                                    Text(
                                        text = "Restore Default (${formatWeightPct(holding.weightPercent)}%)",
                                        style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(MarketWearShapes.CompactRow)
                                            .clickable {
                                                onIntent(
                                                    MarketIntent.SetHoldingWeight(
                                                        holding.id,
                                                        holding.weightPercent
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Default allocation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .clip(MarketWearShapes.CompactRow)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            MarketWearShapes.CompactRow
                                        )
                                        .clickable { editingHoldingId = null }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun WeightStepPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MarketWearShapes.CompactRow,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        modifier = Modifier
            .clip(MarketWearShapes.CompactRow)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun sanitizeDecimalDraft(raw: String): String {
    val sb = StringBuilder(raw.length)
    var seenDot = false
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !seenDot -> {
                seenDot = true
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}

private fun formatWeightPct(value: Double): String {
    val rounded = round(value.coerceAtLeast(0.0) * 100.0) / 100.0
    val whole = rounded.toInt()
    val frac = ((rounded - whole) * 100.0).roundToInt().coerceIn(0, 99)
    return "$whole.${frac.toString().padStart(2, '0')}"
}

@Preview
@Composable
fun PortfolioHoldingsConfiguratorPreview() {
    MarketWearTheme {
        PortfolioHoldingsConfigurator(
            holdings = PortfolioRegistry.HOLDINGS,
            disabledHoldingIds = PortfolioRegistry.disabledIdsForTop10(),
            customWeights = mapOf("aapl" to 12.50),
            onIntent = {}
        )
    }
}
