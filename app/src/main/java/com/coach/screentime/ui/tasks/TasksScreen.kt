package com.coach.screentime.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.components.Pill
import com.coach.screentime.ui.components.PunishmentBanner
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Alert
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Paper
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.SageGreen
import com.coach.screentime.ui.theme.WarnGold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val sheetTask by viewModel.sheetTask.collectAsState()
    val coachReply by viewModel.coachReply.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show coach reply as a Snackbar whenever it arrives
    LaunchedEffect(coachReply) {
        val reply = coachReply ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(reply)
        viewModel.clearCoachReply()
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Ink,
                    contentColor = Paper,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val s = state) {
                TasksUiState.Loading -> HelperText("Loading…")
                TasksUiState.NotConfigured -> HelperText("Google Tasks integration is not configured.\nSet GOOGLE_OAUTH_CLIENT_ID in local.properties and rebuild.")
                TasksUiState.Disconnected -> HelperText("Connect Google Tasks in Settings to enable task enforcement.")
                is TasksUiState.Ready -> {
                    val syncing by viewModel.syncing.collectAsState()
                    ReadyContent(
                        state = s,
                        syncing = syncing,
                        onSyncClick = viewModel::sync,
                        onTaskTap = { row -> viewModel.openSheet(row) },
                    )
                }
            }
        }
    }

    // Bottom sheet: shown whenever a task is tapped (outside Scaffold so it overlays fully)
    sheetTask?.let { row ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = Paper,
        ) {
            TaskActionSheet(
                row = row,
                onMarkWorking = { viewModel.markWorking(row.task.googleId) },
                onMarkCompleted = { viewModel.markCompleted(row.task.googleId) },
                onSubmitReason = { reason -> viewModel.submitReason(row.task.googleId, reason) },
                onDismiss = { viewModel.closeSheet() },
            )
        }
    }
}

@Composable
private fun HelperText(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = CoachType.headlineSm.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            color = InkMute,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ReadyContent(
    state: TasksUiState.Ready,
    syncing: Boolean = false,
    onSyncClick: () -> Unit = {},
    onTaskTap: (TaskRow) -> Unit = {},
) {
    val overdue  = state.tasks.filter { it.isOverdue && it.state != "judged_done" }
    val today    = state.tasks.filter { !it.isOverdue && it.task.dueDateMs?.let { d ->
        Instant.ofEpochMilli(d).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    } == true && it.state != "judged_done" }
    val upcoming = state.tasks.filter { !it.isOverdue && it.task.dueDateMs?.let { d ->
        Instant.ofEpochMilli(d).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now())
    } == true && it.state != "judged_done" }
    val done     = state.tasks.filter { it.state == "judged_done" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Eyebrow("Tasks · Google", modifier = Modifier.padding(bottom = 4.dp))
                    Text("Open loops.", style = CoachType.headlineMd, color = Ink)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.email?.let { email ->
                        Pill(
                            text = email.take(20),
                            leading = {
                                Box(Modifier.size(6.dp).background(SageGreen, RoundedCornerShape(3.dp)))
                            },
                            border = RuleSoft,
                        )
                    }
                    IconButton(onClick = onSyncClick, enabled = !syncing, modifier = Modifier.size(32.dp)) {
                        if (syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = InkMute)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync tasks", tint = InkMute, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Punishment banners
        if (state.activePunishments.isNotEmpty()) {
            items(state.activePunishments) { p ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { PunishmentBanner(p) }
            }
        }

        // Overdue section
        if (overdue.isNotEmpty()) {
            item {
                Row(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("Overdue")
                    Text("· ${overdue.size}", style = CoachType.eyebrow, color = Alert)
                }
            }
            items(overdue, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                    TaskCard(row, onClick = { onTaskTap(row) })
                }
            }
        }

        // Today section
        if (today.isNotEmpty()) {
            item { Eyebrow("Today", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(today, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                    TaskCard(row, onClick = { onTaskTap(row) })
                }
            }
        }

        // Upcoming section
        if (upcoming.isNotEmpty()) {
            item { Eyebrow("Upcoming", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(upcoming, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                    TaskCard(row, onClick = { onTaskTap(row) })
                }
            }
        }

        // Closed
        if (done.isNotEmpty()) {
            item { Eyebrow("Closed", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(done, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                    TaskCard(row, onClick = {}) // done tasks are view-only
                }
            }
        }

        if (state.tasks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No open tasks. Add one in Google Tasks.", style = CoachType.bodyMd, color = InkMute)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(row: TaskRow, onClick: () -> Unit) {
    val isDone = row.state == "judged_done"
    val now = System.currentTimeMillis()
    val isDelayed = row.state == "delayed" && row.delayedUntilMs > now
    val stateInfo = if (isDelayed) {
        StateInfo("delay ends in ${remainingDelayLabel(row.delayedUntilMs - now)}", WarnGold)
    } else {
        stateInfo(row.state)
    }
    val isDueSoon = !isDone && !row.isOverdue && row.task.dueDateMs != null &&
        (row.task.dueDateMs - now) < 6 * 3_600_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, if (row.isOverdue && !isDone) Alert.copy(alpha = 0.3f) else RuleSoft, RoundedCornerShape(14.dp))
            .clickable(enabled = !isDone, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = isDone,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                uncheckedColor = if (row.isOverdue) Alert else Rule,
                checkedColor = SageGreen,
            ),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.task.title,
                style = CoachType.titleSm,
                color = if (isDone) InkMute else Ink,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.isOverdue && !isDone) {
                    Icon(Icons.Filled.Warning, null, tint = Alert, modifier = Modifier.size(11.dp))
                } else if (isDueSoon) {
                    Icon(Icons.Filled.AccessTime, null, tint = AccentTerracotta, modifier = Modifier.size(11.dp))
                }
                Text(
                    dueDateLabel(row),
                    style = CoachType.mono.copy(fontSize = 12.sp),
                    color = when {
                        row.isOverdue && !isDone -> Alert
                        isDueSoon                -> AccentTerracotta
                        else                     -> InkMute
                    },
                )
                if (stateInfo.label.isNotBlank()) {
                    Text("· ${stateInfo.label}", style = CoachType.meta, color = stateInfo.color)
                }
            }
            if (row.task.notes.isNotBlank()) {
                Text(row.task.notes, style = CoachType.meta, color = Ink2, modifier = Modifier.padding(top = 6.dp))
            }
            val hasBeenPrompted = row.state !in listOf("untouched", "judged_done")
            if (row.grantedDelayCount > 0 || hasBeenPrompted) {
                val chipColor = if (row.grantedDelayCount > 0) WarnGold else InkMute
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("◉", style = CoachType.meta, color = chipColor)
                    Text(
                        if (row.grantedDelayCount > 0)
                            "Coach granted ${row.grantedDelayCount} delay${if (row.grantedDelayCount > 1) "s" else ""}."
                        else
                            "0 delays granted.",
                        style = CoachType.meta,
                        color = chipColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskActionSheet(
    row: TaskRow,
    onMarkWorking: () -> Unit,
    onMarkCompleted: () -> Unit,
    onSubmitReason: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showReasonInput by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 8.dp)
            .navigationBarsPadding(),
    ) {
        // Task title
        Eyebrow(dueDateLabel(row), modifier = Modifier.padding(bottom = 4.dp))
        Text(row.task.title, style = CoachType.headlineSm, color = Ink)
        if (row.task.notes.isNotBlank()) {
            Text(row.task.notes, style = CoachType.bodyMd, color = Ink2, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(20.dp))

        if (!showReasonInput) {
            // Primary action — start working on the task
            Button(
                onClick = onMarkWorking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
            ) {
                Text("Yes, I'm doing it now", style = CoachType.titleSm, color = com.coach.screentime.ui.theme.PaperOnDeep)
            }

            Spacer(Modifier.height(10.dp))

            // Mark as completed — the task is already done
            OutlinedButton(
                onClick = onMarkCompleted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SageGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SageGreen),
            ) {
                Text("Mark as completed", style = CoachType.titleSm)
            }

            Spacer(Modifier.height(10.dp))

            // Escape hatch — ask the coach for a delay
            OutlinedButton(
                onClick = { showReasonInput = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
            ) {
                Text("Can't right now — ask coach", style = CoachType.titleSm)
            }
        } else {
            // Reason input
            Text(
                "Tell the coach why you can't do this right now.",
                style = CoachType.bodyMd,
                color = Ink2,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 500) reason = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. I'm in a meeting until 3pm…", style = CoachType.bodyMd, color = InkMute) },
                textStyle = CoachType.bodyMd,
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTerracotta,
                    unfocusedBorderColor = Rule,
                ),
            )
            Text(
                "${reason.length}/500",
                style = CoachType.meta,
                color = InkMute,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.End),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSubmitReason(reason.trim()) },
                enabled = reason.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
            ) {
                Text("Send to coach", style = CoachType.titleSm, color = Card)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showReasonInput = false },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMute),
            ) {
                Text("Back", style = CoachType.titleSm)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private data class StateInfo(val label: String, val color: androidx.compose.ui.graphics.Color)

private fun stateInfo(state: String) = when (state) {
    "working"           -> StateInfo("you're on it", SageGreen)
    "delayed"           -> StateInfo("delayed by coach", WarnGold)
    "prompted"          -> StateInfo("waiting on you", Alert)
    "dismissed_pending" -> StateInfo("punished · still due", Alert)
    "judged_done"       -> StateInfo("marked done", InkMute)
    else                -> StateInfo("", InkMute)
}

/** Formats a positive millisecond duration as "Xd Yh", "Xh Ym", or "Xm". */
private fun remainingDelayLabel(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = (ms / 60_000L).toInt()
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val mins = totalMinutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}

private fun dueDateLabel(row: TaskRow): String {
    val due = row.task.dueDateMs ?: return "no due date"
    val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = (LocalDate.now().toEpochDay() - date.toEpochDay()).toInt()
    return when {
        days > 1  -> "${days}d overdue"
        days == 1 -> "1d overdue"
        days == 0 -> "due today"
        else      -> "due in ${-days}d"
    }
}
