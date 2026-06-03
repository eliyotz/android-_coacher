package com.coach.screentime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.data.store.Mode
import com.coach.screentime.data.store.Strictness

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Coach mode", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.values().forEach { m ->
                        FilterChip(
                            selected = state.mode == m,
                            onClick = { viewModel.setMode(m) },
                            label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.mode == Mode.OBSERVE) "Tracking only — no overlays will fire."
                    else "Overlays and AI negotiation are active for flagged apps.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Strictness", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Strictness.values().forEach { s ->
                        FilterChip(
                            selected = state.strictness == s,
                            onClick = { viewModel.setStrictness(s) },
                            label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (state.strictness) {
                        Strictness.GENTLE -> "Coach leans toward granting extensions."
                        Strictness.BALANCED -> "Coach grants real needs, rejects vague rationalizations."
                        Strictness.STRICT -> "Coach is tough — only clearly justified, time-bounded needs."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Long-term goal", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fed into every coach prompt. One sentence, in your own words.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                var draft by remember(state.goal) { mutableStateOf(state.goal) }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("e.g. \"I want to read more before bed.\"") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = { viewModel.setGoal(draft) },
                ) { Text("Save goal") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Mindfulness pause", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 5, 8).forEach { sec ->
                        FilterChip(
                            selected = state.pauseSec == sec,
                            onClick = { viewModel.setPauseSec(sec) },
                            label = { Text(if (sec == 0) "Off" else "${sec}s") },
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Extension grant", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { min ->
                        FilterChip(
                            selected = state.extensionMinutes == min,
                            onClick = { viewModel.setExtensionMinutes(min) },
                            label = { Text("${min}m") },
                        )
                    }
                }
            }
        }

        CoachEnforcementCard(viewModel)
        CheckInsCard(viewModel)
        GoogleTasksCard(viewModel)
        ExportCard(viewModel)
    }
}

@Composable
private fun CoachEnforcementCard(viewModel: SettingsViewModel) {
    val punishmentsEnabled by viewModel.punishmentsEnabled.collectAsState()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Coach enforcement", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !punishmentsEnabled,
                    onClick = { viewModel.setPunishmentsEnabled(false) },
                    label = { Text("Coach only") },
                )
                FilterChip(
                    selected = punishmentsEnabled,
                    onClick = { viewModel.setPunishmentsEnabled(true) },
                    label = { Text("Allow punishments") },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (punishmentsEnabled)
                    "When you ignore or decline an overdue task, the coach may block apps, force Focus mode, or shrink today's caps."
                else
                    "The coach reminds and reflects, but never blocks apps or forces Focus mode. Overdue tasks get a gentle, dismissible nudge.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun CheckInsCard(viewModel: SettingsViewModel) {
    val nudges by viewModel.nudgesEnabled.collectAsState()
    val reflection by viewModel.reflectionEnabled.collectAsState()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Coach check-ins", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Optional, off by default. Occasional AI notifications — leave both off for a silent app.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = nudges,
                    onClick = { viewModel.setNudgesEnabled(!nudges) },
                    label = { Text("AI nudges") },
                )
                FilterChip(
                    selected = reflection,
                    onClick = { viewModel.setReflectionEnabled(!reflection) },
                    label = { Text("Morning reflection") },
                )
            }
        }
    }
}

@Composable
private fun GoogleTasksCard(viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val connection by viewModel.googleConnection.collectAsState()
    val activity = context as? android.app.Activity

    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && activity != null) {
            val data = result.data
            try {
                val authResult = com.google.android.gms.auth.api.identity.Identity
                    .getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(data)
                val code = authResult.serverAuthCode
                val email = null as String?
                if (!code.isNullOrBlank()) {
                    viewModel.onGoogleSignedIn(code, email)
                }
            } catch (_: com.google.android.gms.common.api.ApiException) {
                // User cancelled or no consent — ignore.
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Google Tasks", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "When connected, the coach reads your overdue tasks and sends a gentle, dismissible reminder. Whether it can ever block apps is governed by Coach enforcement above — off by default.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            when {
                !connection.configured -> Text(
                    "Set GOOGLE_OAUTH_CLIENT_ID in local.properties and rebuild.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                )
                connection.connected -> {
                    Text(
                        "Connected${connection.email?.let { " as $it" } ?: ""}.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.disconnectGoogle() },
                    ) { Text("Disconnect") }
                }
                else -> {
                    androidx.compose.material3.Button(
                        onClick = {
                            if (activity == null) return@Button
                            val request = com.google.android.gms.auth.api.identity.AuthorizationRequest.builder()
                                .setRequestedScopes(
                                    listOf(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/tasks.readonly"))
                                )
                                .requestOfflineAccess(com.coach.screentime.BuildConfig.GOOGLE_OAUTH_CLIENT_ID, /* forceCodeForRefreshToken= */ true)
                                .build()
                            com.google.android.gms.auth.api.identity.Identity
                                .getAuthorizationClient(activity)
                                .authorize(request)
                                .addOnSuccessListener { result ->
                                    if (result.hasResolution()) {
                                        val sender = result.pendingIntent?.intentSender
                                        if (sender != null) {
                                            signInLauncher.launch(
                                                androidx.activity.result.IntentSenderRequest.Builder(sender).build()
                                            )
                                        }
                                    } else {
                                        // Already authorized — pull serverAuthCode directly.
                                        result.serverAuthCode?.let {
                                            viewModel.onGoogleSignedIn(it, null)
                                        }
                                    }
                                }
                        },
                    ) { Text("Connect Google Tasks") }
                }
            }
        }
    }
}

@Composable
private fun ExportCard(viewModel: SettingsViewModel) {
    val status by viewModel.exportStatus.collectAsState()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        if (uri != null) viewModel.exportTo(uri)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Export data", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Writes a JSON file with sessions, rollups, interventions, AI verdicts, weekly reports, reflections, and nudges.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val name = "screen-time-coach-${java.time.LocalDate.now()}.json"
                    launcher.launch(name)
                },
                enabled = status !is ExportStatus.Running,
            ) {
                Text(if (status is ExportStatus.Running) "Exporting…" else "Export to file")
            }
            when (val s = status) {
                is ExportStatus.Done -> Text(
                    "Wrote ${s.byteCount} bytes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                is ExportStatus.Failed -> Text(
                    s.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
                else -> {}
            }
        }
    }
}
