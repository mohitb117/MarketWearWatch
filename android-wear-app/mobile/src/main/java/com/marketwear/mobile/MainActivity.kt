package com.marketwear.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.marketwear.mobile.ui.MobileDashboardScreen
import com.marketwear.sharedui.theme.MarketWearTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketWearTheme {
                MobileDashboardScreen()
            }
        }
    }
}
