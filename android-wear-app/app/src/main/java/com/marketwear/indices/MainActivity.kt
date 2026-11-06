package com.marketwear.indices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import kotlinx.coroutines.launch
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
fun MarketWearApp(initialSymbol: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { MarketRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var spxQuote by remember { mutableStateOf<IndexQuote?>(null) }
    var ndxQuote by remember { mutableStateOf<IndexQuote?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshQuotes(force: Boolean = false) {
        scope.launch {
            isLoading = true
            spxQuote = repository.getQuote("SPX", forceRefresh = force)
            ndxQuote = repository.getQuote("NDX", forceRefresh = force)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshQuotes(force = false)
    }

    val listState = rememberScalingLazyListState()

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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 14.dp, end = 14.dp)
        ) {
            item {
                Text(
                    text = "Market Watch",
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // S&P 500 Card
            item {
                spxQuote?.let { quote ->
                    IndexCard(quote = quote)
                } ?: CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Nasdaq 100 Card
            item {
                ndxQuote?.let { quote ->
                    IndexCard(quote = quote)
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            // Force Refresh Button
            item {
                CompactButton(
                    onClick = { refreshQuotes(force = true) },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = if (isLoading) "Updating..." else "Refresh",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timestamp
            item {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeStr = spxQuote?.lastUpdated?.let { timeFormat.format(Date(it)) } ?: "--"
                Text(
                    text = "Updated: $timeStr",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun IndexCard(quote: IndexQuote) {
    val isPositive = quote.change >= 0
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        onClick = { /* Tap */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(backgroundColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedPercent,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = quote.formattedPoints,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedChange,
                    color = trendColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}
