package com.marketwear.mobile.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * ForegroundService backing the persistent portfolio Live Update notification on the phone.
 *
 * Posting the notification via [startForeground] sets `Notification.FLAG_FOREGROUND_SERVICE`
 * (`isForegroundService = true` in SystemUI), which is required on Android 14 / 15 / 16 and
 * Samsung One UI 8 so the notification cannot be swiped away and is promoted into the pinned
 * Ongoing / Live Update status bar & top-of-shade section.
 */
class PortfolioLiveForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.marketwear.mobile.action.FGS_START_OR_UPDATE"
        const val ACTION_STOP = "com.marketwear.mobile.action.FGS_STOP"

        @Volatile
        private var pendingNotification: Notification? = null

        fun startOrUpdate(context: Context, notification: Notification) {
            pendingNotification = notification
            val intent = Intent(context, PortfolioLiveForegroundService::class.java).apply {
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
            val intent = Intent(context, PortfolioLiveForegroundService::class.java).apply {
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
                            PortfolioLiveNotificationManager.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(
                            PortfolioLiveNotificationManager.NOTIFICATION_ID,
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
