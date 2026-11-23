package com.marketwear.indices.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.wearable.Wearable
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * BroadcastReceiver invoked by the "Open on Phone" action button in the persistent
 * Wear OS portfolio notification. Sends [WearConstants.PATH_OPEN_PHONE_APP] to all
 * connected phone nodes so the user can choose to open the app either on the watch
 * ("Open on Watch") or on the paired phone ("Open on Phone") as two separate actions.
 */
class WatchOpenOnPhoneReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_OPEN_ON_PHONE = "com.marketwear.indices.action.OPEN_ON_PHONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_OPEN_ON_PHONE) return

        Toast.makeText(context, "Opening on Phone…", Toast.LENGTH_SHORT).show()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    runCatching {
                        messageClient.sendMessage(
                            node.id,
                            WearConstants.PATH_OPEN_PHONE_APP,
                            ByteArray(0)
                        ).await()
                    }
                }
            } catch (_: Exception) {
                // Ignore wearable transport failure if phone is temporarily out of range
            } finally {
                pendingResult.finish()
            }
        }
    }
}
