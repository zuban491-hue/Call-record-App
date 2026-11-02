package com.callcapture.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callcapture.app.data.messages.MessageHistoryStore
import com.callcapture.app.service.VoipNotificationListenerService

/** Explains permission dependencies without asserting recording compatibility or device security. */
@Composable
fun PermissionHealthCard(
    permissions: PermissionStatus,
    cellularEnabled: Boolean,
    voipEnabled: Boolean,
    onReviewAccess: () -> Unit,
    onManageMessages: () -> Unit
) {
    val context = LocalContext.current
    val history = remember { MessageHistoryStore.get(context) }
    val revision by history.changes.collectAsStateWithLifecycle()
    val historyEnabled = remember(revision) { history.enabled }
    val connected by VoipNotificationListenerService.historyConnected.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf(false) }
    val recordingEnabled = cellularEnabled || voipEnabled
    val issues = buildList {
        if (!permissions.microphone) add(if (recordingEnabled)
            "Recording is enabled, but microphone access is off. Audio capture cannot start."
            else "Microphone is off. Saved recordings are available; new audio capture needs access.")
        if (cellularEnabled && !permissions.phone) add("Phone-state access is off. Cellular call detection may not work.")
        if (voipEnabled && !permissions.notificationAccess)
            add("Notification access is off. Supported VoIP call notifications cannot be observed.")
        if (historyEnabled && !permissions.notificationAccess) add("Message history is enabled, but Android notification access is off. New messages cannot be saved.")
        else if (historyEnabled && !connected) add("Notification access is enabled. Waiting for Android to connect the listener; new messages may not be saved yet.")
        if (recordingEnabled && !permissions.notifications) add("Recording notifications are off. Notification-drawer status and controls may be hidden.")
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Permission health", style = MaterialTheme.typography.titleMedium)
            Text(if (issues.isEmpty()) "No permission blockers detected for enabled features" else "Review feature access",
                style = MaterialTheme.typography.bodyMedium)
            PermissionTick("Microphone", permissions.microphone)
            PermissionTick("Notification access", permissions.notificationAccess, "Messages and notification-based call detector")
            issues.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(if (historyEnabled) "Message saving: enabled" else "Message saving: paused or not enabled",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { details = !details }) { Text(if (details) "Hide details" else "More permission details") }
            if (details) {
                PermissionTick("Phone state", permissions.phone, "Optional cellular detector")
                PermissionTick("Recording notifications", permissions.notifications)
                PermissionTick("Battery exemption", permissions.batteryExempt, "Optional troubleshooting; Android Doze only")
                Text("A missing optional permission is not a device security problem. Battery exemption does not confirm that all phone-specific background limits are disabled.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("Checks access settings only. Enabled access does not guarantee a running detector, successful recording or audio from both sides of a call.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onReviewAccess) { Text("Review permissions") }
            TextButton(onClick = onManageMessages) { Text("Manage message history") }
        }
    }
}
