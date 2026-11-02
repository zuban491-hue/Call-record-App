package com.callcapture.app.ui.screens

import com.callcapture.app.data.legal.LegalConsent
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callcapture.app.ui.components.rememberLivePermissions
import com.callcapture.app.ui.components.PermissionHealthCard
import com.callcapture.app.data.model.AudioFormatOption
import com.callcapture.app.data.model.AudioSourceOption
import com.callcapture.app.ui.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToData: () -> Unit = {},
    onNavigateToSafety: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val permissions by rememberLivePermissions()
    val context = LocalContext.current
    var pendingChannel by remember { mutableStateOf<String?>(null) }

    pendingChannel?.let { channel ->
        AlertDialog(onDismissRequest = { pendingChannel = null }, title = { Text("Enable call alerts and recording?") },
            text = { Text("This feature observes phone state or supported app call notifications, including caller labels when provided. When this app is visible and Android permits it, microphone capture can start for a connected-call signal. Otherwise, a Tap to record prompt opens this app to start capture. A matching call-notification removal or phone idle signal stops capture; this signal may be missed or inaccurate, so keep the Stop control available. Recordings stay on this device unless you share them. Both sides of calls may not be captured. Get any necessary agreement before recording. No screen-reading access is used.", Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = {
                if (!LegalConsent.accepted(context)) {
                    pendingChannel = null
                    onNavigateToTerms()
                } else {
                LegalConsent.recordFeature(context, "call:$channel")
                when (channel) {
                    "phone" -> viewModel.setCellularEnabled(true)
                    "whatsapp" -> viewModel.setWhatsAppEnabled(true)
                    "messenger" -> viewModel.setMessengerEnabled(true)
                    "telegram" -> viewModel.setTelegramEnabled(true)
                }
                pendingChannel = null
                }
            }) { Text("Agree and enable") } },
            dismissButton = { TextButton(onClick = { pendingChannel = null }) { Text("Not now") } })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PermissionHealthCard(
                permissions = permissions,
                cellularEnabled = state.isCellularEnabled,
                voipEnabled = state.isWhatsAppEnabled || state.isMessengerEnabled || state.isTelegramEnabled,
                onReviewAccess = onNavigateToPermissions,
                onManageMessages = onNavigateToMessages
            )
            TextButton(onClick = onNavigateToPrivacy) { Text("Privacy Policy") }
            TextButton(onClick = onNavigateToTerms) { Text("Terms of Use") }
            TextButton(onClick = onNavigateToData) { Text("Manage local data and consent") }
            TextButton(onClick = onNavigateToSafety) { Text("Recording and privacy notice") }
            SettingsSectionHeader(title = "Message privacy", icon = Icons.Default.Info)
            Text("Message history is optional. Saved notification text stays on this device. Review selected apps, pause saving, or delete copies in Messages.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onNavigateToMessages) { Text("Manage message history") }
            Spacer(modifier = Modifier.height(16.dp))
            // Section 1: Call Recording Sources
            SettingsSectionHeader(title = "Call alerts and recording", icon = Icons.Default.Phone)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SwitchPreference(
                        title = "Cellular Phone Calls",
                        subtitle = "Use phone-state signals; tap to record when required",
                        checked = state.isCellularEnabled,
                        onCheckedChange = { if (it && !LegalConsent.accepted(context)) onNavigateToTerms() else if (it) pendingChannel = "phone" else viewModel.setCellularEnabled(false) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SwitchPreference(
                        title = "WhatsApp Calls",
                        subtitle = "Use WhatsApp call notifications; audio support varies",
                        checked = state.isWhatsAppEnabled,
                        onCheckedChange = { if (it && !LegalConsent.accepted(context)) onNavigateToTerms() else if (it) pendingChannel = "whatsapp" else viewModel.setWhatsAppEnabled(false) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SwitchPreference(
                        title = "Facebook Messenger Calls",
                        subtitle = "Use Messenger call notifications; audio support varies",
                        checked = state.isMessengerEnabled,
                        onCheckedChange = { if (it && !LegalConsent.accepted(context)) onNavigateToTerms() else if (it) pendingChannel = "messenger" else viewModel.setMessengerEnabled(false) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SwitchPreference(
                        title = "Telegram Calls",
                        subtitle = "Use Telegram call notifications; audio support varies",
                        checked = state.isTelegramEnabled,
                        onCheckedChange = { if (it && !LegalConsent.accepted(context)) onNavigateToTerms() else if (it) pendingChannel = "telegram" else viewModel.setTelegramEnabled(false) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Audio Engine Settings
            SettingsSectionHeader(title = "Audio Engine", icon = Icons.Default.Mic)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Audio Format dropdown
                    var formatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = !formatExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${state.audioFormat.name} (.${state.audioFormat.extension})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Output Audio Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            AudioFormatOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.name} (.${option.extension})") },
                                    onClick = {
                                        viewModel.setAudioFormat(option)
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Source dropdown
                    var sourceExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded,
                        onExpandedChange = { sourceExpanded = !sourceExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.audioSource.title,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Audio Input Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false }
                        ) {
                            AudioSourceOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.title) },
                                    onClick = {
                                        viewModel.setAudioSource(option)
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: VoIP Call Enhancements
            SettingsSectionHeader(title = "Microphone Audio Settings", icon = Icons.AutoMirrored.Filled.VolumeUp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SwitchPreference(
                        title = "Speakerphone Assistant",
                        subtitle = "Changes the call audio route when enabled. This can expose the conversation nearby; remote audio capture is not guaranteed.",
                        checked = state.isSpeakerphoneAssistantEnabled,
                        onCheckedChange = { viewModel.setSpeakerphoneAssistantEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Software Gain Boost Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Software Gain Boost: ${String.format(Locale.US, "%.1fx", state.softwareGainMultiplier)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Amplifies faint voices recorded through the device acoustic chamber in WAV mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = state.softwareGainMultiplier,
                        onValueChange = { viewModel.setSoftwareGainMultiplier(it) },
                        valueRange = 1.0f..5.0f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-Way Audio Clarity Tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Audio recording limitations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Start with the standard Microphone input.\n" +
                               "• Use Speakerphone: Speaker audio may reach the microphone, but remote voice capture is not guaranteed and may be silent.\n" +
                               "• If audio is low on your device, switch Output Format to WAV and increase Software Gain Boost to 3.5x - 4.5x.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            val version = remember {
                @Suppress("DEPRECATION")
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "Unknown"
            }
            Text("Call Capture $version", style = MaterialTheme.typography.titleMedium)
            Text("Target SDK 37. No in-app APK downloader or self-updater. Use the official distribution source for updates.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
