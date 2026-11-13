package com.marketwear.indices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.ui.MarketUiState
import com.marketwear.indices.ui.MarketViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_INDEX = "extra_target_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialSymbol = intent.getStringExtra(EXTRA_TARGET_INDEX) ?: "SPX"

        setContent {
            MarketWearApp(initialSymbol = initialSymbol)
        }
    }
}

@Composable
fun MarketWearApp(
    initialSymbol: String,
    viewModel: MarketViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()

    // Memoized partition before ScalingLazyColumn to avoid allocation on scroll recompositions
    val (indices, sectors) = remember(uiState.quotes) {
        uiState.quotes.partition { it.symbol == "SPX" || it.symbol == "NDX" }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 12.dp, end = 12.dp)
        ) {
            item(key = "header_title") {
                Text(
                    text = "Market Watch",
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(
                items = indices,
                key = { it.symbol }
            ) { quote ->
                IndexCard(quote = quote)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Market Sectors Section Header
            if (sectors.isNotEmpty()) {
                item(key = "sectors_header") {
                    Text(
                        text = "Sectors",
                        style = MaterialTheme.typography.caption1,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }

                // Sector Cards
                items(
                    items = sectors,
                    key = { it.symbol }
                ) { quote ->
                    IndexCard(quote = quote)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Refresh action chip — elegantly styled, never truncated
            item(key = "refresh_action") {
                CompactChip(
                    onClick = { viewModel.refresh(force = true) },
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = MaterialTheme.colors.primary
                            )
                        }
                    },
                    label = {
                        Text(
                            text = if (uiState.isRefreshing) "Syncing" else "Refresh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Timestamp
            item(key = "timestamp_footer") {
                val timeStr = if (uiState.lastUpdated > 0L) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    timeFormat.format(Date(uiState.lastUpdated))
                } else {
                    "--"
                }
                Text(
                    text = "Updated $timeStr",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

fun getInstrumentIcon(symbol: String): Int {
    return when (symbol) {
        "SPX" -> R.drawable.ic_sp500
        "NDX" -> R.drawable.ic_nasdaq
        "^YH101" -> R.drawable.ic_sector_materials
        "^YH102" -> R.drawable.ic_sector_cyclical
        "^YH103" -> R.drawable.ic_sector_financial
        "^YH104" -> R.drawable.ic_sector_realestate
        "^YH205" -> R.drawable.ic_sector_defensive
        "^YH206" -> R.drawable.ic_sector_healthcare
        "^YH207" -> R.drawable.ic_sector_utilities
        "^YH308" -> R.drawable.ic_sector_communication
        "^YH309" -> R.drawable.ic_sector_energy
        "^YH310" -> R.drawable.ic_sector_industrials
        "^YH311" -> R.drawable.ic_sector_technology
        else -> R.drawable.ic_trend_up
    }
}

private val CardShape = RoundedCornerShape(14.dp)
private val CardBackgroundColor = Color(0xFF1E1E1E)
private val GreenTrendColor = Color(0xFF4CAF50)
private val RedTrendColor = Color(0xFFF44336)

@Composable
fun IndexCard(quote: IndexQuote, modifier: Modifier = Modifier) {
    val trendColor = if (quote.isPositive) GreenTrendColor else RedTrendColor
    val iconRes = remember(quote.symbol) { getInstrumentIcon(quote.symbol) }

    Card(
        onClick = { /* Tap */ },
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = CardBackgroundColor,
            endBackgroundColor = CardBackgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Top Row: Sector Icon + Multi-line Heading + Percent Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = quote.name,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = quote.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = quote.formattedPercent,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Row: Compact Points (1k / 1M multiples) + Compact Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = quote.formattedCompactPoints,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedCompactChange,
                    color = trendColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}
