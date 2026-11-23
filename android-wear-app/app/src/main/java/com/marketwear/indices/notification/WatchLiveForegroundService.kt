package com.marketwear.indices.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * ForegroundService backing the persistent Wear OS portfolio notification and [androidx.wear.ongoing.OngoingActivity].
 *
 * Running as a foreground service with `OngoingActivity.apply(context)` pins the portfolio
 * notification as a non-dismissible ongoing notification in the Wear OS tray and shows the live
 * ongoing indicator on the watch face while keeping the rich expandable graphic card and
 * "Open on Watch" / "Open on Phone" actions in the notification stream.
 */
class WatchLiveForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.marketwear.indices.action.WATCH_FGS_START_OR_UPDATE"
        const val ACTION_STOP = "com.marketwear.indices.action.WATCH_FGS_STOP"

        @Volatile
        private var pendingNotification: Notification? = null

        fun startOrUpdate(context: Context, notification: Notification) {
            pendingNotification = notification
            val intent = Intent(context, WatchLiveForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            pendingNotification = null
            val intent = Intent(context, WatchLiveForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                val notification = pendingNotification
                if (notification != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            WatchLiveNotificationManager.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(
                            WatchLiveNotificationManager.NOTIFICATION_ID,
                            notification
                        )
                    }
                } else {
                    stopSelf()
                }
                return START_REDELIVER_INTENT
            }
        }
    }
}
