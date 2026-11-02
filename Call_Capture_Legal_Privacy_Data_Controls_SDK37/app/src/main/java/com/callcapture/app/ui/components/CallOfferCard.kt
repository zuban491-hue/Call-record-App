package com.callcapture.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callcapture.app.data.live.RecordingLiveState
import com.callcapture.app.service.CallRecordingCoordinator

@Composable
fun CallOfferCard(onReviewAccess: () -> Unit) {
    val context = LocalContext.current
    val offer by CallRecordingCoordinator.offer.collectAsStateWithLifecycle()
    val progress by RecordingLiveState.state.collectAsStateWithLifecycle()
    val permissions by rememberLivePermissions()
    val active = offer ?: return
    OutlinedCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${active.type.displayName} call signal", style = MaterialTheme.typography.titleMedium)
            Text("This signal may be incomplete. Recording uses microphone sound; both sides may not be captured.", style = MaterialTheme.typography.bodySmall)
            if (progress.phase == RecordingLiveState.Phase.RECORDING) {
                Button(onClick = { CallRecordingCoordinator.stopUser(context) }) { Text("Stop recording") }
            } else if (!permissions.microphone) {
                Text("Microphone access is needed before recording can start.")
                Button(onClick = onReviewAccess) { Text("Review microphone access") }
            } else {
                Button(enabled = progress.phase != RecordingLiveState.Phase.STARTING && progress.phase != RecordingLiveState.Phase.SAVING,
                    onClick = { CallRecordingCoordinator.startFromTap(context, active.token) }) { Text("Start microphone recording") }
            }
            Text("A matching call-notification removal or phone idle signal stops this recording. Keep the Stop control available in case the app misses that signal.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
