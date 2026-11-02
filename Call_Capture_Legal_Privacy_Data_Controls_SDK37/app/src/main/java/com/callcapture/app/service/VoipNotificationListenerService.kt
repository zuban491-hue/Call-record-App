package com.callcapture.app.service

import android.app.Notification
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.callcapture.app.data.messages.MessageHistoryStore
import com.callcapture.app.data.model.CallType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Notification history and opt-in call signals; no screen scraping or Accessibility API. */
class VoipNotificationListenerService : NotificationListenerService() {
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onListenerConnected() {
        super.onListenerConnected()
        historyConnected.value = true
        // Do not import active/historical notifications.
    }
    override fun onListenerDisconnected() {
        historyConnected.value = false
        CallRecordingCoordinator.listenerUnavailable(this)
        super.onListenerDisconnected()
    }
    override fun onDestroy() {
        historyConnected.value = false
        CallRecordingCoordinator.listenerUnavailable(this)
        historyScope.cancel()
        super.onDestroy()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        historyScope.launch {
            try { MessageHistoryStore.get(this@VoipNotificationListenerService).capture(sbn) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { historyError.value = true }
        }
        val type = SUPPORTED[sbn.packageName] ?: return
        if (!CallRecordingCoordinator.enabled(type)) return
        val n = sbn.notification
        // Strict public category: ambiguous message/social notifications never trigger capture.
        if (n.category != Notification.CATEGORY_CALL || n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            // Some apps replace the same call notification with another category on termination.
            CallRecordingCoordinator.removed(this, sbn.key)
            return
        }
        val caller = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.take(300)
        val audioMode = (getSystemService(AUDIO_SERVICE) as? AudioManager)?.mode
        CallRecordingCoordinator.detected(this, sbn.key, type, caller,
            connectedSignal = audioMode == AudioManager.MODE_IN_COMMUNICATION)
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) CallRecordingCoordinator.removed(this, sbn.key)
        // Removing a notification never deletes saved message-history copies.
    }
    companion object {
        val historyConnected = MutableStateFlow(false)
        val historyError = MutableStateFlow(false)
        private val SUPPORTED = mapOf("com.whatsapp" to CallType.WHATSAPP,
            "com.whatsapp.w4b" to CallType.WHATSAPP_BUSINESS, "com.facebook.orca" to CallType.MESSENGER,
            "com.facebook.mlite" to CallType.MESSENGER, "org.telegram.messenger" to CallType.TELEGRAM)
    }
}
