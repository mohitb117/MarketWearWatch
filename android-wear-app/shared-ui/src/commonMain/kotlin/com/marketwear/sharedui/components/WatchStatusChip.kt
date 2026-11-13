package com.marketwear.sharedui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.ui.WatchConnectionState
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTheme

private data class StatusChipStyle(
    val bgColor: Color,
    val dotColor: Color,
    val textColor: Color,
    val label: String
)

@Composable
fun WatchStatusChip(
    connectionState: WatchConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor, textColor, label) = when (connectionState) {
        WatchConnectionState.CONNECTED -> StatusChipStyle(
            MarketWearPalette.ConnectedBg,
            MarketWearPalette.ConnectedDot,
            MarketWearPalette.ConnectedText,
            "Watch Connected"
        )
        WatchConnectionState.DISCONNECTED -> StatusChipStyle(
            MarketWearPalette.DisconnectedBg,
            MarketWearPalette.DisconnectedDot,
            MarketWearPalette.DisconnectedText,
            "Watch Offline"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(bgColor, MarketWearShapes.StatusChip)
            .padding(horizontal = 10.dp, vertical = MarketWearDimens.TinySpacing)
    ) {
        Box(
            modifier = Modifier
                .size(MarketWearDimens.StatusDotSize)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(MarketWearDimens.SmallSpacing))
        Text(
            text = label,
            fontSize = MarketWearTextSizes.StatusChip,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Preview
@Composable
fun WatchStatusChipConnectedPreview() {
    MarketWearTheme {
        WatchStatusChip(connectionState = WatchConnectionState.CONNECTED)
    }
}

@Preview
@Composable
fun WatchStatusChipDisconnectedPreview() {
    MarketWearTheme {
        WatchStatusChip(connectionState = WatchConnectionState.DISCONNECTED)
    }
}
