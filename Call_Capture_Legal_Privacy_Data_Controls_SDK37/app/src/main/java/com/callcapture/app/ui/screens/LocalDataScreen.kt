package com.callcapture.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callcapture.app.data.legal.LegalConsent
import com.callcapture.app.data.legal.LocalDataControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDataScreen(onBack: () -> Unit, onBeforeDelete: () -> Unit, onSettingsChanged: () -> Unit,
    onWithdrawn: () -> Unit, onReadDeletion: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    BackHandler { if (!busy) onBack() }
    fun perform(action: String) {
        scope.launch {
            busy = true
            try {
                LocalDataControls.pause(context)
                LocalDataControls.pauseMessages(context)
                onSettingsChanged()
                when (action) {
                    "delete" -> {
                        onBeforeDelete()
                        val result = LocalDataControls.deleteSavedData(context)
                        message = if (result.failedFiles == 0)
                            "Deleted ${result.recordingRows} recording entries and ${result.messageRows} saved message entries. Capture remains paused. Settings and Terms receipts are retained."
                        else "Deletion is incomplete: ${result.failedFiles} file(s) could not be removed. Some entries may remain. Capture is paused; try again after checking device storage."
                    }
                    "withdraw" -> {
                        withContext(Dispatchers.IO) { LegalConsent.withdraw(context) }
                        onWithdrawn()
                    }
                    else -> message = "Call channels and message saving are paused. Active recording was asked to stop. Existing copies remain. Android permissions are unchanged."
                }
            } catch (_: Exception) {
                message = "Couldn't complete this action. Some data may remain. Capture was paused; review your saved data and try again."
            } finally { busy = false }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Manage local data") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("There is no app account or developer cloud archive in this version. These controls apply to data on this device.")
            OutlinedButton(enabled = !busy, onClick = { perform("pause") }, modifier = Modifier.fillMaxWidth()) { Text("Pause all capture features") }
            OutlinedButton(enabled = !busy, onClick = { confirm = "withdraw" }, modifier = Modifier.fillMaxWidth()) { Text("Withdraw agreement and pause") }
            Button(enabled = !busy, onClick = { confirm = "delete" }, modifier = Modifier.fillMaxWidth()) { Text("Delete saved recordings and messages") }
            Text("Deletion cannot recall shared/exported copies or remove source chats in other apps. It does not uninstall the app, revoke Android permissions or reset settings. Ordinary deletion does not guarantee forensic secure erasure.", style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = !busy, onClick = onReadDeletion) { Text("Read retention and deletion details") }
            if (busy) CircularProgressIndicator()
            message?.let { Text(it) }
            TextButton(enabled = !busy, onClick = onBack) { Text("Back") }
        }
    }
    confirm?.let { action -> AlertDialog(onDismissRequest = { confirm = null },
        title = { Text(if (action == "delete") "Permanently delete saved copies?" else "Withdraw agreement?") },
        text = { Text(if (action == "delete") "This pauses capture and deletes private saved audio, recording metadata/notes and message history on this device. Shared copies and source chats are unaffected. There is no undo."
            else "This clears the local agreement receipt and pauses new capture. Saved copies remain available to view or delete. You can review Terms and opt in again later.") },
        confirmButton = { TextButton(onClick = { confirm = null; perform(action) }) { Text(if (action == "delete") "Delete saved copies" else "Withdraw and pause") } },
        dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }) }
}
