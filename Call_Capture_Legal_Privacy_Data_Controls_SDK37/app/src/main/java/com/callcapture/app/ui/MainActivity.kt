package com.callcapture.app.ui

import com.callcapture.app.data.legal.LegalConsent
import android.os.Bundle
import android.content.Intent
import com.callcapture.app.service.CallRecordingCoordinator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.callcapture.app.ui.screens.*
import com.callcapture.app.ui.theme.OmniCallTheme
import com.callcapture.app.ui.viewmodel.MainViewModel
import com.callcapture.app.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var pendingOfferToken: String? = null
    private fun readTap(intent: Intent?) {
        if (intent?.action == CallRecordingCoordinator.ACTION_RECORD_FROM_TAP)
            pendingOfferToken = intent.getStringExtra(CallRecordingCoordinator.EXTRA_OFFER_TOKEN)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readTap(intent)
    }
    override fun onPostResume() {
        super.onPostResume()
        pendingOfferToken?.let {
            pendingOfferToken = null
            intent?.removeExtra(CallRecordingCoordinator.EXTRA_OFFER_TOKEN)
            intent?.action = null
            CallRecordingCoordinator.startFromTap(this, it)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readTap(intent)
        enableEdgeToEdge()
        setContent {
            OmniCallTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppNavigation(mainViewModel, settingsViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val needsFirstSetup = remember { !context.getSharedPreferences("guided_setup_v2", Context.MODE_PRIVATE).getBoolean("handled", false) }
    val initialRoute = remember { if (!LegalConsent.accepted(context)) "welcome" else if (needsFirstSetup) "first_setup" else "records" }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "records"
    val tabs = listOf("records" to "Records", "messages" to "Messages", "settings" to "Settings")
    fun navigateTab(target: String) {
        if (route == target) return
        nav.navigate(target) {
            popUpTo("records") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun afterAgreement() {
        nav.navigate(if (needsFirstSetup) "first_setup" else "records") {
            popUpTo(nav.graph.id)
            launchSingleTop = true
        }
    }
    fun reviewAccess() {
        nav.navigate(if (LegalConsent.accepted(context)) "permissions" else "terms") { launchSingleTop = true }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (route in tabs.map { it.first }) NavigationBar {
            tabs.forEach { (target, label) ->
                NavigationBarItem(selected = route == target, onClick = { navigateTab(target) },
                    icon = { Icon(when (target) {
                        "records" -> Icons.Default.Folder
                        "messages" -> Icons.Default.ChatBubbleOutline
                        else -> Icons.Default.Settings
                    }, contentDescription = null) }, label = { Text(label) })
            }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = initialRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("welcome") {
                LegalWelcomeScreen(onPrivacy = { nav.navigate("privacy") }, onTerms = { nav.navigate("terms") },
                    onAccepted = ::afterAgreement, onViewSaved = {
                        nav.navigate("records") { popUpTo(nav.graph.id); launchSingleTop = true }
                    })
            }
            composable("terms") { LegalDocumentScreen("terms", onBack = { nav.popBackStack() }, onAccepted = ::afterAgreement) }
            composable("deletion_policy") { LegalDocumentScreen("data", onBack = { nav.popBackStack() }) }
            composable("safety") { LegalDocumentScreen("safety", onBack = { nav.popBackStack() }) }
            composable("data_controls") {
                LocalDataScreen(onBack = { nav.popBackStack() }, onBeforeDelete = { mainViewModel.stopPlayback() },
                    onSettingsChanged = { settingsViewModel.refreshFromPreferences() }, onWithdrawn = {
                        nav.navigate("welcome") { popUpTo(nav.graph.id); launchSingleTop = true }
                    }, onReadDeletion = { nav.navigate("deletion_policy") })
            }
            composable("first_setup") {
                FirstRunSetupScreen(onFinish = {
                    nav.navigate("records") {
                        popUpTo(nav.graph.id)
                        launchSingleTop = true
                    }
                })
            }
            composable("records") {
                HomeScreen(mainViewModel,
                    onNavigateToSettings = { navigateTab("settings") },
                    onNavigateToPermissions = ::reviewAccess,
                    onNavigateToTerms = { nav.navigate("terms") { launchSingleTop = true } })
            }
            composable("messages") { MessagesScreen(onReviewTerms = { nav.navigate("terms") { launchSingleTop = true } }) }
            composable("settings") {
                SettingsScreen(settingsViewModel, onNavigateBack = { navigateTab("records") },
                    onNavigateToMessages = { navigateTab("messages") },
                    onNavigateToPermissions = ::reviewAccess,
                    onNavigateToPrivacy = { nav.navigate("privacy") { launchSingleTop = true } },
                    onNavigateToTerms = { nav.navigate("terms") { launchSingleTop = true } },
                    onNavigateToData = { nav.navigate("data_controls") { launchSingleTop = true } },
                    onNavigateToSafety = { nav.navigate("safety") { launchSingleTop = true } })
            }
            composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
            composable("permissions") {
                PermissionSetupScreen(onNavigateBack = { nav.popBackStack() },
                    onRunSetup = { nav.navigate("first_setup") { launchSingleTop = true } })
            }
        }
    }
}
