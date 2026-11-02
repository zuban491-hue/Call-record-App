package com.callcapture.app

import android.app.Activity
import android.os.Bundle
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.callcapture.app.data.db.AppDatabase
import com.callcapture.app.data.preferences.PreferenceManager

class OmniCallApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferenceManager: PreferenceManager
        private set

    @Volatile private var visibleCount = 0
    val hasVisibleActivity: Boolean get() = visibleCount > 0

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)

        // New architecture requires renewed opt-in, without deleting saved data.
        val migration = getSharedPreferences("notification_recording_migration", MODE_PRIVATE)
        if (!migration.getBoolean("reviewed_v1", false)) {
            preferenceManager.isCellularRecordEnabled = false
            preferenceManager.isWhatsAppRecordEnabled = false
            preferenceManager.isMessengerRecordEnabled = false
            preferenceManager.isTelegramRecordEnabled = false
            preferenceManager.audioSourceOption = com.callcapture.app.data.model.AudioSourceOption.MIC
            preferenceManager.isSpeakerphoneAssistantEnabled = false
            migration.edit().putBoolean("reviewed_v1", true).apply()
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { visibleCount += 1 }
            override fun onActivityStopped(activity: Activity) { visibleCount = (visibleCount - 1).coerceAtLeast(0) }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        if (!com.callcapture.app.data.legal.LegalConsent.accepted(this)) {
            preferenceManager.isCellularRecordEnabled = false
            preferenceManager.isWhatsAppRecordEnabled = false
            preferenceManager.isMessengerRecordEnabled = false
            preferenceManager.isTelegramRecordEnabled = false
            getSharedPreferences("message_history", MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
        }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING_ID,
                getString(R.string.channel_recording_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_recording_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(recordingChannel)
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_CALL_ALERTS_ID,
                "Call recording prompts", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val CHANNEL_CALL_ALERTS_ID = "call_recording_prompts"
        const val CHANNEL_RECORDING_ID = "channel_call_recording"
        lateinit var instance: OmniCallApplication
            private set
    }
}
