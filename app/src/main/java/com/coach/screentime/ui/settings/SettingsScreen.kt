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
    }
}
