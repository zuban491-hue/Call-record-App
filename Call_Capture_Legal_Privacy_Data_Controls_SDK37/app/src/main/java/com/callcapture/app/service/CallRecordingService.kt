package com.callcapture.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.callcapture.app.OmniCallApplication
import com.callcapture.app.data.live.RecordingLiveState
import com.callcapture.app.data.live.RecordingLiveState.Phase
import com.callcapture.app.R
import com.callcapture.app.data.db.AppDatabase
import com.callcapture.app.data.db.RecordingEntity
import com.callcapture.app.data.model.AudioFormatOption
import com.callcapture.app.data.model.CallDirection
import com.callcapture.app.data.model.CallType
import com.callcapture.app.recorder.AudioRecorderEngine
import com.callcapture.app.recorder.MediaRecorderEngine
import com.callcapture.app.recorder.PcmWavRecordingEngine
import com.callcapture.app.ui.MainActivity
import com.callcapture.app.util.ContactHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var recorderEngine: AudioRecorderEngine? = null
    private var currentCallType: CallType = CallType.CELLULAR
    private var currentDirection: CallDirection = CallDirection.UNKNOWN
    private var currentIdentifier: String? = null
    private var resolvedContactName: String? = null
    private var recordingStartTime: Long = 0
    @Volatile private var liveSession: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var tickerRunnable: Runnable? = null
    private var previousSpeakerphoneState: Boolean = false
    private var audioManager: AudioManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RECORDING

        when (action) {
            ACTION_START_RECORDING -> {
                val callTypeStr = intent?.getStringExtra(EXTRA_CALL_TYPE)
                val callType = callTypeStr?.let {
                    try { CallType.valueOf(it) } catch (_: Exception) { CallType.CELLULAR }
                } ?: CallType.CELLULAR

                val directionStr = intent?.getStringExtra(EXTRA_DIRECTION)
                val direction = directionStr?.let {
                    try { CallDirection.valueOf(it) } catch (_: Exception) { CallDirection.UNKNOWN }
                } ?: CallDirection.UNKNOWN

                val identifier = intent?.getStringExtra(EXTRA_IDENTIFIER)

                handleStartRecording(callType, direction, identifier)
            }
            ACTION_STOP_RECORDING -> {
                // Explicit Stop suppresses re-start from updates to the same call signal.
                CallRecordingCoordinator.markUserStopped(this)
                handleStopRecording()
            }
            ACTION_TOGGLE_SPEAKER -> {
                handleToggleSpeaker()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleStartRecording(callType: CallType, direction: CallDirection, identifier: String?) {
        if (recorderEngine?.isRecording() == true) {
            // If ongoing recording has a generic or missing name, update the metadata immediately!
            val isCurrentNameGeneric = currentIdentifier.isNullOrBlank() ||
                    currentIdentifier == currentCallType.displayName ||
                    currentIdentifier == "Unknown" ||
                    currentCallType == CallType.CELLULAR

            if (!identifier.isNullOrBlank() && (isCurrentNameGeneric || callType != CallType.CELLULAR)) {
                Log.d(TAG, "Updating active recording metadata")
                currentCallType = callType
                RecordingLiveState.rename(liveSession, callType.displayName)
                currentIdentifier = identifier
                resolvedContactName = if (callType == CallType.CELLULAR) {
                    ContactHelper.getContactName(this, identifier)
                } else {
                    identifier
                }
                val elapsedSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                val notification = buildOngoingNotification(elapsedSeconds)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            } else {
                Log.d(TAG, "Recording is already active, ignoring duplicate start")
            }
            return
        }

        liveSession = java.util.UUID.randomUUID().toString()
        RecordingLiveState.begin(liveSession, callType.displayName)
        if (com.callcapture.app.data.legal.LocalDataControls.deleting || !com.callcapture.app.data.legal.LegalConsent.accepted(this)) {
            RecordingLiveState.report(liveSession, Phase.ERROR, "Review the Terms of Use before enabling recording.")
            stopSelf()
            return
        }
        // Verify microphone permission before proceeding
        if (!com.callcapture.app.util.PermissionHelper.hasRecordAudioPermission(this)) {
            RecordingLiveState.report(liveSession, Phase.ERROR, "Microphone permission is off.")
            Log.w(TAG, "RECORD_AUDIO permission not granted, cannot start call recording")
            stopSelf()
            return
        }

        currentCallType = callType
        currentDirection = direction
        currentIdentifier = identifier
        recordingStartTime = System.currentTimeMillis()

        // Resolve contact name if phone number
        resolvedContactName = if (callType == CallType.CELLULAR) {
            ContactHelper.getContactName(this, identifier)
        } else {
            identifier
        }

        val app = OmniCallApplication.instance
        val prefs = app.preferenceManager

        // Speakerphone assistant: Activate speakerphone for crystal-clear two-way audio recording
        if (prefs.isSpeakerphoneAssistantEnabled) {
            try {
                previousSpeakerphoneState = isSpeakerphoneActive()
                setSpeakerphoneActive(true)
                Log.d(TAG, "Speakerphone Assistant: Enabled speakerphone for $callType call clarity")
            } catch (e: Exception) {
                Log.e(TAG, "Could not set speakerphone state", e)
            }
        }

        // Use the declared microphone foreground-service type; respect Android background restrictions.
        val notification = buildOngoingNotification(0)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (se: SecurityException) {
            RecordingLiveState.report(liveSession, Phase.ERROR,
                "Android blocked microphone access in the background. Open the app and review access; automatic capture may be unavailable.")
            CallRecordingCoordinator.showTap(this)
            Log.w(TAG, "Android rejected microphone foreground service")
            stopSelf()
            return
        } catch (e: Exception) {
            RecordingLiveState.report(liveSession, Phase.ERROR, "Android could not start the recording service.")
            CallRecordingCoordinator.showTap(this)
            Log.e(TAG, "Unexpected error starting foreground service", e)
            stopSelf()
            return
        }

        // Prepare file
        val recordingsDir = File(filesDir, "recordings").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanName = (resolvedContactName ?: identifier ?: "Unknown")
            .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val ext = prefs.audioFormatOption.extension
        val outputFile = File(recordingsDir, "REC_${callType.name}_${cleanName}_${timeStamp}.$ext")

        // Instantiate recording engine
        val engine: AudioRecorderEngine = if (prefs.audioFormatOption == AudioFormatOption.WAV) {
            PcmWavRecordingEngine(gainMultiplier = prefs.softwareGainMultiplier)
        } else {
            MediaRecorderEngine(this)
        }

        try {
            engine.start(outputFile, prefs.audioSourceOption)
            recorderEngine = engine
            isServiceRecording = true
            RecordingLiveState.report(liveSession, Phase.RECORDING, startedElapsed = SystemClock.elapsedRealtime())
            startNotificationTicker()
            Log.d(TAG, "Recording started")
        } catch (e: Exception) {
            RecordingLiveState.report(liveSession, Phase.ERROR, "Audio capture could not start on this phone.")
            Log.e(TAG, "Failed to start audio engine", e)
            handleStopRecording()
        }
    }

    private fun handleStopRecording() {
        if (recorderEngine == null && RecordingLiveState.state.value.phase == Phase.SAVING) return
        stopNotificationTicker()

        val engine = recorderEngine
        recorderEngine = null
        isServiceRecording = false

        // Restore speakerphone if modified
        val app = OmniCallApplication.instance
        val prefs = app.preferenceManager
        if (prefs.isSpeakerphoneAssistantEnabled) {
            try {
                setSpeakerphoneActive(previousSpeakerphoneState)
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring speakerphone state", e)
            }
        }

        if (engine != null) {
            val stoppedSession = liveSession
            val stoppedContact = resolvedContactName
            val stoppedIdentifier = currentIdentifier
            val stoppedType = currentCallType
            val stoppedDirection = currentDirection
            val stoppedStartTime = recordingStartTime
            RecordingLiveState.report(stoppedSession, Phase.SAVING)
            serviceScope.launch {
                try {
                    val result = engine.stop()
                    if (result.success && result.fileSizeBytes > 0) {
                        // Try to re-resolve caller name if it was unknown (especially for cellular calls from call log)
                        var finalContactName = stoppedContact
                        var finalIdentifier = stoppedIdentifier

                        if (stoppedType == CallType.CELLULAR && (finalContactName == null || finalIdentifier == null)) {
                            val lateResolved = ContactHelper.queryLatestCallLogEntry(this@CallRecordingService)
                            if (!lateResolved.isNullOrBlank()) {
                                finalContactName = lateResolved
                                if (finalIdentifier == null) finalIdentifier = lateResolved
                            }
                        }

                        val displayName = finalContactName
                            ?: finalIdentifier
                            ?: "${stoppedType.displayName} Call"

                        val entity = RecordingEntity(
                            title = "$displayName (${stoppedType.displayName})",
                            contactName = finalContactName,
                            phoneNumber = if (stoppedType == CallType.CELLULAR) finalIdentifier else null,
                            callType = stoppedType,
                            direction = stoppedDirection,
                            filePath = result.file.absolutePath,
                            timestamp = stoppedStartTime,
                            durationMs = result.durationMs,
                            fileSizeBytes = result.fileSizeBytes,
                            audioFormat = result.file.extension
                        )
                        AppDatabase.getInstance(this@CallRecordingService)
                            .recordingDao()
                            .insertRecording(entity)
                        RecordingLiveState.report(stoppedSession, Phase.SAVED, "Recording added to Records.")
                        Log.d(TAG, "Recording saved to database")
                    } else {
                        RecordingLiveState.report(stoppedSession, Phase.ERROR, "No usable recording was saved. Audio capture failed or produced an empty file.")
                        // Empty or failed file; cleanup
                        if (result.file.exists()) {
                            result.file.delete()
                        }
                        Log.d(TAG, "No usable recording produced")
                    }

                } catch (_: Exception) {
                    RecordingLiveState.report(stoppedSession, Phase.ERROR, "Recording could not be saved. Check device storage.")
                } finally {
                    if (liveSession == stoppedSession && recorderEngine == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleToggleSpeaker() {
        val newState = !isSpeakerphoneActive()
        setSpeakerphoneActive(newState)
        Log.d(TAG, "User toggled speakerphone. New state: $newState")
        val elapsedSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
        val notification = buildOngoingNotification(elapsedSeconds)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun isSpeakerphoneActive(): Boolean {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn
        }
    }

    private fun setSpeakerphoneActive(enable: Boolean) {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enable) {
                    val speaker = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        am.setCommunicationDevice(speaker)
                    }
                } else {
                    am.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = enable
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set speakerphone to $enable", e)
        }
    }

    private var normalAudioModeStreak = 0

    private fun startNotificationTicker() {
        normalAudioModeStreak = 0
        tickerRunnable = object : Runnable {
            override fun run() {
                val elapsedSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                val notification = buildOngoingNotification(elapsedSeconds)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, notification)

                // Safe Watchdog:
                // Only for VoIP calls if audio mode drops to MODE_NORMAL and stays NORMAL for 10 straight seconds,
                // and call has run for at least 15s. This prevents sudden stops while catching real terminations.
                if (currentCallType != CallType.CELLULAR && elapsedSeconds >= 15) {
                    val mode = audioManager?.mode ?: AudioManager.MODE_NORMAL
                    if (mode == AudioManager.MODE_NORMAL) {
                        normalAudioModeStreak++
                        if (normalAudioModeStreak >= 10) {
                            Log.d(TAG, "Watchdog: VoIP audio mode stayed NORMAL for 10s. Auto-stopping recording.")
                            handleStopRecording()
                            return
                        }
                    } else {
                        normalAudioModeStreak = 0
                    }
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(tickerRunnable!!, 1000)
    }

    private fun stopNotificationTicker() {
        tickerRunnable?.let { handler.removeCallbacks(it) }
        tickerRunnable = null
    }

    private fun buildOngoingNotification(elapsedSeconds: Int): Notification {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val targetName = resolvedContactName ?: currentIdentifier ?: currentCallType.displayName
        val contentText = "$targetName • $timeStr (${currentCallType.displayName})"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speakerIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_TOGGLE_SPEAKER
        }
        val pendingSpeaker = PendingIntent.getService(
            this,
            2,
            speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isSpeakerOn = isSpeakerphoneActive()
        val speakerTitle = if (isSpeakerOn) "Speaker ON 🔊" else "Speaker OFF 🔈"

        return NotificationCompat.Builder(this, OmniCallApplication.CHANNEL_RECORDING_ID)
            .setContentTitle("🔴 Recording ${currentCallType.displayName}")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpenApp)
            .addAction(
                android.R.drawable.ic_lock_silent_mode_off,
                speakerTitle,
                pendingSpeaker
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_stop),
                pendingStop
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationTicker()
        if (recorderEngine?.isRecording() == true) {
            RecordingLiveState.report(liveSession, Phase.ERROR, "Recording was interrupted before it could be saved.")
            recorderEngine?.stop()
        }
        isServiceRecording = false
    }

    companion object {
        private const val TAG = "CallRecordingService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "com.callcapture.app.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.callcapture.app.STOP_RECORDING"
        const val ACTION_TOGGLE_SPEAKER = "com.callcapture.app.TOGGLE_SPEAKER"

        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_DIRECTION = "extra_direction"
        const val EXTRA_IDENTIFIER = "extra_identifier"

        @Volatile
        var isServiceRecording: Boolean = false
            private set

        fun startRecording(context: Context, type: CallType, direction: CallDirection, identifier: String?) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_CALL_TYPE, type.name)
                putExtra(EXTRA_DIRECTION, direction.name)
                putExtra(EXTRA_IDENTIFIER, identifier)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) {
                if (!isServiceRecording) {
                    val failedSession = java.util.UUID.randomUUID().toString()
                    RecordingLiveState.begin(failedSession, type.displayName)
                    RecordingLiveState.report(failedSession, Phase.ERROR,
                        "Android could not start background recording. Tap the call prompt or open the app.")
                    CallRecordingCoordinator.showTap(context)
                }
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            try { context.startService(intent) } catch (_: Exception) { }
        }
    }
}
