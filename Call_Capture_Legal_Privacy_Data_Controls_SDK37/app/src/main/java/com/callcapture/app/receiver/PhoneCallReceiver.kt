package com.callcapture.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.callcapture.app.data.model.CallType
import com.callcapture.app.service.CallRecordingCoordinator

/** Phone-state signals only. No call-log, contact or phone-number extraction. */
class PhoneCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> CallRecordingCoordinator.detected(context, KEY, CallType.CELLULAR, null, false)
            TelephonyManager.EXTRA_STATE_OFFHOOK -> CallRecordingCoordinator.detected(context, KEY, CallType.CELLULAR, null, true)
            TelephonyManager.EXTRA_STATE_IDLE -> CallRecordingCoordinator.removed(context, KEY)
        }
    }
    companion object { private const val KEY = "cellular-phone-state" }
}
