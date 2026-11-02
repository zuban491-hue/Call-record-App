package com.callcapture.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.callcapture.app.OmniCallApplication
import com.callcapture.app.data.live.RecordingLiveState
import com.callcapture.app.data.model.CallDirection
import com.callcapture.app.data.model.CallType
import com.callcapture.app.ui.MainActivity
import com.callcapture.app.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Public-API call signals only. Never launches an Activity automatically or changes permissions. */
object CallRecordingCoordinator {
    data class Offer(val key: String, val token: String, val type: CallType, val caller: String?)
    private val current = MutableStateFlow<Offer?>(null)
    val offer = current.asStateFlow()
    @Volatile private var recordingKey: String? = null
    private val suppressed = mutableSetOf<String>()
    private val autoAttempted = mutableSetOf<String>()
    private const val ALERT_ID = 1002
    const val ACTION_RECORD_FROM_TAP = "com.callcapture.app.RECORD_FROM_TAP"
    const val EXTRA_OFFER_TOKEN = "offer_token"

    fun enabled(type: CallType): Boolean {
        if (com.callcapture.app.data.legal.LocalDataControls.deleting) return false
        if (!com.callcapture.app.data.legal.LegalConsent.accepted(OmniCallApplication.instance)) return false
        val prefs = OmniCallApplication.instance.preferenceManager
        return when (type) {
            CallType.CELLULAR -> prefs.isCellularRecordEnabled
            CallType.WHATSAPP, CallType.WHATSAPP_BUSINESS -> prefs.isWhatsAppRecordEnabled
            CallType.MESSENGER -> prefs.isMessengerRecordEnabled
            CallType.TELEGRAM -> prefs.isTelegramRecordEnabled
            else -> false
        }
    }
    @Synchronized fun detected(context: Context, key: String, type: CallType, caller: String?, connectedSignal: Boolean) {
        if (!enabled(type) || key in suppressed) return
        val old = current.value
        // Do not replace an active capture with an unrelated concurrent call notification.
        if (CallRecordingService.isServiceRecording && recordingKey != key) return
        val next = Offer(key, if (old?.key == key) old.token else java.util.UUID.randomUUID().toString(), type, caller?.take(300))
        current.value = next
        if (CallRecordingService.isServiceRecording) return
        if (RecordingLiveState.state.value.phase in listOf(RecordingLiveState.Phase.STARTING, RecordingLiveState.Phase.SAVING)) return
        if (connectedSignal && key !in autoAttempted && OmniCallApplication.instance.hasVisibleActivity && PermissionHelper.hasRecordAudioPermission(context)) {
            autoAttempted.add(key)
            recordingKey = key
            CallRecordingService.startRecording(context, type, CallDirection.UNKNOWN, next.caller)
        } else showTap(context)
    }
    @Synchronized fun startFromTap(context: Context, token: String) {
        val active = current.value ?: return
        if (active.token != token || !enabled(active.type) || CallRecordingService.isServiceRecording) return
        if (RecordingLiveState.state.value.phase in listOf(RecordingLiveState.Phase.STARTING, RecordingLiveState.Phase.SAVING)) return
        if (!PermissionHelper.hasRecordAudioPermission(context)) return // In-app card explains the missing access.
        recordingKey = active.key
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ALERT_ID)
        CallRecordingService.startRecording(context, active.type, CallDirection.UNKNOWN, active.caller)
    }
    @Synchronized fun showTap(context: Context) {
        val active = current.value ?: return
        if (!enabled(active.type) || !PermissionHelper.hasNotificationPermission(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            action = ACTION_RECORD_FROM_TAP
            data = Uri.parse("callcapture://record/${active.token}")
            putExtra(EXTRA_OFFER_TOKEN, active.token)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 1002, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, OmniCallApplication.CHANNEL_CALL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("${active.type.displayName}: tap to record")
            .setContentText("Start microphone capture. Both sides of the call may not be available.")
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try { (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ALERT_ID, notification) }
        catch (_: Exception) { /* The in-app offer remains available when notifications are blocked. */ }
    }
    @Synchronized fun removed(context: Context, key: String) {
        suppressed.remove(key)
        autoAttempted.remove(key)
        if (current.value?.key == key) {
            current.value = null
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ALERT_ID)
        }
        if (recordingKey == key) {
            recordingKey = null
            if (CallRecordingService.isServiceRecording) CallRecordingService.stopRecording(context)
        }
    }
    @Synchronized fun markUserStopped(context: Context) {
        current.value?.let { suppressed.add(it.key) }
        recordingKey?.let { suppressed.add(it) }
        recordingKey = null
        current.value = null
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ALERT_ID)
    }
    @Synchronized fun stopUser(context: Context) {
        markUserStopped(context)
        if (CallRecordingService.isServiceRecording) CallRecordingService.stopRecording(context)
    }
    @Synchronized fun listenerUnavailable(context: Context) {
        current.value?.takeIf { it.type != CallType.CELLULAR }?.let { removed(context, it.key) }
    }
}
