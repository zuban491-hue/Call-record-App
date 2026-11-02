package com.callcapture.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.callcapture.app.R
import com.callcapture.app.data.messages.MessageHistoryStore
import com.callcapture.app.ui.components.PermissionTick
import com.callcapture.app.ui.components.rememberLivePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.setupActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** First launch: show a step first; launch Android dialogs only after an explicit tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunSetupScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val activity = context.setupActivity()
    val permissions by rememberLivePermissions()
    val prefs = remember { context.getSharedPreferences("guided_setup_v2", Context.MODE_PRIVATE) }
    val attempts = remember { context.getSharedPreferences("permission_setup_ui", Context.MODE_PRIVATE) }
    val history = remember { MessageHistoryStore.get(context) }
    val scope = rememberCoroutineScope()
    val revision by history.changes.collectAsState()
    var historyEnabled by remember { mutableStateOf(history.enabled) }
    val steps = remember {
        buildList {
            add("microphone")
            if (Build.VERSION.SDK_INT >= 33) add("notifications")
            add("messages")
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) add("phone")
        }
    }
    var index by rememberSaveable { mutableStateOf((if (prefs.getBoolean("handled", false)) 0 else prefs.getInt("step", 0)).coerceIn(0, steps.size)) }
    var awaiting by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var consent by rememberSaveable { mutableStateOf<String?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val step = steps.getOrNull(index)
    fun next() { awaiting = null; message = null; showHelp = false; index = (index + 1).coerceAtMost(steps.size) }
    fun finish() {
        // An explicit later/finish choice suppresses repeated onboarding. Permissions remain revocable.
        prefs.edit().putBoolean("handled", true).putInt("step", index).apply()
        onFinish()
    }
    fun open(intent: Intent): Boolean = try { context.startActivity(intent); true }
        catch (_: Exception) { message = "This page is unavailable. Open Android Settings, then Apps, and select this app."; false }
    fun appInfo() { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
    fun specialAccess(kind: String) {
        val action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        awaiting = if (open(Intent(action))) kind else null
    }
    fun permanentDenial(permission: String, key: String): Boolean =
        attempts.getBoolean(key, false) && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { awaiting = null; message = "Microphone is off. Recording needs this access. You can try again, open app settings, or continue later." }
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { awaiting = null; message = "Notifications are off. You can review app settings or skip this step." }
    }
    val requestPhone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { awaiting = null; message = "Phone-state access is off. Cellular detection may be unavailable. You can skip it." }
    }
    val granted = when (step) {
        "microphone" -> permissions.microphone
        "notifications" -> permissions.notifications
        "messages" -> historyEnabled && permissions.notificationAccess
        "phone" -> permissions.phone
        else -> false
    }
    LaunchedEffect(revision) { historyEnabled = history.enabled }
    LaunchedEffect(index) { prefs.edit().putInt("step", index).apply() }
    LaunchedEffect(step, granted, awaiting) {
        if (step != null && awaiting == step && granted) next()
    }
    BackHandler {
        if (busy) return@BackHandler
        if (index > 0) { awaiting = null; message = null; index -= 1 } else finish()
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Quick setup") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (step == null) {
                Text("Your choices are saved", style = MaterialTheme.typography.headlineSmall)
                Text("You can change access later in Settings. Enabled permissions do not guarantee that this phone can record call audio.")
                PermissionTick("Microphone", permissions.microphone)
                PermissionTick("Recording notifications", permissions.notifications)
                PermissionTick("Notification access", permissions.notificationAccess)
                PermissionTick("Phone state", permissions.phone)
                Text(if (historyEnabled) "Message history: enabled. Android access and notification delivery are also needed."
                    else "Message history: off.")
                Text("Call-recording channels stay as you configured them. Setup does not turn on automatic recording or change battery settings.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = ::finish, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Open app") }
            } else {
                Text("Step ${index + 1} of ${steps.size}", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { index.toFloat() / steps.size }, modifier = Modifier.fillMaxWidth())
                val title = when (step) {
                    "microphone" -> "Allow microphone"
                    "notifications" -> "Show recording status"
                    "messages" -> "Keep message notifications"
                    else -> "Detect phone calls"
                }
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(when (step) {
                    "microphone" -> "Needed to capture microphone sound. Android will ask you to approve access. Other people may be recorded, so get any necessary agreement. This does not unlock both sides of a call."
                    "notifications" -> "Optional. Show recording status and the Stop control in Android notifications. This permission does not read messages."
                    "messages" -> "Optional. Save new notification text from selected WhatsApp and Messenger apps on this device. Copies can remain after the sender deletes a message. Only notification text shared by Android can be saved."
                    else -> "Optional. Used by cellular call-state detection. This requests phone state, not contacts or your call log, and does not provide call audio."
                })
                PermissionTick(if (step == "messages") "Message history and notification access" else title, granted)
                if (step == "messages" && historyEnabled) Text("In Notification access, choose: ${context.getString(R.string.notification_listener_name)}")
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                    message = null
                    if (granted) next() else when (step) {
                        "microphone" -> {
                            awaiting = step
                            if (permanentDenial(Manifest.permission.RECORD_AUDIO, "audioAsked")) appInfo()
                            else {
                                attempts.edit().putBoolean("audioAsked", true).apply()
                                requestMic.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        "notifications" -> {
                            awaiting = step
                            if (permanentDenial(Manifest.permission.POST_NOTIFICATIONS, "notificationsAsked")) appInfo()
                            else if (Build.VERSION.SDK_INT >= 33) {
                                attempts.edit().putBoolean("notificationsAsked", true).apply()
                                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        "messages" -> if (historyEnabled) specialAccess(step) else consent = step
                        "phone" -> {
                            awaiting = step
                            if (permanentDenial(Manifest.permission.READ_PHONE_STATE, "phoneAsked")) appInfo()
                            else {
                                attempts.edit().putBoolean("phoneAsked", true).apply()
                                requestPhone.launch(Manifest.permission.READ_PHONE_STATE)
                            }
                        }
                        else -> Unit
                    }
                }) { Text(if (granted) "Continue" else when (step) {
                    "messages" -> if (historyEnabled) "Open notification access" else "Review message history"
                    else -> "Continue"
                }) }
                TextButton(enabled = !busy, onClick = ::next, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Not now") }
                if (step == "messages" && !granted) {
                    TextButton(onClick = { showHelp = !showHelp }) { Text("Need help?") }
                    if (showHelp) {
                        Text("Use the exact service name above. If Android shows Restricted setting, open App info and review Allow restricted settings in the menu, if offered. Only approve if you understand and trust this app. Menu names vary by phone.")
                        OutlinedButton(onClick = ::appInfo) { Text("Open this app's settings") }
                    }
                }
                TextButton(enabled = !busy, onClick = ::finish) { Text("Finish later") }
            }
        }
    }
    consent?.let { kind ->
        AlertDialog(onDismissRequest = { consent = null }, title = { Text("Save notification text?") },
            text = { Text(
                "Android notification access may expose notifications from other apps. This feature saves only text and sender/conversation labels from selected WhatsApp and Messenger apps, locally on this device. Nothing is uploaded. Copies can remain after a sender deletes a message. It cannot recover old or hidden messages, photos or voice notes. Retained for up to 30 days and 5,000 entries; old entries are removed when history is opened or updated. You can choose apps, pause saving, delete copies or revoke access in Messages. Android may protect sensitive text."
                , Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                consent = null
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) { history.setEnabled(true) }
                        historyEnabled = true
                        if (permissions.notificationAccess) next() else specialAccess(kind)
                    } catch (_: Exception) { message = "Couldn't enable message history. Please try again." }
                    finally { busy = false }
                }
            }) { Text("Agree and continue") } },
            dismissButton = { TextButton(onClick = { consent = null }) { Text("Not now") } })
    }
}
