package com.coach.screentime.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.ui.components.PunishmentBanner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        TasksUiState.Loading -> CenteredText("Loading…")
        TasksUiState.NotConfigured -> CenteredText("Google Tasks integration is not configured. Set GOOGLE_OAUTH_CLIENT_ID in local.properties and rebuild.")
        TasksUiState.Disconnected -> CenteredText("Connect Google Tasks in Settings to enable task enforcement.")
        is TasksUiState.Ready -> ReadyContent(s)
    }
}

@Composable
private fun CenteredText(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReadyContent(state: TasksUiState.Ready) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.activePunishments) { p ->
            PunishmentBanner(p)
        }

        item {
            Text(
                "Connected as ${state.email ?: "(unknown)"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        if (state.tasks.isEmpty()) {
            item {
                Text(
                    "No open tasks. Add one in Google Tasks.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.tasks) { row -> TaskCard(row) }
    }
}

@Composable
private fun TaskCard(row: TaskRow) {
    val accent = if (row.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(row.task.title, fontWeight = FontWeight.SemiBold)
            row.task.dueDateMs?.let { due ->
                val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                val days = (LocalDate.now().toEpochDay() - date.toEpochDay()).toInt()
                val label = when {
                    days > 0 -> "${days}d overdue"
                    days == 0 -> "due today"
                    else -> "due in ${-days}d"
                }
                Spacer(Modifier.height(2.dp))
                Text(label, color = accent, fontSize = 12.sp)
            }
            if (row.task.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    row.task.notes,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            val statusLabel = when (row.state) {
                "working" -> "you're on it"
                "delayed" -> "delayed"
                "prompted" -> "waiting on you"
                "dismissed_pending" -> "punished — task still due"
                "judged_done" -> "marked done"
                else -> ""
            }
            if (statusLabel.isNotBlank()) {
                Text("· $statusLabel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (row.grantedDelayCount > 0) {
                Text(
                    "Coach has granted ${row.grantedDelayCount} delay(s) for this task.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
