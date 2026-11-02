package com.callcapture.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callcapture.app.R
import com.callcapture.app.ui.components.PermissionTick
import com.callcapture.app.ui.components.rememberLivePermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(onNavigateBack: () -> Unit, onRunSetup: () -> Unit = {}) {
    val context = LocalContext.current
    val access by rememberLivePermissions()
    var message by remember { mutableStateOf<String?>(null) }
    fun open(intent: Intent) {
        try { context.startActivity(intent) }
        catch (_: Exception) { message = "Open Android Settings, then Apps, and select this app." }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Permission details") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PermissionTick("Microphone", access.microphone, "Needed to capture microphone sound")
            PermissionTick("Recording notifications", access.notifications, "Recording status, Stop and tap-to-record prompts")
            PermissionTick("Notification access", access.notificationAccess, "Selected message history and call notification signals")
            PermissionTick("Phone state", access.phone, "Optional cellular call-state signals")
            Text("Only enable access for features you want. No screen-reading or Accessibility service is used. Permissions do not unlock the remote side of third-party calls.")
            Button(onClick = onRunSetup, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Open guided setup") }
            Text("In Notification access, choose: ${context.getString(R.string.notification_listener_name)}", style = MaterialTheme.typography.bodySmall)
            Text("If Android blocks a trusted app with Restricted setting, review Allow restricted settings in this app's App info menu, if offered. Android confirmation is always required.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app settings") }
            PermissionTick("Battery exemption", access.batteryExempt, "Optional troubleshooting, not required for setup")
            Text("If work stops when the screen is off, review the phone's battery settings. Labels vary. Changing them may increase battery use and does not enable otherwise blocked call audio.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) { Text("Review battery settings") }
            message?.let { Text(it) }
            TextButton(onClick = onNavigateBack) { Text("Done") }
        }
    }
}
