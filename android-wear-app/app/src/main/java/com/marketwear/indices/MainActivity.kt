package com.marketwear.indices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.indices.ui.MarketWearApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val watchNotificationManager: WatchLiveNotificationManager by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    companion object {
        const val EXTRA_TARGET_INDEX = "extra_target_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWatchNotificationPermissionIfNeeded()
        val initialSymbol = intent.getStringExtra(EXTRA_TARGET_INDEX) ?: "SPX"

        setContent {
            MarketWearApp(initialSymbol = initialSymbol)
        }
    }

    private fun requestWatchNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
