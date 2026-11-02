package com.callcapture.app.ui.components

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.callcapture.app.data.live.RecordingLiveState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun LiveRecordingBanner() {
    val status by RecordingLiveState.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var elapsed by remember(status.session) { mutableStateOf(0L) }
    LaunchedEffect(status, owner) {
        if (status.phase == RecordingLiveState.Phase.RECORDING) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    elapsed = ((SystemClock.elapsedRealtime() - status.startedElapsed) / 1000).coerceAtLeast(0)
                    delay(1000)
                }
            }
        }
    }
    if (status.phase == RecordingLiveState.Phase.IDLE) return
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (status.phase == RecordingLiveState.Phase.ERROR) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(when (status.phase) {
                RecordingLiveState.Phase.STARTING -> "Starting recording…"
                RecordingLiveState.Phase.RECORDING -> "Recording · ${status.appName} · ${String.format(Locale.US, "%02d:%02d", elapsed / 60, elapsed % 60)}"
                RecordingLiveState.Phase.SAVING -> "Saving recording…"
                RecordingLiveState.Phase.SAVED -> "Recording saved"
                RecordingLiveState.Phase.ERROR -> "Recording needs attention"
                else -> ""
            }, style = MaterialTheme.typography.titleSmall)
            Text(if (status.phase == RecordingLiveState.Phase.RECORDING)
                "Audio capture is active. Both sides of the call may not be available."
                else status.detail.ifBlank { "Please wait for recording status." }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
