package com.coach.screentime.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.coach.screentime.ui.theme.Alert
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.SageGreen
import com.coach.screentime.ui.theme.WarnGold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        TasksUiState.Loading -> HelperText("Loading…")
        TasksUiState.NotConfigured -> HelperText("Google Tasks integration is not configured.\nSet GOOGLE_OAUTH_CLIENT_ID in local.properties and rebuild.")
        TasksUiState.Disconnected -> HelperText("Connect Google Tasks in Settings to enable task enforcement.")
        is TasksUiState.Ready -> ReadyContent(s)
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
            style = CoachType.headlineSm.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color = InkMute,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ReadyContent(state: TasksUiState.Ready) {
    val now = System.currentTimeMillis()
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
                state.email?.let { email ->
                    Pill(
                        text = email.take(20),
                        leading = {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(SageGreen, RoundedCornerShape(3.dp))
                            )
                        },
                        border = RuleSoft,
                    )
                }
            }
        }

        // Punishment banners
        if (state.activePunishments.isNotEmpty()) {
            items(state.activePunishments) { p ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PunishmentBanner(p)
                }
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
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { TaskCard(row) }
            }
        }

        // Today section
        if (today.isNotEmpty()) {
            item { Eyebrow("Today", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(today, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { TaskCard(row) }
            }
        }

        // Upcoming section
        if (upcoming.isNotEmpty()) {
            item { Eyebrow("Upcoming", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(upcoming, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { TaskCard(row) }
            }
        }

        // Closed today
        if (done.isNotEmpty()) {
            item { Eyebrow("Closed today", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp)) }
            items(done, key = { it.task.googleId }) { row ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { TaskCard(row) }
            }
        }

        if (state.tasks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No open tasks. Add one in Google Tasks.",
                        style = CoachType.bodyMd,
                        color = InkMute,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(row: TaskRow) {
    val isDone = row.state == "judged_done"
    val stateInfo = stateInfo(row.state)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, if (row.isOverdue && !isDone) Alert.copy(alpha = 0.3f) else RuleSoft, RoundedCornerShape(14.dp))
            .padding(14.dp, 14.dp, 14.dp, 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = isDone,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                uncheckedColor = if (row.isOverdue) Alert else Rule,
                checkedColor = SageGreen,
            ),
            modifier = Modifier.padding(top = 0.dp),
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
                }
                Text(
                    dueDateLabel(row),
                    style = CoachType.mono.copy(fontSize = 12.sp),
                    color = if (row.isOverdue && !isDone) Alert else InkMute,
                )
                if (stateInfo.label.isNotBlank()) {
                    Text("· ${stateInfo.label}", style = CoachType.meta, color = stateInfo.color)
                }
            }
            if (row.task.notes.isNotBlank()) {
                Text(
                    row.task.notes,
                    style = CoachType.meta,
                    color = Ink2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (row.grantedDelayCount > 0) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("◉", style = CoachType.meta, color = WarnGold)
                    Text(
                        "Coach granted ${row.grantedDelayCount} delay${if (row.grantedDelayCount > 1) "s" else ""}.",
                        style = CoachType.meta,
                        color = WarnGold,
                    )
                }
            }
        }
    }
}

private data class StateInfo(val label: String, val color: androidx.compose.ui.graphics.Color)

private fun stateInfo(state: String) = when (state) {
    "working"          -> StateInfo("you're on it", SageGreen)
    "delayed"          -> StateInfo("delayed by coach", WarnGold)
    "prompted"         -> StateInfo("waiting on you", Alert)
    "dismissed_pending"-> StateInfo("punished · still due", Alert)
    "judged_done"      -> StateInfo("marked done", InkMute)
    else               -> StateInfo("", InkMute)
}

private fun dueDateLabel(row: TaskRow): String {
    val due = row.task.dueDateMs ?: return ""
    val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = (LocalDate.now().toEpochDay() - date.toEpochDay()).toInt()
    return when {
        days > 1  -> "${days}d overdue"
        days == 1 -> "1d overdue"
        days == 0 -> "due today"
        else      -> "due in ${-days}d"
    }
}
