package com.callcapture.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callcapture.app.R
import com.callcapture.app.data.legal.LegalConsent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private data class LegalDocument(val title: String, val version: String, val date: String, val sections: List<Pair<String, String>>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(kind: String, onBack: () -> Unit, onAccepted: () -> Unit = {}) {
    val context = LocalContext.current
    var document by remember(kind) { mutableStateOf<LegalDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var agreed by rememberSaveable(kind) { mutableStateOf(false) }
    val accepted = LegalConsent.accepted(context)
    val email = context.getString(R.string.legal_contact_email).trim()
    val privacyUrl = context.getString(R.string.legal_privacy_url).trim()
    val termsUrl = context.getString(R.string.legal_terms_url).trim()
    fun open(intent: Intent) {
        try { context.startActivity(intent) }
        catch (_: Exception) { error = "No app could open this link. Use the developer contact in the store listing." }
    }
    LaunchedEffect(kind) {
        try {
            document = withContext(Dispatchers.IO) {
                val root = JSONObject(context.assets.open("legal/documents.json").bufferedReader().use { it.readText() })
                val source = root.getJSONObject(kind)
                val replacements = mapOf("{{DEVELOPER_NAME}}" to context.getString(R.string.legal_developer_name).ifBlank { "the developer identified in the official store listing" },
                    "{{CONTACT_EMAIL}}" to email.ifBlank { "the developer contact in the official store listing" },
                    "{{PRIVACY_URL}}" to privacyUrl.ifBlank { "the privacy-policy link in the official store listing" },
                    "{{TERMS_URL}}" to termsUrl.ifBlank { "the Terms link on the official distribution page" })
                val array = source.getJSONArray("sections")
                LegalDocument(source.getString("title"), source.getString("version"), source.getString("effectiveDate"),
                    (0 until array.length()).map { index ->
                        val section = array.getJSONArray(index)
                        var text = section.getString(1)
                        replacements.forEach { (token, value) -> text = text.replace(token, value) }
                        section.getString(0) to text
                    })
            }
        } catch (_: Exception) { error = "Couldn't load this document. Please reopen it before agreeing." }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(document?.title ?: "Legal information") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            document?.let { doc ->
                Text("Call Capture · ${doc.version} · ${doc.date}", style = MaterialTheme.typography.labelSmall)
                doc.sections.forEach { (title, body) ->
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body)
                }
                if (kind == "terms") {
                    if (accepted) Text("Terms accepted on ${DateFormat.getDateTimeInstance().format(Date(LegalConsent.acceptedAt(context)))}. Optional feature consents remain separate.")
                    else {
                        Row {
                            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                            Text("I agree to these Terms of Use.", Modifier.padding(top = 12.dp))
                        }
                        Button(enabled = agreed && doc.version == LegalConsent.VERSION, onClick = {
                            LegalConsent.accept(context); onAccepted()
                        }) { Text("Agree to Terms") }
                    }
                }
                val publicUrl = when (kind) { "privacy" -> privacyUrl; "terms" -> termsUrl; else -> "" }
                if (publicUrl.startsWith("https://")) TextButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(publicUrl))) }) { Text("Open published document") }
                if (email.isNotBlank()) TextButton(onClick = {
                    open(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null)).putExtra(Intent.EXTRA_SUBJECT, "Call Capture privacy/support inquiry"))
                }) { Text("Contact developer") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalWelcomeScreen(onPrivacy: () -> Unit, onTerms: () -> Unit, onAccepted: () -> Unit, onViewSaved: () -> Unit) {
    val context = LocalContext.current
    var agree by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onViewSaved)
    Scaffold(topBar = { TopAppBar(title = { Text("Welcome to Call Capture") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Your recordings. Your choices.", style = MaterialTheme.typography.headlineSmall)
            Text("Review the Privacy Policy and Terms of Use. Optional call and message features stay off until you choose them. Existing saved data remains available if you decline.")
            TextButton(onClick = onPrivacy) { Text("Read Privacy Policy") }
            TextButton(onClick = onTerms) { Text("Read Terms of Use") }
            Row {
                Checkbox(checked = agree, onCheckedChange = { agree = it })
                Text("I agree to the Terms of Use.", Modifier.padding(top = 12.dp))
            }
            Text("This agreement does not grant Android permissions or consent to recording/message collection. Those choices are explained separately before activation.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = agree, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                LegalConsent.accept(context); onAccepted()
            }) { Text("Agree and continue") }
            TextButton(onClick = onViewSaved, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("View saved data without enabling features") }
        }
    }
}
