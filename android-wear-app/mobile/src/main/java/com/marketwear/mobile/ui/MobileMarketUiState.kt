package com.marketwear.mobile.ui

import androidx.compose.runtime.Immutable
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.WatchConnectionState

@Immutable
data class MobileMarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val refreshState: RefreshState = RefreshState.IDLE,
    val watchConnectionState: WatchConnectionState = WatchConnectionState.DISCONNECTED,
    val lastSyncedTimestamp: Long = 0L,
    val formattedLastSynced: String = "Not synced",
    val errorMessage: String? = null
)
