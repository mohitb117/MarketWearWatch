package com.marketwear.sharedui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearItemKeys
import com.marketwear.sharedui.theme.MarketWearTheme

/**
 * Shared scrollable market dashboard body (Portfolio Score Hero + Major Indices + Sectors)
 * used across Android Mobile, Desktop, iOS, and Web Compose Multiplatform targets.
 */
@Composable
fun MarketDashboardBody(
    portfolioState: PortfolioCardUiState,
    indices: List<IndexQuote>,
    sectors: List<IndexQuote>,
    onToggleWeightMode: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    chartHeight: Dp = MarketWearDimens.DefaultChartHeight
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MarketWearDimens.ItemSpacing)
    ) {
        item(key = MarketWearItemKeys.PORTFOLIO_HERO) {
            PortfolioScoreCard(
                state = portfolioState,
                onToggleWeightMode = onToggleWeightMode,
                chartHeight = chartHeight
            )
        }

        errorMessage?.let { error ->
            item(key = MarketWearItemKeys.ERROR_BANNER) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(MarketWearDimens.ItemSpacing)
                    )
                }
            }
        }

        item(key = MarketWearItemKeys.INDICES_HEADER) {
            Text(
                text = "Major US Indices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = MarketWearDimens.TinySpacing)
            )
        }

        items(indices, key = { it.symbol }) { quote ->
            IndexMetricCard(quote = quote)
        }

        if (sectors.isNotEmpty()) {
            item(key = MarketWearItemKeys.SECTORS_HEADER) {
                Text(
                    text = "US Market Sectors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = MarketWearDimens.SmallSpacing)
                )
            }

            items(sectors, key = { it.symbol }) { quote ->
                IndexMetricCard(quote = quote)
            }
        }
    }
}

@Preview
@Composable
fun MarketDashboardBodyPreview() {
    val dashboard = MarketUiStateBuilder.buildDashboardState(
        quotes = MarketRegistry.getFallbackQuotes(),
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MarketWearTheme {
        MarketDashboardBody(
            portfolioState = dashboard.portfolioState,
            indices = dashboard.indices,
            sectors = dashboard.sectors,
            onToggleWeightMode = {}
        )
    }
}
