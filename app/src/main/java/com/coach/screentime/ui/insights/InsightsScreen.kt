package com.coach.screentime.ui.insights

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.data.db.entities.WeeklyReportEntity
import com.coach.screentime.ui.components.CoachAvatar
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.SageGreen

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        // Screen header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Eyebrow("Insights", modifier = Modifier.padding(bottom = 4.dp))
                    Text("Patterns", style = CoachType.headlineMd, color = Ink)
                }
                OutlinedButton(
                    onClick = { viewModel.runReportNow() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("Run report", style = CoachType.meta)
                }
            }
        }

        // 7-day chart card
        item {
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Card)
                    .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                    .padding(18.dp, 18.dp, 18.dp, 14.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Eyebrow("Last 7 days")
                        val weekTotal = state.sevenDayBars.sumOf { it.minutes }
                        Text(
                            formatMin(weekTotal),
                            style = CoachType.mono.copy(fontSize = 30.sp),
                            color = Ink,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SevenDayChart(state.sevenDayBars)
                }
            }
        }

        // Heatmap card
        if (state.heatmap.isNotEmpty()) {
            item {
                Box(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                        .padding(18.dp, 18.dp, 18.dp, 16.dp)
                ) {
                    Column {
                        Eyebrow("When you reach for it")
                        Text(
                            "Hour-of-day heatmap, last 7 days. Darker = more.",
                            style = CoachType.meta,
                            color = InkMute,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                        )
                        HourHeatmap(state.heatmap)
                    }
                }
            }
        }

        // Weekly letter(s)
        if (state.empty) {
            item {
                Box(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("No weekly report yet", style = CoachType.titleMd, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Reports run automatically every Sunday morning. Once you have a few days of data, you can generate one now.",
                            style = CoachType.bodyMd,
                            color = InkMute,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.runReportNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                        ) { Text("Generate now", style = CoachType.bodySm) }
                    }
                }
            }
        } else {
            // Latest letter (full)
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    WeeklyLetterCard(state.reports.first(), isLatest = true)
                }
            }
            // Older letters (compact rows)
            items(state.reports.drop(1)) { r ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    OlderLetterRow(r)
                }
            }
        }
    }
}

@Composable
private fun WeeklyLetterCard(report: WeeklyReportEntity, isLatest: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card2)
            .border(1.dp, RuleSoft, RoundedCornerShape(18.dp))
            .padding(24.dp, 24.dp, 22.dp, 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CoachAvatar(size = 28)
            Column {
                Eyebrow("Week · letter from your coach")
                Text(
                    "${report.weekStart} · generated ${formatTimestamp(report.generatedAt)}",
                    style = CoachType.mono.copy(fontSize = 10.5.sp),
                    color = InkMute,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Pull the first paragraph as the opening italic hook
        val paragraphs = report.markdownBody.trim().split("\n\n")
        val opening = paragraphs.firstOrNull()?.trimStart('#', ' ') ?: ""
        val body = paragraphs.drop(1).joinToString("\n\n")

        if (opening.isNotBlank()) {
            Text(
                opening,
                style = CoachType.headlineSm.copy(fontStyle = FontStyle.Italic),
                color = Ink,
            )
            Spacer(Modifier.height(14.dp))
        }

        if (body.isNotBlank()) {
            Text(
                body,
                style = CoachType.bodyMd,
                color = Ink2,
            )
        }

        Spacer(Modifier.height(18.dp))
        Divider(color = Rule)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Drawn from your usage data this week.", style = CoachType.meta, color = InkMute)
        }
    }
}

@Composable
private fun OlderLetterRow(report: WeeklyReportEntity) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
            .padding(14.dp, 14.dp, 14.dp, 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Week of ${report.weekStart} · letter", style = CoachType.titleSm, color = Ink)
            val firstLine = report.markdownBody.lines().firstOrNull { it.isNotBlank() }?.trimStart('#', ' ') ?: ""
            if (firstLine.isNotBlank()) {
                Text(
                    "\"${firstLine.take(80)}\"",
                    style = CoachType.meta,
                    color = InkMute,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text("›", style = CoachType.titleMd, color = InkMute, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun HourHeatmap(grid: Array<IntArray>) {
    val max = (grid.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(28.dp))
            (0..23).forEach { h ->
                val show = h % 6 == 0
                Text(
                    if (show) h.toString() else "",
                    style = CoachType.mono.copy(fontSize = 9.sp),
                    color = InkMute,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val dayLabels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
        grid.indices.forEach { day ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dayLabels.getOrElse(day) { "-${grid.size - 1 - day}d" },
                    style = CoachType.mono.copy(fontSize = 10.sp),
                    color = InkMute,
                    modifier = Modifier.width(28.dp),
                )
                (0..23).forEach { hour ->
                    val v = grid[day][hour]
                    val alpha = (v.toFloat() / max).coerceIn(0f, 1f)
                    val cellColor = if (v == 0)
                        RuleSoft
                    else
                        AccentTerracotta.copy(alpha = 0.18f + alpha * 0.82f)
                    Box(
                        Modifier.weight(1f).height(14.dp).padding(end = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun SevenDayChart(bars: List<DayBar>) {
    if (bars.isEmpty()) return
    val max = (bars.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)
    val goalMin = 150 // 2h 30m default goal line

    Box(Modifier.fillMaxWidth().height(140.dp)) {
        Row(
            Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { b ->
                val pct = b.minutes.toFloat() / max
                val over = b.minutes > goalMin
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight((1f - pct).coerceAtLeast(0.001f)))
                    Box(
                        Modifier.weight(pct.coerceAtLeast(0.02f))
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (over) AccentTerracotta else SageGreen)
                    )
                }
            }
        }
        // Day labels
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bars.forEach { b ->
                Text(
                    b.date.takeLast(2),
                    style = CoachType.mono.copy(fontSize = 10.sp),
                    color = InkMute,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

private fun formatMin(min: Int): String {
    if (min < 60) return "${min}m"
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun formatTimestamp(ts: Long): String {
    val dt = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault())
    return "${dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${dt.hour}:${dt.minute.toString().padStart(2,'0')}"
}
