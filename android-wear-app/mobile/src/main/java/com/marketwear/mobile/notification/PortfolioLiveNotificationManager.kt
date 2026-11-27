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
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.R
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.NotificationExpansionMode
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioLiveUpdateSpec
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.PortfolioWeightMode

/**
 * Renders Android 16 (API 36+) Progress-Centric Notifications ("Live Updates") showing intraday
 * portfolio fluctuations around a 50% morning baseline (100.00):
 * - Status Bar Mini Chip: directional up/down arrow small icon (`setSmallIcon`) + `setShortCriticalText`
 * - [NotificationExpansionMode.COLLAPSED] View: compact 1-line title + 1-line subtitle + 50%-centered `ProgressStyle` bar
 * - [NotificationExpansionMode.EXPANDED] View: full title + detailed subtitle + 50%-centered `ProgressStyle` bar + quick actions
 */
class PortfolioLiveNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        private const val LEGACY_CHANNEL_V2 = "marketwear_portfolio_live_v2"
        private const val LEGACY_CHANNEL_V3 = "marketwear_portfolio_live_v3"
        const val CHANNEL_ID = "marketwear_portfolio_live"
        const val CHANNEL_NAME = "Portfolio Live Updates"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_LAUNCH = 1001
        private const val REQUEST_CODE_TOGGLE_EXPANSION = 1002
        private const val REQUEST_CODE_TOGGLE_WEIGHT_MODE = 1003
        private const val ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS =
            "android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"

        @DrawableRes
        fun statusArrowIconRes(band: PortfolioMomentumBand): Int = when (band) {
            PortfolioMomentumBand.FLAT -> R.drawable.ic_stat_arrow_flat
            PortfolioMomentumBand.MILD_GAIN,
            PortfolioMomentumBand.MODERATE_GAIN,
            PortfolioMomentumBand.STRONG_GAIN -> R.drawable.ic_stat_arrow_up
            PortfolioMomentumBand.MILD_LOSS,
            PortfolioMomentumBand.MODERATE_LOSS,
            PortfolioMomentumBand.STRONG_LOSS -> R.drawable.ic_stat_arrow_down
        }

        @DrawableRes
        fun trackerIconRes(band: PortfolioMomentumBand): Int = when (band) {
            PortfolioMomentumBand.FLAT -> R.drawable.ic_tracker_flat
            PortfolioMomentumBand.MILD_GAIN,
            PortfolioMomentumBand.MODERATE_GAIN,
            PortfolioMomentumBand.STRONG_GAIN -> R.drawable.ic_tracker_up
            PortfolioMomentumBand.MILD_LOSS,
            PortfolioMomentumBand.MODERATE_LOSS,
            PortfolioMomentumBand.STRONG_LOSS -> R.drawable.ic_tracker_down
        }
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

    fun showOrUpdateFromDashboard(
        dashboard: com.marketwear.shared.ui.MarketDashboardState,
        expansionMode: NotificationExpansionMode = NotificationExpansionMode.EXPANDED
    ) {
        showOrUpdate(dashboard.portfolioState.liveUpdateSpec, expansionMode)
    }

    fun showOrUpdateFromQuotes(
        quotes: List<IndexQuote>,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap(),
        expansionMode: NotificationExpansionMode = NotificationExpansionMode.EXPANDED
    ) {
        val portfolioQuote = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL } ?: return
        val cardState = MarketUiStateBuilder.buildPortfolioCardState(
            normalizedQuote = portfolioQuote,
            weightMode = weightMode,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        showOrUpdate(cardState.liveUpdateSpec, expansionMode)
    }

    fun showOrUpdate(
        spec: PortfolioLiveUpdateSpec,
        expansionMode: NotificationExpansionMode = NotificationExpansionMode.EXPANDED
    ) {
        if (!isNotificationPermissionGranted()) return
        ensureChannelCreated()

        val launchIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_LAUNCH,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleExpansionIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TOGGLE_EXPANSION,
            Intent(context, PortfolioNotificationActionReceiver::class.java).apply {
                action = PortfolioNotificationActionReceiver.ACTION_TOGGLE_EXPANSION_MODE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleWeightModeIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TOGGLE_WEIGHT_MODE,
            Intent(context, PortfolioNotificationActionReceiver::class.java).apply {
                action = PortfolioNotificationActionReceiver.ACTION_TOGGLE_WEIGHT_MODE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildAndroid16ProgressNotification(
                spec = spec,
                expansionMode = expansionMode,
                launchIntent = launchIntent,
                toggleExpansionIntent = toggleExpansionIntent,
                toggleWeightModeIntent = toggleWeightModeIntent
            )
        } else {
            buildLegacyProgressNotification(
                spec = spec,
                expansionMode = expansionMode,
                launchIntent = launchIntent,
                toggleExpansionIntent = toggleExpansionIntent,
                toggleWeightModeIntent = toggleWeightModeIntent
            )
        }

        // Start or update the ForegroundService so Android 14/15/16 and Samsung One UI 8
        // mark this notification with FLAG_FOREGROUND_SERVICE (isForegroundService=true),
        // making it non-dismissible and eligible for PromotedOngoing / NowBar status bar chip.
        runCatching {
            PortfolioLiveForegroundService.startOrUpdate(context, notification)
        }.onFailure {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun dismiss() {
        runCatching { PortfolioLiveForegroundService.stop(context) }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    @SuppressLint("NewApi")
    private fun buildAndroid16ProgressNotification(
        spec: PortfolioLiveUpdateSpec,
        expansionMode: NotificationExpansionMode,
        launchIntent: PendingIntent,
        toggleExpansionIntent: PendingIntent,
        toggleWeightModeIntent: PendingIntent
    ): Notification {
        val arrowIconRes = statusArrowIconRes(spec.momentumBand)
        val trackerRes = trackerIconRes(spec.momentumBand)

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
            .setProgressTrackerIcon(Icon.createWithResource(context, trackerRes))

        val (titleText, bodyText, toggleLabel) = when (expansionMode) {
            NotificationExpansionMode.COLLAPSED -> Triple(
                spec.collapsedTitle,
                spec.collapsedSubtitle,
                "Expanded View"
            )
            NotificationExpansionMode.EXPANDED -> Triple(
                spec.expandedTitle,
                spec.expandedSubtitle,
                "Collapsed View"
            )
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(arrowIconRes)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setSubText(spec.subText)
            .setShortCriticalText(spec.shortCriticalText)
            .setColor(spec.accentArgb)
            .setOngoing(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(launchIntent)
            .setStyle(progressStyle)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, arrowIconRes),
                    toggleLabel,
                    toggleExpansionIntent
                ).build()
            )

        if (expansionMode == NotificationExpansionMode.EXPANDED) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, arrowIconRes),
                    "Toggle Norm",
                    toggleWeightModeIntent
                ).build()
            )
        }

        return builder.build()
    }

    private fun buildLegacyProgressNotification(
        spec: PortfolioLiveUpdateSpec,
        expansionMode: NotificationExpansionMode,
        launchIntent: PendingIntent,
        toggleExpansionIntent: PendingIntent,
        toggleWeightModeIntent: PendingIntent
    ): Notification {
        val arrowIconRes = statusArrowIconRes(spec.momentumBand)
        val (titleText, bodyText, toggleLabel) = when (expansionMode) {
            NotificationExpansionMode.COLLAPSED -> Triple(
                spec.collapsedTitle,
                spec.collapsedSubtitle,
                "Expanded View"
            )
            NotificationExpansionMode.EXPANDED -> Triple(
                spec.expandedTitle,
                spec.expandedSubtitle,
                "Collapsed View"
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(arrowIconRes)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setSubText(spec.subText)
            .setProgress(spec.maxProgress, spec.progress, false)
            .setColor(spec.accentArgb)
            .setOngoing(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(launchIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    arrowIconRes,
                    toggleLabel,
                    toggleExpansionIntent
                ).build()
            )

        if (expansionMode == NotificationExpansionMode.EXPANDED) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle(titleText)
                    .setSummaryText(spec.subText)
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    arrowIconRes,
                    "Toggle Norm",
                    toggleWeightModeIntent
                ).build()
            )
        }

        return builder.build()
    }

    private fun ensureChannelCreated() {
        runCatching {
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_V2)
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_V3)
        }
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
