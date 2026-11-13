package com.marketwear.indices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.marketwear.indices.ui.MarketWearApp

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
