package com.callcapture.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.callcapture.app.ui.components.rememberLivePermissions
import com.callcapture.app.ui.components.PermissionTick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callcapture.app.R
import com.callcapture.app.data.messages.MessageHistoryStore
import com.callcapture.app.service.VoipNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(onReviewTerms: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { MessageHistoryStore.get(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var newMessages by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val permissions by rememberLivePermissions()
    val revision by store.changes.collectAsStateWithLifecycle()
    val connected by VoipNotificationListenerService.historyConnected.collectAsStateWithLifecycle()
    val captureError by VoipNotificationListenerService.historyError.collectAsStateWithLifecycle()
    var resume by remember { mutableStateOf(0) }
    var enabled by remember { mutableStateOf(store.enabled) }
    val access = permissions.notificationAccess
    var entries by remember { mutableStateOf(emptyList<MessageHistoryStore.Entry>()) }
    var selected by remember { mutableStateOf(MessageHistoryStore.APPS.keys.filter { store.selected(it) }.toSet()) }
    var query by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var clear by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun change(action: () -> Unit) {
        scope.launch {
            busy = true
            try { withContext(Dispatchers.IO) { action() } }
            catch (_: Exception) { error = "Couldn't update message history. Please try again." }
            finally { busy = false }
        }
    }
    fun openSettings() {
        try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        catch (_: Exception) { error = "Open Android Settings and search for Notification access." }
    }
    LaunchedEffect(revision, resume) {
        enabled = store.enabled
        selected = MessageHistoryStore.APPS.keys.filter { store.selected(it) }.toSet()
        try {
            val oldFirst = entries.firstOrNull()?.id
            val followLatest = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            val latest = withContext(Dispatchers.IO) { store.load() }
            val incoming = latest.firstOrNull()?.let { it.id != oldFirst && it.received >= (entries.firstOrNull()?.received ?: 0L) } == true
            entries = latest
            if (incoming) {
                if (followLatest) listState.scrollToItem(0) else newMessages = true
            }
        }
        catch (_: Exception) { error = "Couldn't load saved notifications. Please try again." }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resume += 1
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val visible = remember(entries, query) { entries.filter {
        query.isBlank() || "${it.title} ${it.sender} ${it.body} ${MessageHistoryStore.APPS[it.pkg]}".contains(query, true)
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Messages") }) }, floatingActionButton = {
        if (newMessages) ExtendedFloatingActionButton(onClick = {
            scope.launch { listState.animateScrollToItem(0); newMessages = false }
        }) { Text("Latest saved messages") }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(when {
                    !enabled -> "Message history is off"
                    !access -> "Notification access needed"
                    !connected -> "Waiting for Android to connect"
                    selected.isEmpty() -> "Choose an app to save messages"
                    else -> "Saving new notification text"
                }, style = MaterialTheme.typography.titleMedium)
                PermissionTick("Notification access", access)
                Spacer(Modifier.height(8.dp))
                Text("Keeps a local copy of new WhatsApp and Messenger notification text. A saved copy can remain after the sender deletes it. This does not recover old messages or identify which messages were unsent.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                if (!com.callcapture.app.data.legal.LegalConsent.accepted(context)) {
                    Text("Review Terms before enabling new message saving.")
                    TextButton(onClick = onReviewTerms) { Text("Review Terms of Use") }
                }
                if (!enabled) Button(enabled = !busy, onClick = {
                    if (com.callcapture.app.data.legal.LegalConsent.accepted(context)) consent = true else onReviewTerms()
                }) { Text("Enable message history") }
                else if (!access) Button(onClick = ::openSettings) { Text("Open notification access") }
                TextButton(onClick = { options = !options }) { Text(if (options) "Hide controls" else "Manage history") }
            }
            if (options) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected apps", style = MaterialTheme.typography.titleSmall)
                    MessageHistoryStore.APPS.forEach { (pkg, label) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, Modifier.weight(1f).padding(top = 12.dp))
                            Switch(checked = pkg in selected, enabled = !busy,
                                onCheckedChange = { value -> change { store.select(pkg, value) } })
                        }
                    }
                    Text("Text only. No photos, voice notes, uploads or cloud backup. Saved for up to 30 days, with a limit of 5,000 entries. Older entries are removed when history is opened or updated. Pausing keeps existing copies.",
                        style = MaterialTheme.typography.bodySmall)
                    if (enabled) OutlinedButton(enabled = !busy, onClick = { change { store.setEnabled(false) } }) { Text("Pause saving") }
                    OutlinedButton(onClick = ::openSettings) { Text("Manage Android notification access") }
                    Text("Select: ${context.getString(R.string.notification_listener_name)}. Android access is shared with the existing call detector. Pausing message history does not change call settings.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Restricted setting? Only if you trust this app, open App info and review Allow restricted settings in the menu, if offered. Menus vary by phone.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
                        catch (_: Exception) { error = "Open Android Settings, then Apps, and select this app." }
                    }) { Text("Open app info") }
                    TextButton(enabled = entries.isNotEmpty() && !busy, onClick = { clear = true }) { Text("Delete all saved messages") }
                }
            }
            item {
                Text("Only text Android shares in notifications can be saved. Muted, hidden, protected or missing notifications may not be captured. Permission being enabled does not guarantee capture.",
                    style = MaterialTheme.typography.bodySmall)
                if (captureError) Text("Some notifications could not be saved. Check available device storage.", color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (entries.isNotEmpty()) item {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search saved messages") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (visible.isEmpty()) item {
                Text(if (query.isNotBlank()) "No matching saved messages" else "No saved messages yet", style = MaterialTheme.typography.titleMedium)
                if (query.isBlank()) Text("After enabling history and Android access, ask someone to send a new message while the chat is closed. Its notification text may appear here.")
            }
            items(visible, key = { it.id }) { entry ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                                if (entry.sender != entry.title) Text(entry.sender, style = MaterialTheme.typography.bodySmall)
                                Text("${MessageHistoryStore.APPS[entry.pkg]} · Saved notification", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(enabled = !busy, onClick = { deleteId = entry.id }) { Icon(Icons.Default.Delete, "Delete saved message") }
                        }
                        SelectionContainer { Text(entry.body) }
                        Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.received)),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("Save notification text?") },
        text = { Text("Android notification access can expose notifications from other apps. This feature saves only text and sender/conversation labels from the WhatsApp and Messenger apps you select, on this device. It starts after you enable it. Copies can remain after a sender deletes a message. Nothing is uploaded. You can pause, delete history, or revoke access. Only enable this on your own device with an understanding of others' privacy. Android may hide sensitive content.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            consent = false
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { store.setEnabled(true) }
                    if (!store.hasNotificationAccess()) openSettings()
                } catch (_: Exception) { error = "Couldn't enable history. Please try again." }
                finally { busy = false }
            }
        }) { Text("Agree and continue") } },
        dismissButton = { TextButton(onClick = { consent = false }) { Text("Not now") } })
    if (clear || deleteId != null) AlertDialog(onDismissRequest = { clear = false; deleteId = null },
        title = { Text(if (clear) "Delete all saved messages?" else "Delete this saved message?") },
        text = { Text("This permanently removes the saved copy here. It does not change messages in WhatsApp or Messenger.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val all = clear; val id = deleteId
            clear = false; deleteId = null
            change { if (all) store.clear() else if (id != null) store.delete(id) }
        }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { clear = false; deleteId = null }) { Text("Cancel") } })
}
