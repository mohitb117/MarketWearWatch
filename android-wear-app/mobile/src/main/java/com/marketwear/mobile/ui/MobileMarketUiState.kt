package com.marketwear.mobile.ui

import androidx.compose.runtime.Immutable
import com.marketwear.shared.model.IndexQuote

@Immutable
data class MobileMarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val isSyncing: Boolean = false,
    val isWatchConnected: Boolean = false,
    val lastSyncedTimestamp: Long = 0L,
    val errorMessage: String? = null
)
