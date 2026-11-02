package com.callcapture.app.ui.components

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.callcapture.app.util.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Permission configuration only, not a recording/service health indicator. */
data class PermissionStatus(
    val microphone: Boolean, val phone: Boolean, val notifications: Boolean,
    val notificationAccess: Boolean, val batteryExempt: Boolean
)

private fun readPermissions(context: Context): PermissionStatus {
    val expected = ComponentName(context, "com.callcapture.app.service.VoipNotificationListenerService")
    val listenerEnabled = try {
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { ComponentName.unflattenFromString(it) == expected } == true
    } catch (_: Exception) { false }
    return PermissionStatus(
        PermissionHelper.hasRecordAudioPermission(context), PermissionHelper.hasPhoneStatePermission(context),
        PermissionHelper.hasNotificationPermission(context),
        listenerEnabled, PermissionHelper.isIgnoringBatteryOptimizations(context))
}

@Composable
fun rememberLivePermissions(): State<PermissionStatus> {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    return produceState(initialValue = readPermissions(context), key1 = context, key2 = owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) { value = readPermissions(context) }
            }
            var observingSettings = false
            try {
                try {
                    // Some OEMs don't dispatch every special-access change; the bounded ticker below covers that.
                    context.contentResolver.registerContentObserver(
                        Settings.Secure.getUriFor("enabled_notification_listeners"), false, observer)
                    observingSettings = true
                } catch (_: Exception) { }
                while (isActive) {
                    value = readPermissions(context)
                    delay(1000)
                }
            } finally {
                if (observingSettings) context.contentResolver.unregisterContentObserver(observer)
                handler.removeCallbacksAndMessages(null)
            }
        }
    }
}

@Composable
fun PermissionTick(label: String, granted: Boolean, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (granted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            if (granted) "$label: allowed" else "$label: not enabled",
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        Text(if (granted) "Allowed" else "Not enabled", style = MaterialTheme.typography.labelSmall)
    }
}
