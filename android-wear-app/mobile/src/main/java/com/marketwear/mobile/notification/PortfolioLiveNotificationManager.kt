package com.marketwear.mobile.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.R
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioLiveUpdateSpec
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.PortfolioWeightMode

/**
 * Renders Android 16 (API 36+) Progress-Centric Notifications ("Live Updates") showing intraday
 * portfolio fluctuations around a 50% morning baseline (100.00): gains extend right in
 * intensity-scaled green, while declines extend left below 50% in intensity-scaled red.
 * Degrades cleanly to a standard ongoing NotificationCompat progress bar on API 26–35.
 */
class PortfolioLiveNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        const val CHANNEL_ID = "marketwear_portfolio_live"
        const val CHANNEL_NAME = "Portfolio Live Updates"
        const val NOTIFICATION_ID = 1001
        private const val ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS =
            "android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"
    }

    fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canPostPromotedNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 36) return true
        return try {
            notificationManager.canPostPromotedNotifications()
        } catch (_: Exception) {
            false
        }
    }

    fun resolveDeliveryState(preference: LiveNotificationPreference): LiveNotificationDeliveryState {
        return when (preference) {
            LiveNotificationPreference.PAUSED -> LiveNotificationDeliveryState.PAUSED_BY_USER
            LiveNotificationPreference.ACTIVE -> when {
                !isNotificationPermissionGranted() -> LiveNotificationDeliveryState.PERMISSION_REQUIRED
                !canPostPromotedNotifications() -> LiveNotificationDeliveryState.STANDARD_ONGOING_NEEDS_OPT_IN
                else -> LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE
            }
        }
    }

    fun createPromotionSettingsIntent(): Intent {
        val packageManager = context.packageManager
        val promoIntent = Intent(ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (promoIntent.resolveActivity(packageManager) != null) {
            return promoIntent
        }
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    fun showOrUpdateFromQuotes(
        quotes: List<IndexQuote>,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ) {
        val portfolioQuote = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL } ?: return
        val cardState = MarketUiStateBuilder.buildPortfolioCardState(
            normalizedQuote = portfolioQuote,
            weightMode = weightMode,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        showOrUpdate(cardState.liveUpdateSpec)
    }

    fun showOrUpdate(spec: PortfolioLiveUpdateSpec) {
        if (!isNotificationPermissionGranted()) return
        ensureChannelCreated()

        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildAndroid16ProgressNotification(spec, launchIntent)
        } else {
            buildLegacyProgressNotification(spec, launchIntent)
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    @SuppressLint("NewApi")
    private fun buildAndroid16ProgressNotification(
        spec: PortfolioLiveUpdateSpec,
        launchIntent: PendingIntent
    ): Notification {
        val trackerIconRes = when (spec.momentumBand) {
            PortfolioMomentumBand.FLAT -> R.drawable.ic_tracker_flat
            PortfolioMomentumBand.MILD_GAIN,
            PortfolioMomentumBand.MODERATE_GAIN,
            PortfolioMomentumBand.STRONG_GAIN -> R.drawable.ic_tracker_up
            PortfolioMomentumBand.MILD_LOSS,
            PortfolioMomentumBand.MODERATE_LOSS,
            PortfolioMomentumBand.STRONG_LOSS -> R.drawable.ic_tracker_down
        }

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(spec.progress)
            .setProgressSegments(
                spec.segments.map { seg ->
                    Notification.ProgressStyle.Segment(seg.length).setColor(seg.argbColor)
                }
            )
            .setProgressPoints(
                listOf(
                    Notification.ProgressStyle.Point(spec.baselinePoint)
                        .setColor(PortfolioLiveUpdateFormatter.BASELINE_POINT_WHITE)
                )
            )
            .setProgressTrackerIcon(Icon.createWithResource(context, trackerIconRes))

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_portfolio)
            .setContentTitle(spec.title)
            .setContentText(spec.subtitle)
            .setShortCriticalText(spec.shortCriticalText)
            .setColor(spec.accentArgb)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(launchIntent)
            .setStyle(progressStyle)
            .build()
    }

    private fun buildLegacyProgressNotification(
        spec: PortfolioLiveUpdateSpec,
        launchIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_portfolio)
            .setContentTitle(spec.title)
            .setContentText(spec.subtitle)
            .setProgress(spec.maxProgress, spec.progress, false)
            .setColor(spec.accentArgb)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(launchIntent)
            .build()
    }

    private fun ensureChannelCreated() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Live 50%-centered daily portfolio fluctuation bar on Android 16+"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
