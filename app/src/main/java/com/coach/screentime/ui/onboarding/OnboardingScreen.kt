package com.coach.screentime.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Set up Screen Time Coach",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Three permissions, one at a time. Each step opens a system screen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            PermissionCard(
                title = "Usage access",
                rationale = "Lets the coach see which app is in the foreground and how long you've used it.",
                granted = state.hasUsageAccess,
                onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
                onRefresh = viewModel::refresh,
            )
        }

        item {
            PermissionCard(
                title = "Display over other apps",
                rationale = "Required to show the mindfulness pause and the AI negotiation overlay on top of your apps.",
                granted = state.hasOverlay,
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
                onRefresh = viewModel::refresh,
            )
        }

        item {
            PermissionCard(
                title = "Notifications",
                rationale = "For the persistent tracker notification and soft-limit warnings.",
                granted = state.hasNotifications,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRefresh = viewModel::refresh,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Battery optimization", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Most OEMs (Samsung, Xiaomi, Oppo, OnePlus, Realme) aggressively kill background services. Open the system battery settings and exempt this app.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }) { Text("Open battery settings") }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.finishOnboarding(); onDone() },
                enabled = state.ready,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.ready) "Start observing (3-day calibration)"
                    else "Grant the required permissions to continue",
                )
            }
            Text(
                "The coach starts in observe-only mode. After a few days of data, you'll set limits.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    granted: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp))
                Text(
                    if (granted) "✓ granted" else "needed",
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(rationale, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onClick) { Text("Open settings") }
                    OutlinedButton(onClick = onRefresh) { Text("I granted it") }
                }
            }
        }
    }
}
